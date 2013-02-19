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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.sonar.updatecenter.common.exception.DependencyCycleException;
import org.sonar.updatecenter.common.exception.IncompatiblePluginVersionException;
import org.sonar.updatecenter.common.exception.PluginNotFoundException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

public final class PluginReferential {

  private Set<Plugin> plugins;

  private PluginReferential() {
    this.plugins = newHashSet();
  }

  public static PluginReferential create(List<Plugin> pluginList) {
    PluginReferential pluginReferential = new PluginReferential();
    for (Plugin plugin : pluginList) {
      pluginReferential.add(plugin);
    }
    return pluginReferential;
  }

  public static PluginReferential createEmpty() {
    return PluginReferential.create(Lists.<Plugin>newArrayList());
  }

  /**
   * @return the list of master plugins
   */
  public List<Plugin> getPlugins() {
    return newArrayList(Iterables.filter(plugins, new Predicate<Plugin>() {
      public boolean apply(Plugin input) {
        return input.isMaster();
      }
    }));
  }

  /**
   * @throws NoSuchElementException if plugin could not be found
   */
  public Plugin findPlugin(final String key) {
    return Iterables.find(plugins, new Predicate<Plugin>() {
      public boolean apply(Plugin input) {
        return input.getKey().equals(key);
      }
    });
  }

  public boolean doesContainPlugin(final String key) {
    return Iterables.any(plugins, new Predicate<Plugin>() {
      public boolean apply(Plugin input) {
        return input.getKey().equals(key);
      }
    });
  }

  public boolean doesContainRelease(final String key, Version version) {
    for (Plugin plugin : plugins) {
      if (plugin.getKey().equals(key)) {
        Release pluginRelease = plugin.getRelease(version);
        if (pluginRelease != null) {
          return true;
        }
      }
    }
    return false;
  }

  public Release findLatestRelease(String pluginKey) {
    Plugin plugin = findPlugin(pluginKey);
    if (plugin != null) {
      return plugin.getLastRelease();
    } else {
      return null;
    }
  }

  public List<String> findReleasesWithDependencies(String pluginKey) {
    List<String> removablePlugins = newArrayList();
    Plugin plugin = findPlugin(pluginKey);
    if (plugin != null) {
      Release pluginRelease = plugin.getLastRelease();
      removablePlugins.add(plugin.getKey());
      for (Plugin child : plugin.getChildren()) {
        removablePlugins.add(child.getKey());
      }
      for (Release incomingDependencies : pluginRelease.getIncomingDependencies()) {
        removablePlugins.addAll(findReleasesWithDependencies(incomingDependencies.getArtifact().getKey()));
      }
    }
    return removablePlugins;
  }

  public void setParent(Plugin plugin, String parentKey) {
    try {
      Plugin parent = findPlugin(parentKey);
      plugin.setParent(parent);
      checkPluginVersion(plugin, parent);
    } catch (NoSuchElementException e) {
      throw new PluginNotFoundException("The plugin '" + parentKey + "' required by the plugin '" + plugin.getKey() + "' is missing.", e);
    }
  }

  private void checkPluginVersion(Plugin plugin, Plugin parent) {
    for (Release release : plugin.getReleases()) {
      if (!parent.getReleases().contains(release)) {
        throw new IncompatiblePluginVersionException("The plugins '" + plugin.getKey() + "' and '" + parent.getKey() +
            "' must have exactly the same version as they belong to the same group.");
      }
    }
  }

  public void addOutgoingDependency(Release release, String requiredPluginReleaseKey, String requiredMinimumReleaseVersion) {
    try {
      Plugin requiredPlugin = findPlugin(requiredPluginReleaseKey);
      Release minimalRequiredRelease = requiredPlugin.getMinimalRelease(Version.create(requiredMinimumReleaseVersion));
      if (minimalRequiredRelease != null) {
        release.addOutgoingDependency(minimalRequiredRelease);
        checkDependencyCycle(release);
      } else {
        Release latest = requiredPlugin.getLastRelease();
        if (latest != null) {
          throw new IncompatiblePluginVersionException("The plugin '" + requiredPlugin.getKey() + "' is in version " + latest.getVersion().getName()
              + " whereas the plugin '" + release.getArtifact().getKey() + "' requires a least a version " + requiredMinimumReleaseVersion + ".");
        }
      }
    } catch (NoSuchElementException e) {
      throw new PluginNotFoundException("The plugin '" + requiredPluginReleaseKey + "' required by '" + release.getArtifact().getKey() + "' is missing.", e);
    }
  }

  private void checkDependencyCycle(Release release) {
    List<Release> releases = newArrayList();
    try {
      checkDependencyCycle(release, releases);
    } catch (DependencyCycleException e) {
      List<String> releaseKeys = newArrayList(Iterables.transform(releases, new Function<Release, String>() {
        public String apply(Release input) {
          return input.getArtifact().getKey();
        }
      }));
      throw new DependencyCycleException("There is a dependency cycle between plugins '" + Joiner.on("', '").join(releaseKeys) + "' that must be cut.", e);
    }
  }

  private void checkDependencyCycle(Release release, List<Release> releases) {
    for (Release outgoingDependency : release.getOutgoingDependencies()) {
      if (releases.contains(outgoingDependency)) {
        throw new DependencyCycleException();
      }
      releases.add(outgoingDependency);
      checkDependencyCycle(outgoingDependency, releases);
    }
  }

  List<Release> getReleasesForMasterPlugins() {
    List<Release> releases = newArrayList();
    for (Plugin plugin : getPlugins()) {
      releases.add(plugin.getLastRelease());
    }
    return releases;
  }

  List<Plugin> getAllPlugins() {
    return newArrayList(plugins);
  }

  private PluginReferential add(Plugin plugin) {
    this.plugins.add(plugin);
    return this;
  }

}
