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

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class PluginReferentialManifestConverter {

  private PluginReferentialManifestConverter() {
    // only static methods
  }

  public static PluginReferential fromPluginManifests(List<PluginManifest> pluginManifestList, Version sonarVersion) {
    List<Plugin> plugins = newArrayList();

    for (PluginManifest pluginManifest : pluginManifestList) {
      Plugin plugin = new Plugin(pluginManifest.getKey());
      plugin.merge(pluginManifest);

      Release release = new Release(plugin, pluginManifest.getVersion());
      release.addRequiredSonarVersions(Version.create(pluginManifest.getSonarVersion()));
      plugin.addRelease(release);
      plugins.add(plugin);
    }

    for (PluginManifest pluginManifest : pluginManifestList) {
      Plugin plugin = getPlugin(pluginManifest.getKey(), plugins);
      Plugin pluginParent = getPlugin(pluginManifest.getParent(), plugins);
      if (pluginParent != null && !pluginParent.getKey().equals(plugin.getKey())) {
        plugin.setParent(pluginParent);
      }
    }

    for (PluginManifest pluginManifest : pluginManifestList) {
      Plugin plugin = getPlugin(pluginManifest.getKey(), plugins);
      for (String requiresPluginKey : pluginManifest.getRequiresPlugins()) {
        for (Release release : plugin.getReleases()) {
          Iterator<String> split = Splitter.on(':').split(requiresPluginKey).iterator();
          String requiredPluginReleaseKey = split.next();
          String requiredMinimumReleaseVersion = split.next();
          Release requiredRelease = getPlugin(requiredPluginReleaseKey, plugins).getRelease(Version.create(requiredMinimumReleaseVersion));
          // TODO throw exception if requiredRelease not found
          release.addOutgoingDependency(requiredRelease);
        }
      }
    }

    Sonar sonar = new Sonar();
    sonar.addRelease(sonarVersion);
    return PluginReferential.create(plugins, sonar, new Date());
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
