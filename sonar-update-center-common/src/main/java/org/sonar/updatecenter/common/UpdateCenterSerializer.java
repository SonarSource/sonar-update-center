/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2021 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

import static org.sonar.updatecenter.common.UpdateCenterDeserializer.CHANGELOG_URL_SUFFIX;
import static org.sonar.updatecenter.common.UpdateCenterDeserializer.DATE_SUFFIX;
import static org.sonar.updatecenter.common.UpdateCenterDeserializer.DESCRIPTION_SUFFIX;
import static org.sonar.updatecenter.common.UpdateCenterDeserializer.DISPLAY_VERSION_SUFFIX;
import static org.sonar.updatecenter.common.UpdateCenterDeserializer.DOWNLOAD_URL_SUFFIX;
import static org.sonar.updatecenter.common.UpdateCenterDeserializer.MAVEN_ARTIFACTID_SUFFIX;
import static org.sonar.updatecenter.common.UpdateCenterDeserializer.MAVEN_GROUPID_SUFFIX;
import static org.sonar.updatecenter.common.UpdateCenterDeserializer.SONAR_PREFIX;
import static org.sonar.updatecenter.common.UpdateCenterDeserializer.getDownloadUrlSuffix;

public final class UpdateCenterSerializer {

  private UpdateCenterSerializer() {
  }

  private static void set(Properties props, String key, String value) {
    if (StringUtils.isNotBlank(value)) {
      props.setProperty(key, value);
    }
  }

  private static void set(Properties props, String key, @Nullable Collection values) {
    if (values != null && !values.isEmpty()) {
      props.setProperty(key, StringUtils.join(values, ","));
    }
  }

  private static void set(Properties props, Plugin plugin, String key, String value) {
    if (StringUtils.isNotBlank(value)) {
      props.setProperty(plugin.getKey() + "." + key, value);
    }
  }

  private static void set(Properties props, Plugin plugin, String key, @Nullable Collection values) {
    if (values != null && !values.isEmpty()) {
      props.setProperty(plugin.getKey() + "." + key, StringUtils.join(values, ","));
    }
  }

  public static Properties toProperties(UpdateCenter center) {
    Properties p = new Properties();
    set(p, "date", FormatUtils.toString(center.getDate(), true));
    set(p, "publicVersions", center.getSonar().getPublicVersions());
    if (!center.getSonar().getPrivateVersions().isEmpty()) {
      set(p, "privateVersions", center.getSonar().getPrivateVersions());
    }
    if (center.getSonar().getDevRelease() != null) {
      set(p, "devVersion", center.getSonar().getDevRelease().getVersion().toString());
    }
    // For backward compatibility
    set(p, "sonar.versions", center.getSonar().getVersions());
    if (center.getSonar().getLtsRelease() != null) {
      set(p, "ltsVersion", center.getSonar().getLtsRelease().getVersion().toString());
    }

    for (Release sonarRelease : center.getSonar().getAllReleases()) {
      set(p, sonarRelease.getVersion() + CHANGELOG_URL_SUFFIX, sonarRelease.getChangelogUrl());
      set(p, sonarRelease.getVersion() + DISPLAY_VERSION_SUFFIX, sonarRelease.getDisplayVersion());
      set(p, sonarRelease.getVersion() + DESCRIPTION_SUFFIX, sonarRelease.getDescription());
      set(p, sonarRelease.getVersion() + DATE_SUFFIX, FormatUtils.toString(sonarRelease.getDate(), false));

      for (Release.Edition edition: Release.Edition.values()) {
        String downloadUrl = sonarRelease.getDownloadUrl(edition);
        if (downloadUrl != null) {
          set(p, sonarRelease.getVersion() + getDownloadUrlSuffix(edition), downloadUrl);
        }
      }

      // For backward compatibility
      set(p, SONAR_PREFIX + sonarRelease.getVersion() + DOWNLOAD_URL_SUFFIX, sonarRelease.getDownloadUrl());
      set(p, SONAR_PREFIX + sonarRelease.getVersion() + CHANGELOG_URL_SUFFIX, sonarRelease.getChangelogUrl());
      set(p, SONAR_PREFIX + sonarRelease.getVersion() + DESCRIPTION_SUFFIX, sonarRelease.getDescription());
      set(p, SONAR_PREFIX + sonarRelease.getVersion() + DATE_SUFFIX, FormatUtils.toString(sonarRelease.getDate(), false));
    }

    List<String> pluginKeys = new ArrayList<>();
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

    for (Release release : plugin.getAllReleases()) {
      set(p, plugin, release.getVersion() + ".sqVersions", StringUtils.join(release.getRequiredSonarVersions(), ","));
      // For backward compatibility
      set(p, plugin, release.getVersion() + ".requiredSonarVersions", StringUtils.join(release.getRequiredSonarVersions(), ","));

      set(p, plugin, release.getVersion() + DOWNLOAD_URL_SUFFIX, release.getDownloadUrl());
      set(p, plugin, release.getVersion() + CHANGELOG_URL_SUFFIX, release.getChangelogUrl());
      set(p, plugin, release.getVersion() + DISPLAY_VERSION_SUFFIX, release.getDisplayVersion());
      set(p, plugin, release.getVersion() + DESCRIPTION_SUFFIX, release.getDescription());
      set(p, plugin, release.getVersion() + MAVEN_GROUPID_SUFFIX, release.groupId());
      set(p, plugin, release.getVersion() + MAVEN_ARTIFACTID_SUFFIX, release.artifactId());
      set(p, plugin, release.getVersion() + DATE_SUFFIX, FormatUtils.toString(release.getDate(), false));
      set(p, plugin, release.getVersion() + ".requirePlugins", StringUtils.join(getRequiredList(release), ","));
    }
    set(p, plugin, "publicVersions", plugin.getPublicVersions());
    if (!plugin.getPrivateVersions().isEmpty()) {
      set(p, plugin, "privateVersions", plugin.getPrivateVersions());
    }
    if (!plugin.getArchivedVersions().isEmpty()) {
      set(p, plugin, "archivedVersions", plugin.getArchivedVersions());
    }
    if (plugin.getDevRelease() != null) {
      // Some Plugins don't have dev version
      set(p, plugin, "devVersion", plugin.getDevRelease().getVersion().toString());
    }
    // For backward compatibility
    set(p, plugin, "versions", plugin.getVersions());
  }

  public static void toProperties(UpdateCenter sonar, File toFile) {
    try (OutputStream output = Files.newOutputStream(toFile.toPath())) {
      toProperties(sonar).store(output, "Generated file");

    } catch (IOException e) {
      throw new IllegalStateException("Fail to store update center properties to: " + toFile.getAbsolutePath(), e);
    }
  }

  private static String[] getRequiredList(Release release) {
    List<String> requiredStringList = new ArrayList<>();
    for (Release requiredRelease : release.getOutgoingDependencies()) {
      requiredStringList.add(requiredRelease.getArtifact().getKey() + ":" + requiredRelease.getVersion().getName());
    }
    return requiredStringList.toArray(new String[] {});
  }
}
