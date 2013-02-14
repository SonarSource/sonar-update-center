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

public class PluginParentUpdate {

  public enum Status {
    COMPATIBLE, INCOMPATIBLE, REQUIRE_SONAR_UPGRADE
  }

  private Status status = Status.COMPATIBLE;
  private PluginParent pluginParent;
  private Release release;

  public PluginParentUpdate(PluginParent pluginParent) {
    this.pluginParent = pluginParent;
  }

  public Status getStatus() {
    return status;
  }

  public PluginParent getPluginParent() {
    return pluginParent;
  }

  public Plugin getPlugin() {
    return pluginParent.getMasterPlugin();
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Release getRelease() {
    return release;
  }

  public void setRelease(Release release) {
    this.release = release;
  }

  public boolean isCompatible() {
    return Status.COMPATIBLE.equals(status);
  }

  public boolean isIncompatible() {
    return Status.INCOMPATIBLE.equals(status);
  }

  public boolean requiresSonarUpgrade() {
    return Status.REQUIRE_SONAR_UPGRADE.equals(status);
  }

  public static PluginParentUpdate createWithStatus(PluginParent pluginParent, Release pluginRelease, Status status) {
    PluginParentUpdate update = new PluginParentUpdate(pluginParent);
    update.setRelease(pluginRelease);
    update.setStatus(status);
    return update;
  }

  public static PluginParentUpdate createForPluginRelease(PluginParent pluginParent, Release pluginRelease, Version sonarVersion) {
    PluginParentUpdate update = new PluginParentUpdate(pluginParent);
    update.setRelease(pluginRelease);

    if (pluginRelease.supportSonarVersion(sonarVersion)) {
      update.setStatus(Status.COMPATIBLE);

    } else {
      for (Version requiredSonarVersion : pluginRelease.getRequiredSonarVersions()) {
        if (requiredSonarVersion.compareTo(sonarVersion)>0) {
          update.setStatus(Status.REQUIRE_SONAR_UPGRADE);
          break;
        }
      }
    }
    return update;
  }
}
