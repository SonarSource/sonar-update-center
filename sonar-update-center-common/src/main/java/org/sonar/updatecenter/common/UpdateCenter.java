/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

public class UpdateCenter {

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
    return date != null ? new Date(date.getTime()) : null;
  }

  public UpdateCenter setDate(Date date) {
    this.date = date != null ? new Date(date.getTime()) : null;
    return this;
  }

  public List<PluginUpdate> findAvailablePlugins() {
    List<PluginUpdate> availables = newArrayList();
    for (Plugin plugin : updateCenterPluginReferential.getPlugins()) {
      if (isInstalled(plugin)) {
        continue;
      }
      Release release = plugin.getLastCompatibleRelease(installedSonarVersion);
      if (release != null) {
        try {
          PluginUpdate pluginUpdate = PluginUpdate.createWithStatus(release, PluginUpdate.Status.COMPATIBLE);
          pluginUpdate.setDependencies(findInstallablePlugins(plugin.getKey(), release.getVersion()));
          availables.add(pluginUpdate);
        } catch (IncompatiblePluginVersionException e) {
          availables.add(PluginUpdate.createWithStatus(release, PluginUpdate.Status.DEPENDENCIES_REQUIRE_SONAR_UPGRADE));
        }

      } else {
        release = plugin.getLastCompatibleReleaseIfUpgrade(installedSonarVersion);
        if (release != null) {
          availables.add(PluginUpdate.createWithStatus(release, PluginUpdate.Status.REQUIRE_SONAR_UPGRADE));
        }
      }
    }
    return availables;
  }

  /**
   * Return all plugins with at least one version compatible with SQ. For ecosystems only parent plugin is returned.
   */
  public List<Plugin> findAllCompatiblePlugins() {
    List<Plugin> availables = newArrayList();
    for (Plugin plugin : updateCenterPluginReferential.getPlugins()) {
      Release release = plugin.getLastCompatible(installedSonarVersion);
      if (release != null) {
        availables.add(plugin);
      }
    }
    return availables;
  }

  public List<PluginUpdate> findPluginUpdates() {
    List<PluginUpdate> updates = newArrayList();
    for (Release installedRelease : getInstalledMasterReleases()) {
      try {
        Plugin plugin = findPlugin(installedRelease);
        for (Release nextRelease : plugin.getReleasesGreaterThan(installedRelease.getVersion())) {
          updates.add(getPluginUpdate(plugin, nextRelease));
        }
      } catch (NoSuchElementException e) {
        // Nothing to do, this plugin is not in the update center, it has been installed manually.
      }
    }
    return updates;
  }

  private PluginUpdate getPluginUpdate(Plugin plugin, Release nextRelease) {
    PluginUpdate pluginUpdate = PluginUpdate.createForPluginRelease(nextRelease, installedSonarVersion);
    try {
      if (pluginUpdate.isCompatible()) {
        pluginUpdate.setDependencies(findInstallablePlugins(plugin.getKey(), nextRelease.getVersion()));
      }
    } catch (IncompatiblePluginVersionException e) {
      pluginUpdate.setStatus(PluginUpdate.Status.DEPENDENCIES_REQUIRE_SONAR_UPGRADE);
    }
    return pluginUpdate;
  }

  /**
   * Return all releases to download (including outgoing dependencies and installed incoming dependencies) to install / update a plugin
   */
  public List<Release> findInstallablePlugins(String pluginKey, Version minimumVersion) {
    Set<Release> installablePlugins = newHashSet();
    Set<Release> checkedPlugins = newHashSet();
    addInstallablePlugins(pluginKey, minimumVersion, installablePlugins, checkedPlugins);
    return newArrayList(installablePlugins);
  }

  private void addInstallablePlugins(String pluginKey, Version minimumVersion, Set<Release> installablePlugins, Set<Release> checkedPlugins) {
    try {
      if (!contain(pluginKey, installablePlugins) && !contain(pluginKey, checkedPlugins)) {
        Plugin plugin = updateCenterPluginReferential.findPlugin(pluginKey);
        Release pluginRelease = plugin.getLastCompatibleRelease(installedSonarVersion);
        if (pluginRelease != null) {
          if (pluginRelease.getVersion().compareTo(minimumVersion) < 0) {
            throw new IncompatiblePluginVersionException("Plugin " + pluginKey + " is needed to be installed at version greater or equal " + minimumVersion);
          }
          addInstallableRelease(pluginRelease, installablePlugins, checkedPlugins);
        }
      }
    } catch (NoSuchElementException e) {
      throw new PluginNotFoundException("Needed plugin '" + pluginKey + "' version " + minimumVersion + " not found.");
    }
  }

  private void addInstallableRelease(Release pluginRelease, Set<Release> installablePlugins, Set<Release> checkedPlugins) {
    addReleaseIfNotAlreadyInstalled(pluginRelease, installablePlugins);
    checkedPlugins.add(pluginRelease);
    for (Release outgoingDependency : pluginRelease.getOutgoingDependencies()) {
      addInstallablePlugins(outgoingDependency.getArtifact().getKey(), outgoingDependency.getVersion(), installablePlugins, checkedPlugins);
    }
    for (Release incomingDependency : pluginRelease.getIncomingDependencies()) {
      String pluginKey = incomingDependency.getArtifact().getKey();
      if (isInstalled(pluginKey)) {
        addInstallablePlugins(pluginKey, incomingDependency.getVersion(), installablePlugins, checkedPlugins);
      }
    }
  }

  private boolean contain(final String pluginKey, Set<Release> installablePlugins) {
    return Iterables.any(installablePlugins, new Predicate<Release>() {
      @Override
      public boolean apply(Release input) {
        return input.getKey().equals(pluginKey);
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

  private SonarUpdate createSonarUpdate(Release sonarRelease) {
    SonarUpdate update = new SonarUpdate(sonarRelease);
    for (Release installedRelease : getInstalledMasterReleases()) {
      try {
        Plugin plugin = findPlugin(installedRelease);
        Release release = plugin.getRelease(installedRelease.getAdjustedVersion());
        if (release.supportSonarVersion(sonarRelease.getVersion())) {
          update.addCompatiblePlugin(plugin);
        } else {
          searchCompatiblePluginUpgrade(sonarRelease, update, installedRelease, plugin);
        }
      } catch (NoSuchElementException e) {
        LOG.info("The plugin '" + installedRelease.getArtifact().getKey() +
          "' version : " + installedRelease.getVersion().getName() + " has not been found on the update center.");
      }
    }
    return update;
  }

  private static void searchCompatiblePluginUpgrade(Release sonarRelease, SonarUpdate update, Release installedRelease, Plugin plugin) {
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

  @VisibleForTesting
  PluginReferential getInstalledPluginReferential() {
    return installedPluginReferential;
  }

  private boolean isInstalled(final Release releaseToFind) {
    return installedPluginReferential.doesContainRelease(releaseToFind.getArtifact().getKey(), releaseToFind.getVersion());
  }

  private boolean isInstalled(final Plugin plugin) {
    return isInstalled(plugin.getKey());
  }

  private boolean isInstalled(final String pluginKey) {
    return installedPluginReferential.doesContainPlugin(pluginKey);
  }

  private Plugin findPlugin(Release release) {
    String key = release.getArtifact().getKey();
    return updateCenterPluginReferential.findPlugin(key);
  }

  private List<Release> getInstalledMasterReleases() {
    return installedPluginReferential.getLastMasterReleases();
  }

}
