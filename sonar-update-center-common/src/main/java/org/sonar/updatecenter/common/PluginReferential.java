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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;
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
      if (plugin.isMaster()) {
        pluginReferential.add(plugin);
      } else {
        pluginReferential.addChild(plugin, pluginList);
      }
    }
    return pluginReferential;
  }

  public static PluginReferential createEmpty(){
   return PluginReferential.create(Lists.<Plugin>newArrayList());
  }

  public List<Plugin> getPlugins() {
    return newArrayList(plugins);
  }

  public PluginReferential add(Plugin plugin) {
    this.plugins.add(plugin);
    return this;
  }

  @Nullable
  public Plugin findPlugin(final String key) {
    return Iterables.find(plugins, new Predicate<Plugin>() {
      public boolean apply(Plugin input) {
        return input.getKey().equals(key);
      }
    }, null);
  }

  @Nullable
  public Release findRelease(final String key, Version version) {
    for (Plugin plugin : plugins) {
      if (plugin.getKey().equals(key)){
        Release pluginRelease = plugin.getRelease(version);
        if (pluginRelease != null) {
          return pluginRelease;
        }
      }
      for (Plugin child : plugin.getChildren()) {
        if (child.getKey().equals(key)){
          return child.getRelease(version);
        }
      }
    }
    return null;
  }

  public Release findRelease(String pluginKey, String version) {
    Plugin plugin = findPlugin(pluginKey);
    return plugin.getRelease(Version.create(version));
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

  List<Release> getReleasesForMasterPlugins(){
    List<Release> releases = newArrayList();
    for (Plugin plugin : plugins) {
      releases.add(plugin.getLastRelease());
    }
    return releases;
  }

  private void addChild(final Plugin plugin, List<Plugin> pluginList) {
    Plugin pluginParent = Iterables.find(pluginList, new Predicate<Plugin>() {
      public boolean apply(Plugin input) {
        return input.getKey().equals(plugin.getParent().getKey());
      }
    });
    pluginParent.addChild(plugin);
  }

}
