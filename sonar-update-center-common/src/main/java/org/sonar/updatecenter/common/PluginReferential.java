/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
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
import static com.google.common.collect.Sets.newTreeSet;

public class PluginReferential {

  private Set<Plugin> plugins;

  private PluginReferential() {
    this.plugins = newTreeSet();
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
   * @return the list of plugins where last releases is master releases
   */
  public List<Plugin> getLastMasterReleasePlugins() {
    return newArrayList(Iterables.filter(plugins, new Predicate<Plugin>() {
      public boolean apply(Plugin input) {
        Release lastRelease = input.getLastRelease();
        return lastRelease != null;
      }
    }));
  }

  public List<Plugin> getPlugins() {
    return newArrayList(plugins);
  }

  /**
   * @throws NoSuchElementException if plugin could not be found
   */
  public Plugin findPlugin(final String key) {
    try {
      return Iterables.find(plugins, new Predicate<Plugin>() {
        public boolean apply(Plugin input) {
          return input.getKey().equals(key);
        }
      });
    } catch (NoSuchElementException e) {
      throw new NoSuchElementException("Unable to find plugin with key " + key);
    }
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
      if (plugin.getKey().equals(key) && plugin.doesContainVersion(version)) {
        return true;
      }
    }
    return false;
  }

  public List<String> findLastReleasesWithDependencies(String pluginKey) {
    List<String> removablePlugins = newArrayList();
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

  List<Release> getLastMasterReleases() {
    List<Release> releases = newArrayList();
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
