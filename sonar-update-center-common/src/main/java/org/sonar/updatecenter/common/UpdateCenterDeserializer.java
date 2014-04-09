/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
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
  public static final String CHANGELOG_URL_SUFFIX = ".changelogUrl";
  public static final String DOWNLOAD_URL_SUFFIX = ".downloadUrl";
  public static final String SONAR_PREFIX = "sonar.";
  public static final String PLUGINS = "plugins";

  private UpdateCenterDeserializer() {
    // only static methods
  }

  public static enum Mode {
    PROD,
    DEV
  }

  public static UpdateCenter fromProperties(File file) throws IOException {
    return fromProperties(file, Mode.PROD);
  }

  public static UpdateCenter fromProperties(File file, Mode mode) throws IOException {
    FileInputStream in = FileUtils.openInputStream(file);
    try {
      Properties props = new Properties();
      props.load(in);
      loadPluginProperties(file, props);
      UpdateCenter pluginReferential = fromProperties(props, mode);
      pluginReferential.setDate(new Date(file.lastModified()));
      return pluginReferential;

    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  private static void loadPluginProperties(File file, Properties props) throws IOException {
    String[] pluginKeys = getArray(props, "plugins");
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

  public static UpdateCenter fromProperties(Properties p) {
    return fromProperties(p, Mode.PROD);
  }

  public static UpdateCenter fromProperties(Properties p, Mode mode) {
    Sonar sonar = new Sonar();
    Date date = FormatUtils.toDate(p.getProperty("date"), true);
    List<Plugin> plugins = newArrayList();

    parseSonar(p, mode, sonar);

    parsePlugins(p, mode, sonar, plugins);

    PluginReferential pluginReferential = PluginReferential.create(plugins);
    for (Plugin plugin : pluginReferential.getPlugins()) {
      for (Release release : plugin.getReleases()) {
        String parentKey = get(p, plugin.getKey(), release.getVersion().getName() + ".parent");
        if (parentKey != null) {
          pluginReferential.setParent(release, parentKey);
        }

        String[] requiredReleases = StringUtils.split(StringUtils.defaultIfEmpty(get(p, plugin.getKey(), release.getVersion().getName() + ".requirePlugins"), ""), ",");
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

  private static void parsePlugins(Properties p, Mode mode, Sonar sonar, List<Plugin> plugins) {
    String[] pluginKeys = getArray(p, "plugins");
    for (String pluginKey : pluginKeys) {
      Plugin plugin = new Plugin(pluginKey);
      plugin.setName(get(p, pluginKey, "name"));
      plugin.setDescription(get(p, pluginKey, "description"));
      plugin.setCategory(get(p, pluginKey, "category"));
      plugin.setHomepageUrl(get(p, pluginKey, "homepageUrl"));
      plugin.setLicense(get(p, pluginKey, "license"));
      plugin.setOrganization(get(p, pluginKey, "organization"));
      plugin.setOrganizationUrl(get(p, pluginKey, "organizationUrl"));
      plugin.setTermsConditionsUrl(get(p, pluginKey, "termsConditionsUrl"));
      plugin.setIssueTrackerUrl(get(p, pluginKey, "issueTrackerUrl"));
      plugin.setSourcesUrl(get(p, pluginKey, "scm"));
      plugin.setDevelopers(newArrayList(getArray(p, pluginKey, "developers")));

      if (mode == Mode.DEV) {
        parsePluginDevVersions(p, pluginKey, plugin);
      }

      String[] pluginReleases = getArray(p, pluginKey, "publicVersions");
      for (String pluginVersion : pluginReleases) {
        Release release = new Release(plugin, pluginVersion);
        release.setPublic(true);
        plugin.addRelease(release);
        release.setDownloadUrl(getOrFail(p, pluginKey, pluginVersion + DOWNLOAD_URL_SUFFIX));
        release.setChangelogUrl(getOrFail(p, pluginKey, pluginVersion + CHANGELOG_URL_SUFFIX));
        release.setDescription(getOrFail(p, pluginKey, pluginVersion + DESCRIPTION_SUFFIX));
        release.setDate(toDate(getOrFail(p, pluginKey, pluginVersion + DATE_SUFFIX), false));
        String[] requiredSonarVersions = getRequiredSonarVersions(p, pluginKey, pluginVersion, sonar);
        for (String requiredSonarVersion : requiredSonarVersions) {
          release.addRequiredSonarVersions(Version.create(requiredSonarVersion));
        }
      }
      plugins.add(plugin);
    }
  }

  private static void parsePluginDevVersions(Properties p, String pluginKey, Plugin plugin) {
    String devVersion = get(p, pluginKey, "devVersion");
    plugin.setDevRelease(devVersion);
  }

  private static void parseSonar(Properties p, Mode mode, Sonar sonar) {
    parseSonarVersions(p, sonar, mode);
    if (mode == Mode.DEV) {
      parseSonarDevVersions(p, sonar);
    }
    parseSonarLtsVersion(p, sonar);
  }

  private static void parseSonarDevVersions(Properties p, Sonar sonar) {
    String devVersion = getOrFail(p, "devVersion");
    sonar.setDevRelease(devVersion);
  }

  private static void parseSonarLtsVersion(Properties p, Sonar sonar) {
    String ltsVersion = getOrFail(p, "ltsVersion");
    sonar.setLtsRelease(ltsVersion);
    if (!sonar.getReleases().contains(sonar.getLtsRelease())) {
      throw new IllegalStateException("ltsVersion seems wrong as it is not listed in SonarQube versions");
    }
  }

  private static void parseSonarVersions(Properties p, Sonar sonar, Mode mode) {
    parseSonarVersions(p, sonar, "publicVersions", true);
    if (mode == Mode.DEV) {
      parseSonarVersions(p, sonar, "privateVersions", false);
    }
  }

  private static void parseSonarVersions(Properties p, Sonar sonar, String key, boolean isPublicRelease) {
    for (String sonarVersion : getArray(p, key)) {
      Release release = new Release(sonar, sonarVersion);
      release.setPublic(isPublicRelease);
      release.setChangelogUrl(get(p, sonarVersion + CHANGELOG_URL_SUFFIX, isPublicRelease));
      release.setDescription(get(p, sonarVersion + DESCRIPTION_SUFFIX, isPublicRelease));
      release.setDownloadUrl(get(p, sonarVersion + DOWNLOAD_URL_SUFFIX, isPublicRelease));
      release.setDate(FormatUtils.toDate(get(p, sonarVersion + DATE_SUFFIX, isPublicRelease), false));
      sonar.addRelease(release);
    }
  }

  private static String[] getRequiredSonarVersions(Properties p, String pluginKey, String pluginVersion, Sonar sonar) {
    String sqVersions = get(p, pluginKey, pluginVersion + ".sqVersions");
    List<String> patterns = split(StringUtils.defaultIfEmpty(sqVersions, ""));
    List<String> result = new LinkedList<String>();
    for (String pattern : patterns) {
      if (pattern != null) {
        Matcher matcher = Pattern.compile("\\[(.*),(.*)\\]").matcher(pattern);
        if (matcher.matches()) {
          final Version low = Version.create(resolve(matcher.group(1), sonar));
          final Version high = Version.create(resolve(matcher.group(2), sonar));
          resolveRange(sonar, result, low, high);
        } else {
          result.add(resolve(pattern, sonar));
        }
      }
    }
    return result.toArray(new String[result.size()]);
  }

  private static void resolveRange(Sonar sonar, List<String> result, final Version low, final Version high) {
    Collection<Version> versions = Collections2.filter(transform(sonar.getAllReleases(), new Function<Release, Version>() {
      public Version apply(Release release) {
        return release != null ? release.getVersion() : null;
      }
    }), new Predicate<Version>() {
      public boolean apply(Version version) {
        return version != null && version.compareTo(low) >= 0 && version.compareTo(high) <= 0;
      }
    });
    for (Version version : versions) {
      result.add(version.toString());
    }
  }

  private static List<String> split(String requiredSonarVersions) {
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
    splitted.add(s);
    return splitted;
  }

  private static String resolve(String version, Sonar sonar) {
    if ("LATEST".equals(version)) {
      return sonar.getAllReleases().last().getVersion().toString();
    }
    return version;
  }

  private static String get(Properties props, String key, boolean validate) {
    return validate ? getOrFail(props, key) : get(props, key);
  }

  private static String getOrFail(Properties props, String key) {
    String value = props.getProperty(key);
    if (StringUtils.isBlank(value)) {
      throw new IllegalStateException(key + " should be defined");
    }
    return value;
  }

  private static String get(Properties props, String key) {
    return StringUtils.defaultIfEmpty(props.getProperty(key), null);
  }

  private static String getOrFail(Properties p, String pluginKey, String field) {
    return getOrFail(p, pluginKey + "." + field);
  }

  private static String get(Properties p, String pluginKey, String field) {
    return get(p, pluginKey + "." + field);
  }

  private static String[] getArray(Properties props, String key) {
    return StringUtils.split(StringUtils.defaultIfEmpty(props.getProperty(key), ""), ",");
  }

  private static String[] getArray(Properties p, String pluginKey, String field) {
    return getArray(p, pluginKey + "." + field);
  }

}
