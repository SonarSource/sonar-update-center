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
import java.util.Properties;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.updatecenter.common.FormatUtils.toDate;

public final class UpdateCenterDeserializer {

  private UpdateCenterDeserializer() {
    // only static methods
  }

  public static UpdateCenter fromProperties(File file) throws IOException {
    FileInputStream in = FileUtils.openInputStream(file);
    try {
      Properties props = new Properties();
      props.load(in);
      UpdateCenter pluginReferential = fromProperties(props);
      pluginReferential.setDate(new Date(file.lastModified()));
      return pluginReferential;

    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  public static UpdateCenter fromProperties(Properties p) {
    Sonar sonar = new Sonar();
    Date date = FormatUtils.toDate(p.getProperty("date"), true);
    List<Plugin> plugins = newArrayList();

    String[] sonarVersions = getArray(p, "sonar.versions");
    for (String sonarVersion : sonarVersions) {
      Release release = new Release(sonar, sonarVersion);
      String sonarPrefix = "sonar.";
      release.setChangelogUrl(get(p, sonarPrefix + sonarVersion + ".changelogUrl"));
      release.setDescription(get(p, sonarPrefix + sonarVersion + ".description"));
      release.setDownloadUrl(get(p, sonarPrefix + sonarVersion + ".downloadUrl"));
      release.setDate(FormatUtils.toDate(get(p, "sonar." + sonarVersion + ".date"), false));
      sonar.addRelease(release);
    }

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

      String[] pluginReleases = StringUtils.split(StringUtils.defaultIfEmpty(get(p, pluginKey, "versions"), ""), ",");
      for (String pluginVersion : pluginReleases) {
        Release release = new Release(plugin, pluginVersion);
        plugin.addRelease(release);
        release.setDownloadUrl(get(p, pluginKey, pluginVersion + ".downloadUrl"));
        release.setChangelogUrl(get(p, pluginKey, pluginVersion + ".changelogUrl"));
        release.setDescription(get(p, pluginKey, pluginVersion + ".description"));
        release.setDate(toDate(get(p, pluginKey, pluginVersion + ".date"), false));
        String[] requiredSonarVersions = getRequiredSonarVersions(p, pluginKey, pluginVersion, sonar.getReleases());
        for (String requiredSonarVersion : requiredSonarVersions) {
          release.addRequiredSonarVersions(Version.create(requiredSonarVersion));
        }
      }
      plugins.add(plugin);
    }

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

  private static String[] getRequiredSonarVersions(Properties p, String pluginKey, String pluginVersion, SortedSet<Release> sonarReleases) {
    List<String> patterns = split(StringUtils.defaultIfEmpty(get(p, pluginKey, pluginVersion + ".requiredSonarVersions"), ""));
    List<String> result = new LinkedList<String>();
    for (String pattern : patterns) {
      if (pattern != null) {
        Matcher matcher = Pattern.compile("\\[(.*),(.*)\\]").matcher(pattern);
        if (matcher.matches()) {
          final Version low = Version.create(resolve(matcher.group(1), sonarReleases));
          final Version high = Version.create(resolve(matcher.group(2), sonarReleases));
          Collection<Version> versions = Collections2.filter(transform(sonarReleases, new Function<Release, Version>() {
            public Version apply(Release release) {
              return release.getVersion();
            }
          }), new Predicate<Version>() {
            public boolean apply(Version version) {
              return version.compareTo(low) >= 0 && version.compareTo(high) <= 0;
            }
          });
          for (Version version : versions) {
            result.add(version.toString());
          }
        }
        else {
          result.add(resolve(pattern, sonarReleases));
        }
      }
    }
    return result.toArray(new String[result.size()]);
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

  private static String resolve(String version, SortedSet<Release> sonarReleases) {
    if ("LATEST".equals(version)) {
      return sonarReleases.last().getVersion().toString();
    }
    return version;
  }

  private static String get(Properties props, String key) {
    return StringUtils.defaultIfEmpty(props.getProperty(key), null);
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
