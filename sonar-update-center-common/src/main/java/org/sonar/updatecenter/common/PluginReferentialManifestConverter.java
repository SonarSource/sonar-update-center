/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.updatecenter.common;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

public class PluginReferentialManifestConverter {

  private PluginReferentialManifestConverter() {
    // only static methods
  }

  public static PluginReferential fromPluginManifests(List<PluginManifest> pluginManifestList) {
    List<Plugin> plugins = new ArrayList<>();
    for (PluginManifest pluginManifest : pluginManifestList) {
      Plugin plugin = Plugin.factory(pluginManifest.getKey());
      plugin.merge(pluginManifest);

      Release release = new Release(plugin, pluginManifest.getVersion());
      release.addRequiredSonarVersions(Product.OLD_SONARQUBE, Version.create(pluginManifest.getSonarVersion()));
      release.addRequiredSonarVersions(Product.SONARQUBE_COMMUNITY_BUILD, Version.create(pluginManifest.getSonarVersion()));
      release.addRequiredSonarVersions(Product.SONARQUBE_SERVER, Version.create(pluginManifest.getSonarVersion()));
      release.setDisplayVersion(pluginManifest.getDisplayVersion());
      plugin.addRelease(release);
      plugins.add(plugin);
    }

    PluginReferential pluginReferential = PluginReferential.create(plugins);

    for (PluginManifest pluginManifest : pluginManifestList) {
      Plugin plugin = pluginReferential.findPlugin(pluginManifest.getKey());
      for (String requiresPluginKey : pluginManifest.getRequirePlugins()) {
        if (StringUtils.isNotBlank(requiresPluginKey)) {
          plugin.getReleases().forEach(release -> {
            String[] split = requiresPluginKey.split(":");
            String requiredPluginReleaseKey = split[0];
            String requiredMinimumReleaseVersion = split[1];
            pluginReferential.addOutgoingDependency(release, requiredPluginReleaseKey, requiredMinimumReleaseVersion);
          });
        }
      }
    }
    return pluginReferential;
  }

}
