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
import java.util.List;

public final class SonarUpdate implements Comparable<SonarUpdate> {

  private final Release release;
  private final List<Plugin> compatiblePlugins = new ArrayList<>();
  private final List<Plugin> incompatiblePlugins = new ArrayList<>();
  private final List<Release> pluginsToUpgrade = new ArrayList<>();

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
