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

public final class UpdateCenterMatrix {

  private UpdateCenter center;
  private Version installedSonarVersion;
  private Map<Plugin, Version> installedPlugins = Maps.newHashMap();
  private Map<PluginParent, Version> installedGroups = Maps.newHashMap();
  private List<String> pendingPluginFilenames = newArrayList();
  private Date date;

  public UpdateCenterMatrix(UpdateCenter center, Version installedSonarVersion) {
    this.center = center;
    this.installedSonarVersion = installedSonarVersion;
  }

  public UpdateCenterMatrix registerInstalledPlugin(String pluginKey, Version pluginVersion) {
    Plugin plugin = center.getPlugin(pluginKey);
    if (plugin != null) {
      installedPlugins.put(plugin, pluginVersion);
      // TODO check partial group installation
      PluginParent pluginParent = center.getParent(pluginKey);
      if (pluginParent != null) {
        installedGroups.put(center.getParent(pluginKey), pluginVersion);
      }
    }
    return this;
  }

  public UpdateCenterMatrix registerPendingPluginsByFilename(String filename) {
    pendingPluginFilenames.add(filename);
    return this;
  }

  public UpdateCenter getCenter() {
    return center;
  }

  public List<PluginParent> getInstalledGroups(){
    return newArrayList(installedGroups.keySet());
  }

  public List<PluginParentUpdate> findAvailablePlugins() {
    Version adjustedSonarVersion = getAdjustedSonarVersion();
    List<PluginParentUpdate> availables = newArrayList();
    for (PluginParent pluginParent : center.getPluginParents()){
      Plugin plugin = pluginParent.getMasterPlugin();
      // TODO check each plugin is not in already downloaded mode
      if (!installedPlugins.containsKey(plugin) && !isAlreadyDownloaded(plugin)) {
        Release release = plugin.getLastCompatibleRelease(adjustedSonarVersion);
        if (release != null) {
          availables.add(PluginParentUpdate.createWithStatus(pluginParent, release, PluginParentUpdate.Status.COMPATIBLE));
        } else {
          release = plugin.getLastCompatibleReleaseIfUpgrade(adjustedSonarVersion);
          if (release != null) {
            availables.add(PluginParentUpdate.createWithStatus(pluginParent, release, PluginParentUpdate.Status.REQUIRE_SONAR_UPGRADE));
          }
        }
      }
    }
    return availables;
  }

  public List<PluginParentUpdate> findPluginUpdates() {
    Version adjustedSonarVersion = getAdjustedSonarVersion();

    List<PluginParentUpdate> updates = newArrayList();
    for (Map.Entry<PluginParent, Version> entry : installedGroups.entrySet()) {
      PluginParent pluginParent = entry.getKey();
      Plugin plugin = pluginParent.getMasterPlugin();
      if (!isAlreadyDownloaded(plugin)) {
        Version pluginVersion = entry.getValue();
        for (Release release : plugin.getReleasesGreaterThan(pluginVersion)) {
          updates.add(PluginParentUpdate.createForPluginRelease(pluginParent, release, adjustedSonarVersion));
        }
      }
    }
    return updates;
  }

  public List<SonarUpdate> findSonarUpdates() {
    List<SonarUpdate> updates = Lists.newArrayList();
    SortedSet<Release> releases = center.getSonar().getReleasesGreaterThan(installedSonarVersion);
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

  public UpdateCenterMatrix setDate(Date d) {
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

  private boolean isAlreadyDownloaded(Artifact artifact) {
    for (Release r : artifact.getReleases()) {
      if (pendingPluginFilenames.contains(r.getFilename())) {
        // already downloaded
        return true;
      }
    }
    return false;
  }
}
