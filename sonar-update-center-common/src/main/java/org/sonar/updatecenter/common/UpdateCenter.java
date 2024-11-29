/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.updatecenter.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.exception.IncompatiblePluginVersionException;
import org.sonar.updatecenter.common.exception.PluginNotFoundException;

public class UpdateCenter {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateCenter.class);
  private PluginReferential updateCenterPluginReferential;
  private PluginReferential installedPluginReferential;
  private Version installedSonarVersion;
  private Product installedSonarProduct;
  private Date date;
  private Sonar sonar;
  private List<Scanner> scanners;

  private UpdateCenter(PluginReferential updateCenterPluginReferential, List<Scanner> scanners, Sonar sonar, @Nullable Product product) {
    this.updateCenterPluginReferential = updateCenterPluginReferential;
    this.sonar = sonar;
    this.installedPluginReferential = PluginReferential.createEmpty();
    this.scanners = scanners;
    this.installedSonarProduct = product;
  }

  public static UpdateCenter create(PluginReferential updateCenterPluginReferential, List<Scanner> scanners, Sonar sonar,
    @Nullable Product product) {
    return new UpdateCenter(updateCenterPluginReferential, scanners, sonar, product);
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

  public UpdateCenter setDate(@Nullable Date date) {
    this.date = date != null ? new Date(date.getTime()) : null;
    return this;
  }

  public List<Scanner> getScanners() {
    return scanners;
  }

  public List<PluginUpdate> findAvailablePlugins() {
    List<PluginUpdate> availables = new ArrayList<>();
    for (Plugin plugin : updateCenterPluginReferential.getPlugins()) {
      if (isInstalled(plugin)) {
        continue;
      }
      Release release = plugin.getLastCompatible(installedSonarVersion, installedSonarProduct);
      if (release != null) {
        try {
          PluginUpdate pluginUpdate = PluginUpdate.createWithStatus(release, PluginUpdate.Status.COMPATIBLE);
          pluginUpdate.setDependencies(findInstallablePlugins(plugin.getKey(), release.getVersion()));
          availables.add(pluginUpdate);
        } catch (IncompatiblePluginVersionException e) {
          availables.add(PluginUpdate.createWithStatus(release, PluginUpdate.Status.DEPENDENCIES_REQUIRE_SONAR_UPGRADE));
        }
      } else {
        release = plugin.getLastCompatibleReleaseIfUpgrade(installedSonarVersion, installedSonarProduct);
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
    List<Plugin> availables = new ArrayList<>();
    for (Plugin plugin : updateCenterPluginReferential.getPlugins()) {
      Release release = plugin.getLastCompatible(installedSonarVersion, installedSonarProduct);
      if (release != null) {
        availables.add(plugin);
      }
    }
    return availables;
  }

  public List<PluginUpdate> findPluginUpdates() {
    List<PluginUpdate> updates = new ArrayList<>();
    for (Release installedRelease : getInstalledMasterReleases()) {
      try {
        Plugin plugin = findPlugin(installedRelease);
        for (Release nextRelease : plugin.getReleasesGreaterThan(installedRelease.getVersion(), null)) {
          updates.add(getPluginUpdate(plugin, nextRelease));
        }
      } catch (NoSuchElementException e) {
        // Nothing to do, this plugin is not in the update center, it has been installed manually.
      }
    }
    return updates;
  }

  private PluginUpdate getPluginUpdate(Plugin plugin, Release nextRelease) {
    PluginUpdate pluginUpdate = PluginUpdate.createForPluginRelease(nextRelease, installedSonarVersion, installedSonarProduct);
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
    Set<Release> installablePlugins = new HashSet<>();
    Set<Release> checkedPlugins = new HashSet<>();
    addInstallablePlugins(pluginKey, minimumVersion, installablePlugins, checkedPlugins);
    return new ArrayList<>(installablePlugins);
  }

  private void addInstallablePlugins(String pluginKey, Version minimumVersion, Set<Release> installablePlugins,
    Set<Release> checkedPlugins) {
    try {
      if (!contain(pluginKey, installablePlugins) && !contain(pluginKey, checkedPlugins)) {
        Plugin plugin = updateCenterPluginReferential.findPlugin(pluginKey);
        Release pluginRelease = plugin.getLastCompatible(installedSonarVersion, installedSonarProduct);
        if (pluginRelease != null) {
          if (pluginRelease.getVersion().compareTo(minimumVersion) < 0) {
            throw new IncompatiblePluginVersionException("Plugin " + pluginKey + " is needed to be installed at version greater or equal "
              + minimumVersion);
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
    return installablePlugins.stream().anyMatch(input -> input.getKey().equals(pluginKey));
  }

  private void addReleaseIfNotAlreadyInstalled(Release release, Set<Release> installablePlugins) {
    if (!isInstalled(release)) {
      installablePlugins.add(release);
    }
  }

  public List<SonarUpdate> findSonarUpdates() {
    List<SonarUpdate> updates = new ArrayList<>();
    SortedSet<Release> releases = sonar.getReleasesGreaterThan(installedSonarVersion, installedSonarProduct);
    if (installedSonarProduct == Product.SONARQUBE_COMMUNITY_BUILD) {
      findPaidReleaseToUpdateTo().ifPresent(releases::add);
    }
    for (Release release : releases) {
      updates.add(createSonarUpdate(release));
    }
    return updates;
  }

  private Optional<Release> findPaidReleaseToUpdateTo() {
    SortedSet<Release> sonarQubeServerReleases = sonar.getReleases(Product.SONARQUBE_SERVER);
    SortedSet<Release> sonarQubeCommunityBuildReleases = sonar.getReleases(Product.SONARQUBE_COMMUNITY_BUILD);

    if (!sonarQubeServerReleases.isEmpty()) {
      Release latest = sonarQubeServerReleases.last();
      if (latest.getVersion().isPatchVersion()) {
        return Optional.empty();
      }
      if (!sonarQubeCommunityBuildReleases.isEmpty()) {
        Optional<Release> currentCommunityBuild = sonarQubeCommunityBuildReleases.stream()
          .filter(release -> release.getVersion().equals(installedSonarVersion))
          .findFirst();
        if (!currentCommunityBuild.isPresent() || currentCommunityBuild.get().getDate().compareTo(latest.getDate()) <= 0) {
          return Optional.of(latest);
        } else {
          // the user is running a community build that is more recent than the latest sonarqube server release
          return Optional.empty();
        }
      }
      // the user is running community build so old that it disappeared from update center
      return Optional.of(latest);
    } else {
      // edge case - no sonarqube server releases at all, only needed for November 2024
      return Optional.empty();
    }
  }

  private SonarUpdate createSonarUpdate(Release sonarRelease) {
    SonarUpdate update = new SonarUpdate(sonarRelease);
    for (Release installedRelease : getInstalledMasterReleases()) {
      try {
        Plugin plugin = findPlugin(installedRelease);
        Release release = plugin.getRelease(installedRelease.getAdjustedVersion());
        if (release.supportSonarVersion(sonarRelease.getVersion(), installedSonarProduct)) {
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

  private void searchCompatiblePluginUpgrade(Release sonarRelease, SonarUpdate update, Release installedRelease, Plugin plugin) {
    boolean ok = false;
    Release compatibleRelease = null;
    for (Release greaterPluginRelease : plugin.getReleasesGreaterThan(installedRelease.getVersion(), null)) {
      if (greaterPluginRelease.supportSonarVersion(sonarRelease.getVersion(), installedSonarProduct)) {
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

  public Product getInstalledSonarProduct() {
    return installedSonarProduct;
  }

  public void setInstalledSonarProduct(Product installedSonarProduct) {
    this.installedSonarProduct = installedSonarProduct;
  }
}
