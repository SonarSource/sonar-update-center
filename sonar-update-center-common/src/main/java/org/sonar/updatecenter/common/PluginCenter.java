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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.exception.IncompatiblePluginVersionException;
import org.sonar.updatecenter.common.exception.PluginNotFoundException;

import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

public final class PluginCenter {

  private static final Logger LOG = LoggerFactory.getLogger(PluginCenter.class);
  private PluginReferential updateCenterPluginReferential;
  private PluginReferential installedPluginReferential;
  private Version installedSonarVersion;
  private Date date;

  private PluginCenter(PluginReferential updateCenterPluginReferential, PluginReferential installedPluginReferential, Version installedSonarVersion) {
    this.updateCenterPluginReferential = updateCenterPluginReferential;
    this.installedSonarVersion = installedSonarVersion;
    this.installedPluginReferential = installedPluginReferential;
  }

  public static PluginCenter create(PluginReferential updateCenterPluginReferential, PluginReferential installedPluginReferential, Version installedSonarVersion) {
    return new PluginCenter(updateCenterPluginReferential, installedPluginReferential, installedSonarVersion);
  }

  public static PluginCenter createForUpdateCenterPlugins(PluginReferential updateCenterPluginReferential, Version installedSonarVersion) {
    return PluginCenter.create(updateCenterPluginReferential, PluginReferential.createEmptyReferential(), installedSonarVersion);
  }

  public static PluginCenter createForInstalledPlugins(PluginReferential installedPluginReferential, Version installedSonarVersion) {
    return PluginCenter.create(PluginReferential.createEmptyReferential(), installedPluginReferential, installedSonarVersion);
  }

  public PluginReferential getUpdateCenterPluginReferential() {
    return updateCenterPluginReferential;
  }

  public PluginReferential getInstalledPluginReferential() {
    return installedPluginReferential;
  }

  public List<PluginUpdate> findAvailablePlugins() {
    Version adjustedSonarVersion = getAdjustedSonarVersion();
    List<PluginUpdate> availables = newArrayList();
    // TODO check all dependencies are available, if not, set a a status special
    for (Plugin plugin : updateCenterPluginReferential.getPlugins()) {
      if (!isInstalled(plugin)) {
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
    List<PluginUpdate> updates = newArrayList();
    for (Release installedRelease : installedPluginReferential.getReleasesForMasterPlugins()) {
      Plugin plugin = findPlugin(installedRelease);
      if (plugin != null) {
        // TODO check all dependencies are available, if not, set a a status special
        for (Release nextRelease : plugin.getReleasesGreaterThan(installedRelease.getVersion())) {
          updates.add(PluginUpdate.createForPluginRelease(nextRelease, getAdjustedSonarVersion()));
        }
      } else {
        LOG.info("The plugin '" + installedRelease.getArtifact().getKey() + "' version : " + installedRelease.getVersion().getName() + " has not been found on the update center.");
      }
    }
    return updates;
  }

  /**
   * Return all releases to download (including outgoing dependencies) to install / update a plugin
   */
  public List<Release> findInstallablePlugins(String pluginKey, Version minimumVersion) {
    Set<Release> installablePlugins = newHashSet();
    findInstallablePlugins(pluginKey, minimumVersion, installablePlugins);
    return newArrayList(installablePlugins);
  }

  /**
   * Return all releases to download (including outgoing dependencies) to install / update a plugin
   */
  public void findInstallablePlugins(String pluginKey, Version minimumVersion, Set<Release> installablePlugins) {
    Plugin plugin = updateCenterPluginReferential.findPlugin(pluginKey);
    if (plugin != null && !contain(pluginKey, installablePlugins)) {
      Release pluginRelease = plugin.getLastCompatibleRelease(getAdjustedSonarVersion());
      if (pluginRelease.getVersion().compareTo(minimumVersion) < 0) {
        throw new IncompatiblePluginVersionException("Plugin " + pluginKey + " is needed to be installed at version greater or equal " + minimumVersion);
      }
      addReleaseIfNotAlreadyInstalled(pluginRelease, installablePlugins);
      for (Plugin child : plugin.getChildren()) {
        addReleaseIfNotAlreadyInstalled(child.getRelease(pluginRelease.getVersion()), installablePlugins);
      }
      for (Release outgoingDependency : pluginRelease.getOutgoingDependencies()) {
        if (!installablePlugins.contains(outgoingDependency)) {
          findInstallablePlugins(outgoingDependency.getArtifact().getKey(), outgoingDependency.getVersion(), installablePlugins);
          installablePlugins.addAll(installablePlugins);
        }
      }
      for (Release incomingDependency : pluginRelease.getIncomingDependencies()) {
        if (!installablePlugins.contains(incomingDependency) && !contain(incomingDependency.getArtifact().getKey(), installablePlugins)) {
          findInstallablePlugins(incomingDependency.getArtifact().getKey(), incomingDependency.getVersion(), installablePlugins);
          installablePlugins.addAll(installablePlugins);
        }
      }
    } else {
      throw new PluginNotFoundException("Needed plugin '" + pluginKey + "' version " + minimumVersion + " not found.");
    }
//    return installablePlugins;
  }

  private boolean contain(final String pluginKey, Set<Release> installablePlugins) {
    return Iterables.any(installablePlugins, new Predicate<Release>() {
      public boolean apply(@Nullable Release input) {
        return input.getArtifact().getKey().equals(pluginKey);
      }
    });
  }

  private void addReleaseIfNotAlreadyInstalled(Release release, Set<Release> installablePlugins) {
    if (!isInstalled(release)) {
      installablePlugins.add(release);
    }
  }

  /**
   * Return releases keys to remove (including incoming dependencies) to remove a plugin
   */
  public List<String> findRemovableReleases(String pluginKey) {
    List<String> removablePlugins = newArrayList();
    Plugin plugin = installedPluginReferential.findPlugin(pluginKey);
    if (plugin != null) {
      Release pluginRelease = plugin.getLastRelease();
      removablePlugins.add(plugin.getKey());
      for (Plugin child : plugin.getChildren()) {
        removablePlugins.add(child.getKey());
      }
      for (Release incomingDependencies : pluginRelease.getIncomingDependencies()) {
        removablePlugins.addAll(findRemovableReleases(incomingDependencies.getArtifact().getKey()));
      }
    }
    return removablePlugins;
  }

  public List<SonarUpdate> findSonarUpdates() {
    List<SonarUpdate> updates = Lists.newArrayList();
    SortedSet<Release> releases = updateCenterPluginReferential.getSonar().getReleasesGreaterThan(installedSonarVersion);
    for (Release release : releases) {
      updates.add(createSonarUpdate(release));
    }
    return updates;
  }

  SonarUpdate createSonarUpdate(Release sonarRelease) {
    SonarUpdate update = new SonarUpdate(sonarRelease);
    for (Release installedRelease : installedPluginReferential.getReleasesForMasterPlugins()) {
      Plugin plugin = findPlugin(installedRelease);
      if (plugin != null) {
        if (installedRelease.supportSonarVersion(sonarRelease.getVersion())) {
          update.addCompatiblePlugin(plugin);

        } else {
          // search for a compatible plugin upgrade
          boolean ok = false;
          Release compatibleRelease = null;
          for (Release greaterPluginRelease : plugin.getReleasesGreaterThan(installedRelease.getVersion())) {
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
      } else {
        LOG.info("The plugin '" + installedRelease.getArtifact().getKey() + "' version : " + installedRelease.getVersion().getName() + " has not been found on the update center.");
      }
    }
    return update;
  }

  public Release findLatestCompatibleRelease(String pluginKey) {
    Plugin plugin = updateCenterPluginReferential.findPlugin(pluginKey);
    if (plugin != null) {
      return plugin.getLastCompatibleRelease(getAdjustedSonarVersion());
    } else {
      return null;
    }
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

  private boolean isInstalled(final Release releaseToFind) {
    return installedPluginReferential.findRelease(releaseToFind.getArtifact().getKey(), releaseToFind.getVersion()) != null;
  }

  private boolean isInstalled(final Plugin plugin) {
    return installedPluginReferential.findPlugin(plugin.getKey()) != null;
  }

  @Nullable
  private Plugin findPlugin(Release release) {
    return updateCenterPluginReferential.findPlugin(release.getArtifact().getKey());
  }

}
