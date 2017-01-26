/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2017 SonarSource SA
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

import static com.google.common.collect.Lists.newArrayList;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Splitter;

public class PluginReferentialManifestConverter {

  private PluginReferentialManifestConverter() {
    // only static methods
  }

  public static PluginReferential fromPluginManifests(List<PluginManifest> pluginManifestList) {
    List<Plugin> plugins = newArrayList();
    for (PluginManifest pluginManifest : pluginManifestList) {
      Plugin plugin = Plugin.factory(pluginManifest.getKey());
      plugin.merge(pluginManifest);

      Release release = new Release(plugin, pluginManifest.getVersion());
      release.addRequiredSonarVersions(Version.create(pluginManifest.getSonarVersion()));
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
            Iterator<String> split = Splitter.on(':').split(requiresPluginKey).iterator();
            String requiredPluginReleaseKey = split.next();
            String requiredMinimumReleaseVersion = split.next();
            pluginReferential.addOutgoingDependency(release, requiredPluginReleaseKey, requiredMinimumReleaseVersion);
          });
        }
      }
    }
    return pluginReferential;
  }

}
