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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.exception.IncompatiblePluginVersionException;
import org.sonar.updatecenter.common.exception.PluginNotFoundException;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

public final class UpdateCenter {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateCenter.class);
  private PluginReferential updateCenterPluginReferential;
  private PluginReferential installedPluginReferential;
  private Version installedSonarVersion;
  private Date date;
  private Sonar sonar;

  private UpdateCenter(PluginReferential updateCenterPluginReferential, Sonar sonar) {
    this.updateCenterPluginReferential = updateCenterPluginReferential;
    this.sonar = sonar;
    this.installedPluginReferential = PluginReferential.createEmpty();
  }

  public static UpdateCenter create(PluginReferential updateCenterPluginReferential, Sonar sonar) {
    return new UpdateCenter(updateCenterPluginReferential, sonar);
  }

  public PluginReferential getUpdateCenterPluginReferential() {
    return updateCenterPluginReferential;
  }

  @VisibleForTesting
  PluginReferential getInstalledPluginReferential() {
    return installedPluginReferential;
  }

  public List<Plugin> getPlugins() {
    return updateCenterPluginReferential.getPlugins();
  }

  public UpdateCenter registerInstalledPlugins(PluginReferential installedPluginReferential) {
    this.installedPluginReferential = installedPluginReferential;
    return this;
  }

  public Sonar getSonar() {
    return sonar;
  }

  public UpdateCenter setInstalledSonarVersion(Version installedSonarVersion) {
    this.installedSonarVersion = installedSonarVersion;
    return this;
  }

  public Date getDate() {
    return date;
  }

  public UpdateCenter setDate(Date date) {
    this.date = date;
    return this;
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
      try {
        Plugin plugin = findPlugin(installedRelease);
        // TODO check all dependencies are available, if not, set a a status special
        for (Release nextRelease : plugin.getReleasesGreaterThan(installedRelease.getVersion())) {
          updates.add(PluginUpdate.createForPluginRelease(nextRelease, getAdjustedSonarVersion()));
        }
      } catch (NoSuchElementException e) {
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
    if (updateCenterPluginReferential.doesContainPlugin(pluginKey) && !contain(pluginKey, installablePlugins)) {
      Plugin plugin = updateCenterPluginReferential.findPlugin(pluginKey);
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
  }

  private boolean contain(final String pluginKey, Set<Release> installablePlugins) {
    return Iterables.any(installablePlugins, new Predicate<Release>() {
      public boolean apply(Release input) {
        return input.getArtifact().getKey().equals(pluginKey);
      }
    });
  }

  private void addReleaseIfNotAlreadyInstalled(Release release, Set<Release> installablePlugins) {
    if (!isInstalled(release)) {
      installablePlugins.add(release);
    }
  }

  public List<SonarUpdate> findSonarUpdates() {
    List<SonarUpdate> updates = Lists.newArrayList();
    SortedSet<Release> releases = sonar.getReleasesGreaterThan(installedSonarVersion);
    for (Release release : releases) {
      updates.add(createSonarUpdate(release));
    }
    return updates;
  }

  SonarUpdate createSonarUpdate(Release sonarRelease) {
    SonarUpdate update = new SonarUpdate(sonarRelease);
    for (Release installedRelease : installedPluginReferential.getReleasesForMasterPlugins()) {
      try {
        Plugin plugin = findPlugin(installedRelease);
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
      } catch (NoSuchElementException e) {
        LOG.info("The plugin '" + installedRelease.getArtifact().getKey() + "' version : " + installedRelease.getVersion().getName() + " has not been found on the update center.");
      }
    }
    return update;
  }

  public Release findLatestCompatibleRelease(String pluginKey) {
    if (updateCenterPluginReferential.doesContainPlugin(pluginKey)) {
      Plugin plugin = updateCenterPluginReferential.findPlugin(pluginKey);
      return plugin.getLastCompatibleRelease(getAdjustedSonarVersion());
    } else {
      return null;
    }
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
    return installedPluginReferential.doesContainRelease(releaseToFind.getArtifact().getKey(), releaseToFind.getVersion());
  }

  private boolean isInstalled(final Plugin plugin) {
    return installedPluginReferential.doesContainPlugin(plugin.getKey());
  }

  private Plugin findPlugin(Release release) {
    String key = release.getArtifact().getKey();
    return updateCenterPluginReferential.findPlugin(key);
  }

}
