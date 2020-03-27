/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.updatecenter.common;

import org.apache.commons.lang.StringUtils;
import org.sonar.updatecenter.common.exception.SonarVersionRangeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static org.sonar.updatecenter.common.FormatUtils.toDate;

public final class UpdateCenterDeserializer {

  public static final String DATE_SUFFIX = ".date";
  public static final String DESCRIPTION_SUFFIX = ".description";
  public static final String MAVEN_GROUPID_SUFFIX = ".mavenGroupId";
  public static final String MAVEN_ARTIFACTID_SUFFIX = ".mavenArtifactId";
  public static final String CHANGELOG_URL_SUFFIX = ".changelogUrl";
  public static final String DOWNLOAD_URL_SUFFIX = ".downloadUrl";
  public static final String DOWNLOAD_DEVELOPER_URL_SUFFIX = ".downloadDeveloperUrl";
  public static final String DOWNLOAD_ENTERPRISE_URL_SUFFIX = ".downloadEnterpriseUrl";
  public static final String DOWNLOAD_DATACENTER_URL_SUFFIX = ".downloadDatacenterUrl";
  public static final String DISPLAY_VERSION_SUFFIX = ".displayVersion";
  public static final String SONAR_PREFIX = "sonar.";
  public static final String DEFAULTS_PREFIX = "defaults";
  public static final String PLUGINS = "plugins";
  public static final String SCANNERS = "scanners";
  private static final String PUBLIC_VERSIONS = "publicVersions";
  private static final String PRIVATE_VERSIONS = "privateVersions";
  private static final String ARCHIVED_VERSIONS = "archivedVersions";
  private static final String DEV_VERSION = "devVersion";
  private static final String LATEST_KEYWORD = "LATEST";
  private static final String FLAVORS_PREFIX = "flavors";
  private Mode mode;
  private boolean ignoreError;
  private boolean includeArchives;

  public UpdateCenterDeserializer(Mode mode, boolean ignoreError) {
    this(mode, ignoreError, false);
  }

  public UpdateCenterDeserializer(Mode mode, boolean ignoreError, boolean includeArchives) {
    this.mode = mode;
    this.ignoreError = ignoreError;
    this.includeArchives = includeArchives;
  }

  public static String getDownloadUrlSuffix(Release.Edition edition) {
    switch (edition) {
      case DEVELOPER:
        return DOWNLOAD_DEVELOPER_URL_SUFFIX;
      case ENTERPRISE:
        return DOWNLOAD_ENTERPRISE_URL_SUFFIX;
      case DATACENTER:
        return DOWNLOAD_DATACENTER_URL_SUFFIX;
      default:
        return DOWNLOAD_URL_SUFFIX;
    }
  }

  public enum Mode {
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
    try (InputStream in = Files.newInputStream(mainFile.toPath())) {
      Properties props = new Properties();
      props.load(in);
      loadProperties(mainFile, props, PLUGINS);
      loadProperties(mainFile, props, SCANNERS);
      UpdateCenter pluginReferential = fromProperties(props);
      pluginReferential.setDate(new Date(mainFile.lastModified()));
      return pluginReferential;
    }
  }



  private static void loadProperties(File file, Properties props, String listKey) throws IOException {
    String[] keys = getArray(props, listKey);
    for (String key : keys) {
      File propFile = new File(file.getParent(), key + ".properties");
      try (InputStream fileInputStream = Files.newInputStream(propFile.toPath())) {
        Properties p = new Properties();
        p.load(fileInputStream);
        for (Map.Entry<Object, Object> prop : p.entrySet()) {
          props.put(key + "." + prop.getKey(), prop.getValue());
        }

      }
    }
  }

  public UpdateCenter fromProperties(Properties p) {
    Sonar sonar = new Sonar();
    Date date = FormatUtils.toDate(p.getProperty("date"), true);
    List<Plugin> plugins = new ArrayList<>();
    List<Scanner> scanners = new ArrayList<>();

    parseSonar(p, sonar);

    parsePlugins(p, sonar, plugins);
    parseScanners(p, sonar, scanners);

    validatePublicPluginSQVersionOverlap(plugins);

    validateLATESTonLatestPluginVersion(plugins);

    PluginReferential pluginReferential = PluginReferential.create(plugins);
    for (Plugin plugin : pluginReferential.getPlugins()) {
      for (Release release : plugin.getAllReleases()) {
        String[] requiredReleases = StringUtils.split(StringUtils.defaultIfEmpty(get(p, plugin.getKey(), release.getVersion().getName() + ".requirePlugins", false), ""), ",");
        for (String requiresPluginKey : requiredReleases) {
          String[] split = requiresPluginKey.split(":");
          String requiredPluginReleaseKey = split[0];
          String requiredMinimumReleaseVersion = split[1];
          pluginReferential.addOutgoingDependency(release, requiredPluginReleaseKey, requiredMinimumReleaseVersion);
        }
      }
    }
    return UpdateCenter.create(pluginReferential, scanners, sonar).setDate(date);
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
    Map<Version, Release> sonarVersion = new HashMap<>();
    for (Release r : plugin.getPublicReleases()) {
      for (Version v : r.getRequiredSonarVersions()) {
        if (sonarVersion.containsKey(v)) {
          reportError("SQ version " + v + " is declared compatible with two public versions of " + pluginName(plugin)
            + " plugin: " + r.getVersion()
            + " and " + sonarVersion.get(v).getVersion());
        }
        sonarVersion.put(v, r);
      }
    }
  }

  private void validateLATESTonLatestPluginVersion(List<Plugin> plugins) {
    for (Plugin plugin : plugins) {

      SortedSet<Release> publicAndArchivedReleases = new TreeSet<>(plugin.getPublicReleases());
      publicAndArchivedReleases.addAll(plugin.getArchivedReleases());

      for (Release r : publicAndArchivedReleases) {
        Version[] versionsWLatest = r.getSonarVersionFromString(LATEST_KEYWORD);
        // only latest release may depend on LATEST SQ
        if (!r.equals(publicAndArchivedReleases.last()) && versionsWLatest.length > 0) {
          reportError("Only the latest release of plugin " + pluginName(plugin)
            + " may depend on " + LATEST_KEYWORD + " SonarQube");
        }
      }
    }
  }

  private static String pluginName(Component component) {
    return StringUtils.isNotBlank(component.getName()) ? component.getName() : component.getKey();
  }

  private void parseScanners(Properties p, Sonar sonar, List<Scanner> scanners) {
    String[] scannerKeys = getArray(p, SCANNERS);
    for (String pluginKey : scannerKeys) {
      Scanner scanner = Scanner.factory(pluginKey);

      parseComponent(p, sonar, pluginKey, scanner);

      // do not add plugin without any version
      if (!scanner.getAllReleases().isEmpty()) {
        scanners.add(scanner);
      }
    }
  }

  private void parseComponent(Properties p, Sonar sonar, String key, Component c) {
    c.setName(get(p, key, "name", false));
    c.setDescription(get(p, key, "description", false));
    c.setCategory(get(p, key, "category", true));
    c.setHomepageUrl(get(p, key, "homepageUrl", false));
    c.setLicense(get(p, key, "license", false));
    c.setOrganization(get(p, key, "organization", false));
    c.setOrganizationUrl(get(p, key, "organizationUrl", false));
    c.setTermsConditionsUrl(get(p, key, "termsConditionsUrl", false));
    c.setIssueTrackerUrl(get(p, key, "issueTrackerUrl", false));
    c.setSourcesUrl(get(p, key, "scm", false));
    c.setDevelopers(asList(getArray(p, key, "developers")));

    HashMap<String, Map.Entry<String, Integer>> flavorsLabel = new HashMap<>();
    parseFlavors(p, key, flavorsLabel);


    parseReleases(p, sonar, key, c, PUBLIC_VERSIONS, flavorsLabel, true, false);
    if (mode == Mode.DEV) {
      parseReleases(p, sonar, key, c, PRIVATE_VERSIONS, flavorsLabel, false, false);
      parseDevVersions(p, sonar, key, c, flavorsLabel);
    }

    if (includeArchives) {
      parseReleases(p, sonar, key, c, PRIVATE_VERSIONS, flavorsLabel, false, false);
      parseReleases(p, sonar, key, c, ARCHIVED_VERSIONS, flavorsLabel, false, false);
    } else {
      parseReleases(p, sonar, key, c, ARCHIVED_VERSIONS, flavorsLabel, false, true);
    }
  }

  private void parsePlugins(Properties p, Sonar sonar, List<Plugin> plugins) {
    String[] pluginKeys = getArray(p, PLUGINS);
    for (String pluginKey : pluginKeys) {
      Plugin plugin = Plugin.factory(pluginKey);

      parseComponent(p, sonar, pluginKey, plugin);
      plugin.setSupportedBySonarSource(Boolean.valueOf(get(p, pluginKey, "supportedBySonarSource", false)));

      // do not add plugin without any version
      if (!plugin.getAllReleases().isEmpty()) {
        plugins.add(plugin);
      }
    }
  }

  private void parseReleases(Properties p, Sonar sonar, String pluginKey, Component component, String key,
    HashMap<String, Map.Entry<String, Integer>> flavosLabel, boolean isPublicRelease, boolean isArchivedRelease) {
    String[] pluginPublicReleases = getArray(p, pluginKey, key);
    for (String pluginVersion : pluginPublicReleases) {
      Release release = parseRelease(p, sonar, pluginKey, component, isPublicRelease, isArchivedRelease, pluginVersion, flavosLabel);
      if (!component.getAllReleases().contains(release)) {
        component.addRelease(release);
      } else {
        reportError("Duplicate version for plugin " + pluginKey + ": " + pluginVersion);
      }
    }
  }

  private void parseFlavors(Properties p, String pluginKey, HashMap<String, Map.Entry<String, Integer>> flavosLabel) {
    String[] flavors = getArray(p, pluginKey, FLAVORS_PREFIX);
    for (int i = 0; i < flavors.length; i++) {
      flavosLabel.put(flavors[i], new AbstractMap.SimpleEntry<String, Integer>(get(p, pluginKey, FLAVORS_PREFIX + "." + flavors[i]  + ".label", true), i));
    }
  }

  private Release parseRelease(Properties p, Sonar sonar, String pluginKey, Component component,
    boolean isPublicRelease, boolean isArchivedRelease, String pluginVersion, HashMap<String, Map.Entry<String, Integer>> flavosLabel) {

    Release release = new Release(component, pluginVersion);
    try {
      release.setPublic(isPublicRelease);
      release.setArchived(isArchivedRelease);
      parseDownloadUrl(p, pluginKey, pluginVersion, isPublicRelease, flavosLabel, release);
      release.setChangelogUrl(getOrDefault(p, pluginKey, pluginVersion, CHANGELOG_URL_SUFFIX, false));
      release.setDisplayVersion(getOrDefault(p, pluginKey, pluginVersion, DISPLAY_VERSION_SUFFIX, false));
      release.setDate(toDate(getOrDefault(p, pluginKey, pluginVersion, DATE_SUFFIX, isPublicRelease), false));
      release.setDescription(getOrDefault(p, pluginKey, pluginVersion, DESCRIPTION_SUFFIX, isPublicRelease));
      if(component.needArtifact()) {
        release.setGroupId(getOrDefault(p, pluginKey, pluginVersion, MAVEN_GROUPID_SUFFIX, true));
        release.setArtifactId(getOrDefault(p, pluginKey, pluginVersion, MAVEN_ARTIFACTID_SUFFIX, true));
      }
      if(component.needSqVersion()) {
        Version[] requiredSonarVersions = getRequiredSonarVersions(p, pluginKey, pluginVersion, sonar, isArchivedRelease);
        if (!isArchivedRelease && requiredSonarVersions.length == 0) {
          reportError("Plugin " + pluginName(component) + " version " + pluginVersion
            + " should declare compatible SQ versions");
        }
        for (Version requiredSonarVersion : requiredSonarVersions) {
          release.addRequiredSonarVersions(requiredSonarVersion);
        }
      }
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("issue while processing plugin " + pluginKey, ex);
    }
    return release;
  }

  private void parseDownloadUrl(Properties p, String pluginKey, String pluginVersion, boolean isPublicRelease,
    HashMap<String, Map.Entry<String, Integer>> flavorLabel, Release release) {
    for(Map.Entry<String, Map.Entry<String, Integer>>  flavor : flavorLabel.entrySet()) {
      String url = get(p, pluginKey, pluginVersion + DOWNLOAD_URL_SUFFIX + "." + flavor.getKey(), false);
      if (url != null) {
        release.addScannerDownloadUrlAndLabel(flavor.getKey(), flavor.getValue().getKey(), url, flavor.getValue().getValue());
      }
    }

    String url = getOrDefault(p, pluginKey, pluginVersion, DOWNLOAD_URL_SUFFIX, false);
    if(url != null) {
      release.setDownloadUrl(url);
    }
    if (!release.hasDownloadUrl() && isPublicRelease) {
      reportError("Download url is missing");
    }
  }

  private void parseDevVersions(Properties p, Sonar sonar, String pluginKey, Component component, HashMap<String, Map.Entry<String, Integer>> flavosLabel) {
    String devVersion = get(p, pluginKey, DEV_VERSION, false);
    if (StringUtils.isNotBlank(devVersion)) {
      Release release = parseRelease(p, sonar, pluginKey, component, false, false, devVersion, flavosLabel);
      component.setDevRelease(release);
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
    if (mode == Mode.DEV || includeArchives) {
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
    release.setDisplayVersion(getOrDefault(p, sonarVersion, DISPLAY_VERSION_SUFFIX, false));
    release.setDescription(getOrDefault(p, sonarVersion, DESCRIPTION_SUFFIX, isPublicRelease));
    for (Release.Edition edition: Release.Edition.values()) {
      String downloadUrl = getOrDefault(p, sonarVersion, getDownloadUrlSuffix(edition), edition == Release.Edition.COMMUNITY && isPublicRelease);
      if (downloadUrl != null) {
        release.setDownloadUrl(downloadUrl, edition);
      }
    }
    release.setDate(FormatUtils.toDate(getOrDefault(p, sonarVersion, DATE_SUFFIX, isPublicRelease), false));
    return release;
  }

  private Version[] getRequiredSonarVersions(Properties p, String pluginKey, String pluginVersion,
    Sonar sonar, boolean isArchived) {
    String sqVersions = get(p, pluginKey, pluginVersion + ".sqVersions", !isArchived);
    List<String> patterns = split(StringUtils.defaultIfEmpty(sqVersions, ""));
    List<Version> result = new LinkedList<>();
    for (String pattern : patterns) {
      if (pattern != null) {
        Matcher multipleEltMatcher = Pattern.compile("\\[(.*),(.*)\\]").matcher(pattern);
        Matcher simpleEltMatcher = Pattern.compile("\\[(.*)\\]").matcher(pattern);
        if (multipleEltMatcher.matches()) {
          final Version low = resolveLowVersion(multipleEltMatcher.group(1), pattern, pluginKey);
          final Version high = resolveKeywordAndStar(multipleEltMatcher.group(2), sonar, pluginKey);
          resolveRangeOfRequiredSQVersion(sonar, result, low, high);
        } else if (simpleEltMatcher.matches()) {
          result.add(resolveKeywordAndStar(simpleEltMatcher.group(1), sonar, pluginKey));
        } else {
          result.add(resolveKeywordAndStar(pattern, sonar, pluginKey));
        }
      }
    }
    return result.toArray(new Version[result.size()]);
  }

  private static void resolveRangeOfRequiredSQVersion(Sonar sonar, List<Version> result, final Version low, final Version high) {
    sonar.getAllReleases().stream()
      .filter(Objects::nonNull)
      .map(Release::getVersion)
      .filter(Objects::nonNull)
      .filter(version -> version.compareTo(low) >= 0 && version.compareTo(high) <= 0)
      .forEach(version -> {
        String fromString;
        if (version.equals(low)) {
          fromString = low.getFromString();
        } else if (version.equals(high)) {
          fromString = high.getFromString();
        } else {
          fromString = "";
        }
        result.add(Version.create(version, fromString));
      });
  }

  private static List<String> split(String requiredSonarVersions) {
    List<String> splitted = new ArrayList<>();
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
        s += Character.toString(c);
      }
    }
    if (StringUtils.isNotBlank(s)) {
      splitted.add(s);
    }
    return splitted;
  }

  private static Version resolveLowVersion(String versionStr, String range, String pluginKey) {
    if (LATEST_KEYWORD.equals(versionStr)) {
      throw new SonarVersionRangeException(String.format(
        "Cannot use LATEST keyword at the start of a range in '%s' (in plugin '%s'). Use 'sqVersions=LATEST' instead.",
        range,
        pluginKey
      ));
    }

    if (versionStr.endsWith("*")) {
      throw new SonarVersionRangeException(String.format(
        "Cannot use a wildcard version at the start of a range in '%s' (in plugin '%s'). " +
          "If you want to mark this range as compatible with any MAJOR.MINOR.* version, use the MAJOR.MINOR version instead " +
          "(e.g.: 'sqVersions=[6.7,6.7.*]', 'sqVersions=[6.7,LATEST]').",
        range,
        pluginKey
      ));
    }

    return Version.create(versionStr);
  }

  private static Version resolveKeywordAndStar(String versionStr, Sonar sonar, String pluginKey) {
    if (LATEST_KEYWORD.equals(versionStr)) {
      return Version.create(sonar.getAllReleases().last().getVersion(), LATEST_KEYWORD);
    } else if (versionStr.endsWith("*")) {
      return resolveWithWildcard(versionStr, sonar, pluginKey);
    }
    return Version.create(versionStr);
  }

  private static Version resolveWithWildcard(String versionStr, Sonar sonar, String pluginKey) {
    String prefix = versionStr.substring(0, versionStr.length() - 1);
    String prefixWithoutDot = prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
    Release found = null;
    for (Release r : sonar.getAllReleases()) {
      if (r.getVersion().toString().equals(prefixWithoutDot) || r.getVersion().toString().startsWith(prefix)) {
        found = r;
      }
    }
    if (found != null) {
      return Version.create(found.getVersion(), "*");
    } else {
      throw new IllegalStateException(String.format("Unable to resolve version '%s' (in plugin '%s')", versionStr, pluginKey));
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

  private static String get(Properties props, String key) {
    return StringUtils.defaultIfEmpty(props.getProperty(key), null);
  }

  private static String getOrDefault(Properties props, String key, String defaultKey) {
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

  private static String[] getArray(Properties props, String key) {
    return StringUtils.split(StringUtils.defaultIfEmpty(props.getProperty(key), ""), ",");
  }

  private static String[] getArray(Properties p, String pluginKey, String field) {
    return getArray(p, pluginKey + "." + field);
  }

}
