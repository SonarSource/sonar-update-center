/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import java.util.ArrayList;
import java.util.List;

public final class SonarUpdate implements Comparable<SonarUpdate> {

  private Release release;
  private List<Plugin> compatiblePlugins = new ArrayList<>();
  private List<Plugin> incompatiblePlugins = new ArrayList<>();
  private List<Release> pluginsToUpgrade = new ArrayList<>();

  public SonarUpdate(Release release) {
    this.release = release;
  }

  public Release getRelease() {
    return release;
  }

  public List<Plugin> getCompatiblePlugins() {
    return compatiblePlugins;
  }

  public List<Plugin> getIncompatiblePlugins() {
    return incompatiblePlugins;
  }

  public List<Release> getPluginsToUpgrade() {
    return pluginsToUpgrade;
  }

  public boolean hasWarnings() {
    return isIncompatible() || requiresPluginUpgrades();
  }

  public boolean requiresPluginUpgrades() {
    return !pluginsToUpgrade.isEmpty();
  }

  public boolean isIncompatible() {
    return !incompatiblePlugins.isEmpty();
  }

  public void addCompatiblePlugin(Plugin plugin) {
    compatiblePlugins.add(plugin);
  }

  public void addIncompatiblePlugin(Plugin plugin) {
    incompatiblePlugins.add(plugin);
  }

  public void addPluginToUpgrade(Release plugin) {
    pluginsToUpgrade.add(plugin);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SonarUpdate update = (SonarUpdate) o;
    return release.equals(update.release);
  }

  @Override
  public int hashCode() {
    return release.hashCode();
  }

  @Override
  public int compareTo(SonarUpdate su) {
    return release.compareTo(su.release);
  }
}
