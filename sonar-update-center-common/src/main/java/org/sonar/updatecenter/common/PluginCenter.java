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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static com.google.common.collect.Lists.newArrayList;

public final class PluginCenter {

  private PluginReferential pluginReferential;
  private Version installedSonarVersion;
  private Map<Plugin, Version> installedPlugins = Maps.newHashMap();
  private Date date;

  public PluginCenter(PluginReferential pluginReferential, Version installedSonarVersion) {
    this.pluginReferential = pluginReferential;
    this.installedSonarVersion = installedSonarVersion;
  }

  public PluginCenter registerInstalledPlugin(String pluginKey, Version pluginVersion) {
    Plugin plugin = pluginReferential.getPlugin(pluginKey);
    if (plugin != null) {
      installedPlugins.put(plugin, pluginVersion);
    }
    return this;
  }

  public PluginReferential getPluginReferential() {
    return pluginReferential;
  }

  public List<Plugin> getInstalledPlugins() {
    return newArrayList(installedPlugins.keySet());
  }

  public List<PluginUpdate> findAvailablePlugins() {
    Version adjustedSonarVersion = getAdjustedSonarVersion();
    List<PluginUpdate> availables = newArrayList();
    for (Plugin plugin : pluginReferential.getPlugins()) {

      // TODO check each plugin is not in already downloaded mode
      if (!installedPlugins.containsKey(plugin)) {
        Release release = plugin.getLastCompatibleRelease(adjustedSonarVersion);
        if (release != null) {
          availables.add(PluginUpdate.createWithStatus(release, PluginUpdate.Status.COMPATIBLE));
        } else {
          release = plugin.getLastCompatibleReleaseIfUpgrade(adjustedSonarVersion);
          if (release != null) {
            availables.add(PluginUpdate.createWithStatus(release, PluginUpdate.Status.REQUIRE_SONAR_UPGRADE));
          }
        }
      }
    }
    return availables;
  }

  public List<PluginUpdate> findPluginUpdates() {
    Version adjustedSonarVersion = getAdjustedSonarVersion();

    List<PluginUpdate> updates = newArrayList();
    for (Map.Entry<Plugin, Version> entry : installedPlugins.entrySet()) {
      Plugin plugin = entry.getKey();
      Version pluginVersion = entry.getValue();
      for (Release release : plugin.getReleasesGreaterThan(pluginVersion)) {
        updates.add(PluginUpdate.createForPluginRelease(release, adjustedSonarVersion));
      }
    }
    return updates;
  }

  /**
   * Return plugin files to download (including dependencies)
   */
  public List<Release> findInstallablePlugins(String pluginKey, Version version) {
    List<Release> installablePlugins = newArrayList();

    Plugin plugin = pluginReferential.getPlugin(pluginKey);
    installablePlugins.add(plugin.getRelease(version));
    for (Plugin child : plugin.getChildren()) {
      installablePlugins.add(child.getRelease(version));
    }

    return installablePlugins;
  }

  public List<SonarUpdate> findSonarUpdates() {
    List<SonarUpdate> updates = Lists.newArrayList();
    SortedSet<Release> releases = pluginReferential.getSonar().getReleasesGreaterThan(installedSonarVersion);
    for (Release release : releases) {
      updates.add(createSonarUpdate(release));
    }
    return updates;
  }

  SonarUpdate createSonarUpdate(Release sonarRelease) {
    SonarUpdate update = new SonarUpdate(sonarRelease);
    // TODO use groups instead of plugins
    for (Map.Entry<Plugin, Version> entry : installedPlugins.entrySet()) {
      Plugin plugin = entry.getKey();
      Version pluginVersion = entry.getValue();
      Release pluginRelease = plugin.getRelease(pluginVersion);

      if (pluginRelease != null && pluginRelease.supportSonarVersion(sonarRelease.getVersion())) {
        update.addCompatiblePlugin(plugin);

      } else {
        // search for a compatible plugin upgrade
        boolean ok = false;
        Release compatibleRelease = null;
        for (Release greaterPluginRelease : plugin.getReleasesGreaterThan(pluginVersion)) {
          if (greaterPluginRelease.supportSonarVersion(sonarRelease.getVersion())) {
            compatibleRelease = greaterPluginRelease;
            ok = true;
          }
        }
        if (ok) {
          update.addPluginToUpgrade(compatibleRelease);
        } else {
          update.addIncompatiblePlugin(plugin);
        }
      }
    }
    return update;
  }

  public Date getDate() {
    return date;
  }

  public PluginCenter setDate(Date d) {
    this.date = d;
    return this;
  }

  /**
   * Update center declares RELEASE versions of Sonar, for instance 3.2 but not 3.2-SNAPSHOT.
   * We assume that SNAPSHOT, milestones and release candidates of Sonar support the
   * same plugins than related RELEASE.
   */
  private Version getAdjustedSonarVersion() {
    return Version.createRelease(installedSonarVersion.toString());
  }

}
