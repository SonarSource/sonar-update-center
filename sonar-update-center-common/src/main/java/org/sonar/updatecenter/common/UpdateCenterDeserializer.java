/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.updatecenter.common.FormatUtils.toDate;

public final class UpdateCenterDeserializer {

  private UpdateCenterDeserializer() {
    // only static methods
  }

  public static PluginReferential fromProperties(File file) throws IOException {
    FileInputStream in = FileUtils.openInputStream(file);
    try {
      Properties props = new Properties();
      props.load(in);
      PluginReferential pluginReferential = fromProperties(props);
      pluginReferential.setDate(new Date(file.lastModified()));
      return pluginReferential;

    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  public static PluginReferential fromProperties(Properties p) {
    Sonar sonar = new Sonar();
    Date date = FormatUtils.toDate(p.getProperty("date"), true);
    List<Plugin> plugins = newArrayList();

    String[] sonarVersions = getArray(p, "sonar.versions");
    for (String sonarVersion : sonarVersions) {
      Release release = new Release(sonar, sonarVersion);
      release.setChangelogUrl(get(p, "sonar." + sonarVersion + ".changelogUrl"));
      release.setDescription(get(p, "sonar." + sonarVersion + ".description"));
      release.setDownloadUrl(get(p, "sonar." + sonarVersion + ".downloadUrl"));
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
        String[] requiredSonarVersions = StringUtils.split(StringUtils.defaultIfEmpty(get(p, pluginKey, pluginVersion + ".requiredSonarVersions"), ""), ",");
        for (String requiredSonarVersion : requiredSonarVersions) {
          release.addRequiredSonarVersions(Version.create(requiredSonarVersion));
        }
      }
      plugins.add(plugin);
    }

    for (Plugin plugin : plugins) {
      plugin.setParent(getPlugin(get(p, plugin.getKey(), "parent"), plugins));
    }

    PluginReferential pluginReferential = PluginReferential.create(plugins, sonar, date);
    for (Plugin plugin : plugins) {
      for (String requiresPluginKey : getArray(p, plugin.getKey(), "requiresPlugins")) {
        Iterator<String> split = Splitter.on(':').split(requiresPluginKey).iterator();
        String requiredPluginReleaseKey = split.next();
        String requiredMinimumReleaseVersion = split.next();
        Release requiredRelease = pluginReferential.findRelease(requiredPluginReleaseKey, requiredMinimumReleaseVersion);
        if (requiredRelease != null) {
          plugin.addRequired(requiredRelease);
        } else {
          throw new RuntimeException("Plugin not found : '"+ requiredPluginReleaseKey + "' with minimum version "+ requiredMinimumReleaseVersion);
        }
      }
    }

    return pluginReferential;
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

  @Nullable
  public static Plugin getPlugin(final String key, List<Plugin> plugins) {
    return Iterables.find(plugins, new Predicate<Plugin>() {
      public boolean apply(Plugin input) {
        return input.getKey().equals(key);
      }
    }, null);
  }

}
