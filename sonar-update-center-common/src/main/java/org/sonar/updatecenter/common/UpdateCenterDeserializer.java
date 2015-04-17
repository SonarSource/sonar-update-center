/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.updatecenter.common;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.updatecenter.common.FormatUtils.toDate;

public final class UpdateCenterDeserializer {

  public static final String DATE_SUFFIX = ".date";
  public static final String DESCRIPTION_SUFFIX = ".description";
  public static final String MAVEN_GROUPID_SUFFIX = ".mavenGroupId";
  public static final String MAVEN_ARTIFACTID_SUFFIX = ".mavenArtifactId";
  public static final String CHANGELOG_URL_SUFFIX = ".changelogUrl";
  public static final String DOWNLOAD_URL_SUFFIX = ".downloadUrl";
  public static final String SONAR_PREFIX = "sonar.";
  public static final String DEFAULTS_PREFIX = "defaults";
  public static final String PLUGINS = "plugins";
  private static final String PUBLIC_VERSIONS = "publicVersions";
  private static final String PRIVATE_VERSIONS = "privateVersions";
  private static final String ARCHIVED_VERSIONS = "archivedVersions";
  private static final String DEV_VERSION = "devVersion";
  private Mode mode;
  private boolean ignoreError;

  public UpdateCenterDeserializer(Mode mode, boolean ignoreError) {
    this.mode = mode;
    this.ignoreError = ignoreError;
  }

  public static enum Mode {
    /**
     * Will ignore devVersion and privateVersions
     */
    PROD,
    DEV
  }

  /**
   * Load configuration with one file for each plugin
   */
  public UpdateCenter fromManyFiles(File mainFile) throws IOException {
    FileInputStream in = FileUtils.openInputStream(mainFile);
    try {
      Properties props = new Properties();
      props.load(in);
      loadPluginProperties(mainFile, props);
      UpdateCenter pluginReferential = fromProperties(props);
      pluginReferential.setDate(new Date(mainFile.lastModified()));
      return pluginReferential;

    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Load configuration with one single file containing everything
   */
  public UpdateCenter fromSingleFile(File mainFile) throws IOException {
    FileInputStream in = FileUtils.openInputStream(mainFile);
    try {
      Properties props = new Properties();
      props.load(in);
      UpdateCenter pluginReferential = fromProperties(props);
      pluginReferential.setDate(new Date(mainFile.lastModified()));
      return pluginReferential;

    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  private void loadPluginProperties(File file, Properties props) throws IOException {
    String[] pluginKeys = getArray(props, PLUGINS);
    for (String pluginKey : pluginKeys) {
      File pluginFile = new File(file.getParent(), pluginKey + ".properties");
      FileInputStream pluginFis = FileUtils.openInputStream(pluginFile);
      try {
        Properties pluginProps = new Properties();
        pluginProps.load(pluginFis);
        for (Map.Entry<Object, Object> prop : pluginProps.entrySet()) {
          props.put(pluginKey + "." + prop.getKey(), prop.getValue());
        }
      } finally {
        IOUtils.closeQuietly(pluginFis);
      }
    }
  }

  public UpdateCenter fromProperties(Properties p) {
    Sonar sonar = new Sonar();
    Date date = FormatUtils.toDate(p.getProperty("date"), true);
    List<Plugin> plugins = newArrayList();

    parseSonar(p, sonar);

    parsePlugins(p, sonar, plugins);

    validatePublicPluginSQVersionOverlap(plugins);

    PluginReferential pluginReferential = PluginReferential.create(plugins);
    for (Plugin plugin : pluginReferential.getPlugins()) {
      for (Release release : plugin.getAllReleases()) {
        String[] requiredReleases = StringUtils.split(StringUtils.defaultIfEmpty(get(p, plugin.getKey(), release.getVersion().getName() + ".requirePlugins", false), ""), ",");
        for (String requiresPluginKey : requiredReleases) {
          Iterator<String> split = Splitter.on(':').split(requiresPluginKey).iterator();
          String requiredPluginReleaseKey = split.next();
          String requiredMinimumReleaseVersion = split.next();
          pluginReferential.addOutgoingDependency(release, requiredPluginReleaseKey, requiredMinimumReleaseVersion);
        }
      }
    }
    return UpdateCenter.create(pluginReferential, sonar).setDate(date);
  }

  private void reportError(String message) {
    if (ignoreError) {
      System.err.println(message);
    } else {
      throw new IllegalStateException(message);
    }
  }

  private void validatePublicPluginSQVersionOverlap(List<Plugin> plugins) {
    for (Plugin plugin : plugins) {
      validatePublicPluginSQVersionOverlap(plugin);
    }
  }

  private void validatePublicPluginSQVersionOverlap(Plugin plugin) {
    Map<Version, Release> sonarVersion = new HashMap<Version, Release>();
    for (Release r : plugin.getPublicReleases()) {
      for (Version v : r.getRequiredSonarVersions()) {
        if (sonarVersion.containsKey(v)) {
          reportError("SQ version " + v + " is declared compatible with two public versions of " + pluginName(plugin) + " plugin: " + r.getVersion()
            + " and "
            + sonarVersion.get(v).getVersion());
        }
        sonarVersion.put(v, r);
      }
    }
  }

  private String pluginName(Plugin plugin) {
    return StringUtils.isNotBlank(plugin.getName()) ? plugin.getName() : plugin.getKey();
  }

  private void parsePlugins(Properties p, Sonar sonar, List<Plugin> plugins) {
    String[] pluginKeys = getArray(p, "plugins");
    for (String pluginKey : pluginKeys) {
      Plugin plugin = new Plugin(pluginKey);
      plugin.setName(get(p, pluginKey, "name", false));
      plugin.setDescription(get(p, pluginKey, "description", false));
      plugin.setCategory(get(p, pluginKey, "category", true));
      plugin.setHomepageUrl(get(p, pluginKey, "homepageUrl", false));
      plugin.setLicense(get(p, pluginKey, "license", false));
      plugin.setOrganization(get(p, pluginKey, "organization", false));
      plugin.setOrganizationUrl(get(p, pluginKey, "organizationUrl", false));
      plugin.setTermsConditionsUrl(get(p, pluginKey, "termsConditionsUrl", false));
      plugin.setIssueTrackerUrl(get(p, pluginKey, "issueTrackerUrl", false));
      plugin.setSourcesUrl(get(p, pluginKey, "scm", false));
      plugin.setDevelopers(newArrayList(getArray(p, pluginKey, "developers")));

      parsePluginReleases(p, sonar, pluginKey, plugin, PUBLIC_VERSIONS, true, false);
      if (mode == Mode.DEV) {
        parsePluginReleases(p, sonar, pluginKey, plugin, PRIVATE_VERSIONS, false, false);
        parsePluginDevVersions(p, sonar, pluginKey, plugin);
      }
      parsePluginReleases(p, sonar, pluginKey, plugin, ARCHIVED_VERSIONS, false, true);

      if (!plugin.getAllReleases().isEmpty()) {
        plugins.add(plugin);
      }
    }
  }

  private void parsePluginReleases(Properties p, Sonar sonar, String pluginKey, Plugin plugin, String key, boolean isPublicRelease, boolean isArchivedRelease) {
    String[] pluginPublicReleases = getArray(p, pluginKey, key);
    for (String pluginVersion : pluginPublicReleases) {
      Release release = parsePlugin(p, sonar, pluginKey, plugin, isPublicRelease, isArchivedRelease, pluginVersion);
      if (!plugin.getAllReleases().contains(release)) {
        plugin.addRelease(release);
      } else {
        reportError("Duplicate version for plugin " + pluginKey + ": " + pluginVersion);
      }
    }
  }

  private Release parsePlugin(Properties p, Sonar sonar, String pluginKey, Plugin plugin, boolean isPublicRelease, boolean isArchivedRelease, String pluginVersion) {
    Release release = new Release(plugin, pluginVersion);
    release.setPublic(isPublicRelease);
    release.setArchived(isArchivedRelease);
    release.setDownloadUrl(getOrDefault(p, pluginKey, pluginVersion, DOWNLOAD_URL_SUFFIX, isPublicRelease));
    release.setChangelogUrl(getOrDefault(p, pluginKey, pluginVersion, CHANGELOG_URL_SUFFIX, false));
    release.setDate(toDate(getOrDefault(p, pluginKey, pluginVersion, DATE_SUFFIX, isPublicRelease), false));
    release.setDescription(getOrDefault(p, pluginKey, pluginVersion, DESCRIPTION_SUFFIX, isPublicRelease));
    release.setGroupId(getOrDefault(p, pluginKey, pluginVersion, MAVEN_GROUPID_SUFFIX, true));
    release.setArtifactId(getOrDefault(p, pluginKey, pluginVersion, MAVEN_ARTIFACTID_SUFFIX, true));
    String[] requiredSonarVersions = getRequiredSonarVersions(p, pluginKey, pluginVersion, sonar, isArchivedRelease);
    if (!isArchivedRelease && requiredSonarVersions.length == 0) {
      reportError("Plugin " + pluginName(plugin) + " version " + pluginVersion + " should declare compatible SQ versions");
    }
    for (String requiredSonarVersion : requiredSonarVersions) {
      release.addRequiredSonarVersions(Version.create(requiredSonarVersion));
    }
    return release;
  }

  private void parsePluginDevVersions(Properties p, Sonar sonar, String pluginKey, Plugin plugin) {
    String devVersion = get(p, pluginKey, DEV_VERSION, false);
    if (StringUtils.isNotBlank(devVersion)) {
      Release release = parsePlugin(p, sonar, pluginKey, plugin, false, false, devVersion);
      plugin.setDevRelease(release);
    }
  }

  private void parseSonar(Properties p, Sonar sonar) {
    parseSonarVersions(p, sonar);
    if (mode == Mode.DEV) {
      parseSonarDevVersions(p, sonar);
    }
    parseSonarLtsVersion(p, sonar);
  }

  private void parseSonarDevVersions(Properties p, Sonar sonar) {
    String devVersion = get(p, DEV_VERSION, true);
    Release release = parseSonarVersion(p, sonar, false, devVersion);
    sonar.setDevRelease(release);
  }

  private void parseSonarLtsVersion(Properties p, Sonar sonar) {
    String ltsVersion = get(p, "ltsVersion", true);
    sonar.setLtsRelease(ltsVersion);
    if (!sonar.getReleases().contains(sonar.getLtsRelease())) {
      reportError("ltsVersion seems wrong as it is not listed in SonarQube versions");
    }
  }

  private void parseSonarVersions(Properties p, Sonar sonar) {
    parseSonarVersions(p, sonar, PUBLIC_VERSIONS, true);
    if (mode == Mode.DEV) {
      parseSonarVersions(p, sonar, PRIVATE_VERSIONS, false);
    }
  }

  private void parseSonarVersions(Properties p, Sonar sonar, String key, boolean isPublicRelease) {
    for (String sonarVersion : getArray(p, key)) {
      Release release = parseSonarVersion(p, sonar, isPublicRelease, sonarVersion);
      if (!sonar.getAllReleases().contains(release)) {
        sonar.addRelease(release);
      } else {
        reportError("Duplicate version for SonarQube: " + sonarVersion);
      }
    }
  }

  private Release parseSonarVersion(Properties p, Sonar sonar, boolean isPublicRelease, String sonarVersion) {
    Release release = new Release(sonar, sonarVersion);
    release.setPublic(isPublicRelease);
    release.setChangelogUrl(getOrDefault(p, sonarVersion, CHANGELOG_URL_SUFFIX, isPublicRelease));
    release.setDescription(getOrDefault(p, sonarVersion, DESCRIPTION_SUFFIX, isPublicRelease));
    release.setDownloadUrl(getOrDefault(p, sonarVersion, DOWNLOAD_URL_SUFFIX, isPublicRelease));
    release.setDate(FormatUtils.toDate(getOrDefault(p, sonarVersion, DATE_SUFFIX, isPublicRelease), false));
    return release;
  }

  private String[] getRequiredSonarVersions(Properties p, String pluginKey, String pluginVersion, Sonar sonar, boolean isArchived) {
    String sqVersions = get(p, pluginKey, pluginVersion + ".sqVersions", !isArchived);
    List<String> patterns = split(StringUtils.defaultIfEmpty(sqVersions, ""));
    List<String> result = new LinkedList<String>();
    for (String pattern : patterns) {
      if (pattern != null) {
        Matcher matcher = Pattern.compile("\\[(.*),(.*)\\]").matcher(pattern);
        if (matcher.matches()) {
          final Version low = Version.create(resolve(matcher.group(1), sonar));
          final Version high = Version.create(resolve(matcher.group(2), sonar));
          resolveRangeOfRequiredSQVersion(sonar, result, low, high);
        } else {
          result.add(resolve(pattern, sonar));
        }
      }
    }
    return result.toArray(new String[result.size()]);
  }

  private void resolveRangeOfRequiredSQVersion(Sonar sonar, List<String> result, final Version low, final Version high) {
    Collection<Version> versions = Collections2.filter(transform(sonar.getAllReleases(), new Function<Release, Version>() {
      @Override
      public Version apply(Release release) {
        return release != null ? release.getVersion() : null;
      }
    }), new Predicate<Version>() {
      @Override
      public boolean apply(Version version) {
        return version != null && version.compareTo(low) >= 0 && version.compareTo(high) <= 0;
      }
    });
    for (Version version : versions) {
      result.add(version.toString());
    }
  }

  private List<String> split(String requiredSonarVersions) {
    List<String> splitted = new ArrayList<String>();
    int skipCommas = 0;
    String s = "";
    for (char c : requiredSonarVersions.toCharArray()) {
      if (c == ',' && skipCommas == 0) {
        splitted.add(s);
        s = "";
      } else {
        if (c == '[') {
          skipCommas++;
        }
        if (c == ']') {
          skipCommas--;
        }
        s += c;
      }
    }
    if (StringUtils.isNotBlank(s)) {
      splitted.add(s);
    }
    return splitted;
  }

  private String resolve(String version, Sonar sonar) {
    if ("LATEST".equals(version)) {
      return sonar.getAllReleases().last().getVersion().toString();
    }
    if (version.endsWith("*")) {
      return resolveWithWildcard(version, sonar);
    }
    return version;
  }

  private String resolveWithWildcard(String version, Sonar sonar) {
    String prefix = version.substring(0, version.length() - 1);
    String prefixWithoutDot = prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
    Release found = null;
    for (Release r : sonar.getAllReleases()) {
      if (r.getVersion().toString().equals(prefixWithoutDot) || r.getVersion().toString().startsWith(prefix)) {
        found = r;
      }
    }
    if (found != null) {
      return found.getVersion().toString();
    } else {
      throw new IllegalStateException("Unable to resolve " + version);
    }
  }

  private String getOrDefault(Properties props, String sqVersion, String suffix, boolean required) {
    String key = sqVersion + suffix;
    String defaultKey = DEFAULTS_PREFIX + suffix;
    String value = getOrDefault(props, key, defaultKey);
    if (StringUtils.isBlank(value) && required) {
      reportUndefined(key);
    }
    return value;
  }

  private void reportUndefined(String key) {
    reportError(key + " should be defined");
  }

  private String get(Properties props, String key, boolean required) {
    String value = get(props, key);
    if (StringUtils.isBlank(value) && required) {
      reportUndefined(key);
    }
    return value;
  }

  private String get(Properties props, String key) {
    return StringUtils.defaultIfEmpty(props.getProperty(key), null);
  }

  private String getOrDefault(Properties props, String key, String defaultKey) {
    if (props.containsKey(key)) {
      return props.getProperty(key);
    }
    return StringUtils.defaultIfEmpty(props.getProperty(defaultKey), null);
  }

  private String getOrDefault(Properties props, String pluginKey, String version, String suffix, boolean required) {
    String key = pluginKey + "." + version + suffix;
    String defaultKey = pluginKey + "." + DEFAULTS_PREFIX + suffix;
    String value = getOrDefault(props, key, defaultKey);
    if (StringUtils.isBlank(value) && required) {
      reportUndefined(key);
    }
    return value;
  }

  private String get(Properties p, String pluginKey, String field, boolean required) {
    String key = pluginKey + "." + field;
    String value = get(p, key);
    if (StringUtils.isBlank(value) && required) {
      reportUndefined(key);
    }
    return value;
  }

  private String[] getArray(Properties props, String key) {
    return StringUtils.split(StringUtils.defaultIfEmpty(props.getProperty(key), ""), ",");
  }

  private String[] getArray(Properties p, String pluginKey, String field) {
    return getArray(p, pluginKey + "." + field);
  }

}
