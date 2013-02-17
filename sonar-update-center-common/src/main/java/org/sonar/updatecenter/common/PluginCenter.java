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
import org.sonar.updatecenter.common.exception.IncompatiblePluginVersionException;
import org.sonar.updatecenter.common.exception.PluginNotFoundException;

import java.util.Date;
import java.util.List;
import java.util.SortedSet;

import static com.google.common.collect.Lists.newArrayList;

public final class PluginCenter {

  private PluginReferential pluginReferential;
  private Version installedSonarVersion;
  private List<Release> installedReleases;
  private Date date;

  public PluginCenter(PluginReferential pluginReferential, Version installedSonarVersion) {
    this.pluginReferential = pluginReferential;
    this.installedSonarVersion = installedSonarVersion;
    installedReleases = newArrayList();
  }

  public PluginCenter registerInstalledPlugin(String key, Version version) {
    Release release = pluginReferential.findRelease(key, version);
    if (release != null) {
      installedReleases.add(release);
    } else {
      throw new PluginNotFoundException("Release not found : "+ key + " with version : "+ version.getName());
    }
    return this;
  }

  public PluginReferential getPluginReferential() {
    return pluginReferential;
  }

  public List<Release> getInstalledReleases() {
    return installedReleases;
  }

  public List<PluginUpdate> findAvailablePlugins() {
    Version adjustedSonarVersion = getAdjustedSonarVersion();
    List<PluginUpdate> availables = newArrayList();
    // TODO check all dependencies are available, if not, set a a status special
    for (Plugin plugin : pluginReferential.getPlugins()) {
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
    Version adjustedSonarVersion = getAdjustedSonarVersion();

    List<PluginUpdate> updates = newArrayList();
    for (Release release : installedReleases) {
      Plugin plugin = findPlugin(release);
      // TODO check all dependencies are available, if not, set a a status special
      for (Release nextRelease : plugin.getReleasesGreaterThan(release.getVersion())) {
        updates.add(PluginUpdate.createForPluginRelease(nextRelease, adjustedSonarVersion));
      }
    }
    return updates;
  }

  /**
   * Return all release to download (including dependencies) to install / update a plugin
   */
  public List<Release> findInstallablePlugins(String pluginKey, Version minimumVersion) {
    List<Release> installablePlugins = newArrayList();
    Plugin plugin = pluginReferential.findPlugin(pluginKey);
    if (plugin != null) {
      Release pluginRelease = plugin.getLastCompatibleRelease(installedSonarVersion);
      if (pluginRelease.getVersion().compareTo(minimumVersion) < 0) {
        throw new IncompatiblePluginVersionException("Plugin "+ pluginKey + " is needed to be installed at version greater or equal "+ minimumVersion);
      }
      addReleaseIfNotAlreadyInstalled(pluginRelease, installablePlugins);
      for (Plugin child : plugin.getChildren()) {
        addReleaseIfNotAlreadyInstalled(child.getRelease(pluginRelease.getVersion()), installablePlugins);
      }
      for (Release requiredRelease : pluginRelease.getRequiredReleases()) {
        installablePlugins.addAll(findInstallablePlugins(requiredRelease.getArtifact().getKey(), requiredRelease.getVersion()));
      }
    } else {
     throw new PluginNotFoundException("Needed plugin '"+ pluginKey + "' version "+ minimumVersion + " not found.");
    }
    return installablePlugins;
  }

  private void addReleaseIfNotAlreadyInstalled(Release release, List<Release> installablePlugins) {
    if (!isInstalled(release)) {
      installablePlugins.add(release);
    }
  }

  /**
   * Return plugin keys to remove (including dependencies) to remove a plugin
   */
  public List<String> findRemovablePlugins(String pluginKey) {
    List<String> removablePlugins = newArrayList();
    Plugin plugin = pluginReferential.findPlugin(pluginKey);
    if (plugin != null) {
      Release pluginRelease = plugin.getLastRelease();
      removablePlugins.add(plugin.getKey());
      for (Plugin child : plugin.getChildren()) {
          removablePlugins.add(child.getKey());
      }
      for (Release requiredRelease : pluginRelease.getRequiredReleases()) {
        removablePlugins.addAll(findRemovablePlugins(requiredRelease.getArtifact().getKey()));
      }
    }
    return removablePlugins;
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
    for (Release release : installedReleases) {
      Plugin plugin = findPlugin(release);

      if (release.supportSonarVersion(sonarRelease.getVersion())) {
        update.addCompatiblePlugin(plugin);

      } else {
        // search for a compatible plugin upgrade
        boolean ok = false;
        Release compatibleRelease = null;
        for (Release greaterPluginRelease : plugin.getReleasesGreaterThan(release.getVersion())) {
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

  public Release findLatestCompatible(String pluginKey) {
    Plugin plugin = pluginReferential.findPlugin(pluginKey);
    if (plugin != null) {
      return plugin.getLastCompatibleRelease(installedSonarVersion);
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
    return Iterables.any(installedReleases, new Predicate<Release>() {
      public boolean apply(Release release) {
        return releaseToFind.equals(release);
      }
    });
  }

  private boolean isInstalled(final Plugin plugin) {
    return Iterables.any(installedReleases, new Predicate<Release>() {
      public boolean apply(Release release) {
        return plugin.getKey().equals(release.getArtifact().getKey());
      }
    });
  }

  private Plugin findPlugin(Release release) {
    return pluginReferential.findPlugin(release.getArtifact().getKey());
  }

}
