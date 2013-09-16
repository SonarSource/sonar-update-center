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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.updatecenter.common.UpdateCenterDeserializer.CHANGELOG_URL_SUFFIX;
import static org.sonar.updatecenter.common.UpdateCenterDeserializer.DATE_SUFFIX;
import static org.sonar.updatecenter.common.UpdateCenterDeserializer.DESCRIPTION_SUFFIX;
import static org.sonar.updatecenter.common.UpdateCenterDeserializer.DOWNLOAD_URL_SUFFIX;
import static org.sonar.updatecenter.common.UpdateCenterDeserializer.SONAR_PREFIX;

public final class UpdateCenterSerializer {

  private UpdateCenterSerializer() {
  }

  private static void set(Properties props, String key, String value) {
    if (StringUtils.isNotBlank(value)) {
      props.setProperty(key, value);
    }
  }

  private static void set(Properties props, String key, Collection values) {
    if (values != null && !values.isEmpty()) {
      props.setProperty(key, StringUtils.join(values, ","));
    }
  }

  private static void set(Properties props, Plugin plugin, String key, String value) {
    if (StringUtils.isNotBlank(value)) {
      props.setProperty(plugin.getKey() + "." + key, value);
    }
  }

  private static void set(Properties props, Plugin plugin, String key, Collection values) {
    if (values != null && !values.isEmpty()) {
      props.setProperty(plugin.getKey() + "." + key, StringUtils.join(values, ","));
    }
  }

  public static Properties toProperties(UpdateCenter center) {
    Properties p = new Properties();
    set(p, "date", FormatUtils.toString(center.getDate(), true));
    set(p, "sonar.versions", center.getSonar().getVersions());
    if (center.getSonar().getLtsRelease() != null) {
      set(p, "sonar.ltsVersion", center.getSonar().getLtsRelease().getVersion().toString());
    }

    for (Release sonarRelease : center.getSonar().getReleases()) {
      set(p, SONAR_PREFIX + sonarRelease.getVersion() + DOWNLOAD_URL_SUFFIX, sonarRelease.getDownloadUrl());
      set(p, SONAR_PREFIX + sonarRelease.getVersion() + CHANGELOG_URL_SUFFIX, sonarRelease.getChangelogUrl());
      set(p, SONAR_PREFIX + sonarRelease.getVersion() + DESCRIPTION_SUFFIX, sonarRelease.getDescription());
      set(p, SONAR_PREFIX + sonarRelease.getVersion() + DATE_SUFFIX, FormatUtils.toString(sonarRelease.getDate(), false));
    }

    List<String> pluginKeys = newArrayList();
    for (Plugin plugin : center.getUpdateCenterPluginReferential().getPlugins()) {
      addPlugin(plugin, pluginKeys, p);
    }
    set(p, "plugins", pluginKeys);
    return p;
  }

  private static void addPlugin(Plugin plugin, List<String> pluginKeys, Properties p) {
    pluginKeys.add(plugin.getKey());
    set(p, plugin, "name", plugin.getName());
    set(p, plugin, "description", plugin.getDescription());
    set(p, plugin, "category", plugin.getCategory());
    set(p, plugin, "homepageUrl", plugin.getHomepageUrl());
    set(p, plugin, "license", plugin.getLicense());
    set(p, plugin, "organization", plugin.getOrganization());
    set(p, plugin, "organizationUrl", plugin.getOrganizationUrl());
    set(p, plugin, "termsConditionsUrl", plugin.getTermsConditionsUrl());
    set(p, plugin, "issueTrackerUrl", plugin.getIssueTrackerUrl());
    set(p, plugin, "scm", plugin.getSourcesUrl());
    set(p, plugin, "developers", StringUtils.join(plugin.getDevelopers(), ","));

    List<String> releaseKeys = new ArrayList<String>();
    for (Release release : plugin.getReleases()) {
      releaseKeys.add(release.getVersion().toString());
      if (release.getParent() != null) {
        set(p, plugin, release.getVersion() + ".parent", release.getParent().getKey());
      }
      set(p, plugin, release.getVersion() + ".requiredSonarVersions", StringUtils.join(release.getRequiredSonarVersions(), ","));
      set(p, plugin, release.getVersion() + DOWNLOAD_URL_SUFFIX, release.getDownloadUrl());
      set(p, plugin, release.getVersion() + CHANGELOG_URL_SUFFIX, release.getChangelogUrl());
      set(p, plugin, release.getVersion() + DESCRIPTION_SUFFIX, release.getDescription());
      set(p, plugin, release.getVersion() + DATE_SUFFIX, FormatUtils.toString(release.getDate(), false));
      set(p, plugin, release.getVersion() + ".requirePlugins", StringUtils.join(getRequiredList(release), ","));
    }
    set(p, plugin, "versions", releaseKeys);
  }

  public static void toProperties(UpdateCenter sonar, File toFile) {
    FileOutputStream output = null;
    try {
      output = FileUtils.openOutputStream(toFile);
      toProperties(sonar).store(output, "Generated file");

    } catch (IOException e) {
      throw new IllegalStateException("Fail to store update center properties to: " + toFile.getAbsolutePath(), e);

    } finally {
      IOUtils.closeQuietly(output);
    }
  }

  private static String[] getRequiredList(Release release) {
    List<String> requiredStringList = newArrayList();
    for (Release requiredRelease : release.getOutgoingDependencies()) {
      requiredStringList.add(requiredRelease.getArtifact().getKey() + ":" + requiredRelease.getVersion().getName());
    }
    return requiredStringList.toArray(new String[] {});
  }
}
