/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2022 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.sonar.updatecenter.common.exception.DependencyCycleException;
import org.sonar.updatecenter.common.exception.IncompatiblePluginVersionException;
import org.sonar.updatecenter.common.exception.PluginNotFoundException;

public class PluginReferential {

  private Set<Plugin> plugins;

  private PluginReferential() {
    this.plugins = new TreeSet<>();
  }

  public static PluginReferential create(List<Plugin> pluginList) {
    PluginReferential pluginReferential = new PluginReferential();
    for (Plugin plugin : pluginList) {
      pluginReferential.add(plugin);
    }
    return pluginReferential;
  }

  public static PluginReferential createEmpty() {
    return PluginReferential.create(new ArrayList<>());
  }

  /**
   * @return the list of plugins where last releases is master releases
   */
  public List<Plugin> getLastMasterReleasePlugins() {
    return plugins.stream()
      .filter(plugin -> plugin.getLastRelease() != null)
      .collect(Collectors.toList());
  }

  public List<Plugin> getPlugins() {
    return new ArrayList<>(plugins);
  }

  /**
   * @throws NoSuchElementException if plugin could not be found
   */
  public Plugin findPlugin(String key) {
    return plugins.stream()
      .filter(plugin -> plugin.getKey().equals(key))
      .findFirst()
      .orElseThrow(() -> new NoSuchElementException("Unable to find plugin with key " + key));
  }

  public boolean doesContainPlugin(String key) {
    return plugins.stream()
      .anyMatch(plugin -> plugin.getKey().equals(key));
  }

  public boolean doesContainRelease(final String key, Version version) {
    for (Plugin plugin : plugins) {
      if (plugin.getKey().equals(key) && plugin.doesContainVersion(version)) {
        return true;
      }
    }
    return false;
  }

  public List<String> findLastReleasesWithDependencies(String pluginKey) {
    List<String> removablePlugins = new ArrayList<>();
    Plugin plugin = findPlugin(pluginKey);
    if (plugin != null) {
      Release pluginRelease = plugin.getLastRelease();
      if (pluginRelease != null) {
        removablePlugins.add(plugin.getKey());
        for (Release incomingDependencies : pluginRelease.getIncomingDependencies()) {
          removablePlugins.addAll(findLastReleasesWithDependencies(incomingDependencies.getArtifact().getKey()));
        }
      }
    }
    return removablePlugins;
  }

  public void addOutgoingDependency(Release release, String requiredPluginReleaseKey, String requiredMinimumReleaseVersion) {

    // skip dependencies on license, as it's deprecated and is provided by SQ anyway. There is no need
    // to have the explicit dependency on the update center side.
    // Once we removed plugins < 8.9, and bundled plugins are removed from the matrix, we can drop this if
    if("license".equals(requiredPluginReleaseKey)){
      return;
    }

    try {
      Plugin requiredPlugin = findPlugin(requiredPluginReleaseKey);
      Release minimalRequiredRelease = requiredPlugin.getMinimalRelease(Version.create(requiredMinimumReleaseVersion));
      if (minimalRequiredRelease != null) {
        release.addOutgoingDependency(minimalRequiredRelease);
        minimalRequiredRelease.addIncomingDependency(release);
        checkDependencyCycle(release);
      } else {
        Release latest = requiredPlugin.getLastRelease();
        if (latest != null) {
          throw new IncompatiblePluginVersionException(String.format("The plugin '%s' is in version %s whereas the plugin '%s' requires a least a version %s.",
            requiredPlugin.getKey(), latest.getVersion().getName(), release.getArtifact().getKey(), requiredMinimumReleaseVersion));
        }
      }
    } catch (NoSuchElementException e) {
      throw new PluginNotFoundException(String.format("The plugin '%s' required by '%s' is missing.", requiredPluginReleaseKey, release.getArtifact().getKey()), e);
    }
  }

  private void checkDependencyCycle(Release release) {
    List<Release> releases = new ArrayList<>();
    try {
      checkDependencyCycle(release, releases);
    } catch (DependencyCycleException e) {
      String releaseKeys = releases.stream()
        .map(rel -> rel.getArtifact().getKey())
        .collect(Collectors.joining("', '"));
      throw new DependencyCycleException("There is a dependency cycle between plugins '" + releaseKeys + "' that must be cut.", e);
    }
  }

  private static void checkDependencyCycle(Release release, List<Release> releases) {
    for (Release outgoingDependency : release.getOutgoingDependencies()) {
      if (releases.contains(outgoingDependency)) {
        throw new DependencyCycleException();
      }
      releases.add(outgoingDependency);
      checkDependencyCycle(outgoingDependency, releases);
    }
  }

  List<Release> getLastMasterReleases() {
    List<Release> releases = new ArrayList<>();
    for (Plugin plugin : getLastMasterReleasePlugins()) {
      releases.add(plugin.getLastRelease());
    }
    return releases;
  }

  private PluginReferential add(Plugin plugin) {
    this.plugins.add(plugin);
    return this;
  }

}
