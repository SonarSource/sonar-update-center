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

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class UpdateCenterMatrixTest {

  private UpdateCenter center;

  private Plugin foo;
  private Release foo10;
  private Release foo11;
  private Release foo12;

  private Plugin bar;
  private Release bar10;
  private Release bar11;

  @Before
  public void initCenter() {
    center = new UpdateCenter();
    foo = new Plugin("foo");
    foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1", "2.2").setDownloadUrl("http://server/foo-1.0.jar");
    foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1", "2.2", "2.3").setDownloadUrl("http://server/foo-1.1.jar");
    foo12 = new Release(foo, "1.2").addRequiredSonarVersions("2.3").setDownloadUrl("http://server/foo-1.2.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);
    foo.addRelease(foo12);
    center.addPlugin(foo);

    bar = new Plugin("bar");
    bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1", "2.2").setDownloadUrl("http://server/bar-1.0.jar");
    bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.2.2", "2.3").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar10);
    bar.addRelease(bar11);
    center.addPlugin(bar);
  }

  @Test
  public void should_find_plugin_installed() {
    UpdateCenterMatrix matrix = new UpdateCenterMatrix(center, Version.create("2.1"));
    matrix.registerInstalledPlugin("foo", Version.create("1.0"));

    List<PluginsGroup> pluginsGroups = matrix.getInstalledGroups();
    assertThat(pluginsGroups).hasSize(1);
    assertThat(pluginsGroups.get(0).getKey()).isEqualTo("foo");
  }

  @Test
  public void should_find_plugin_updates() {
    UpdateCenterMatrix matrix = new UpdateCenterMatrix(center, Version.create("2.1"));
    matrix.registerInstalledPlugin("foo", Version.create("1.0"));

    List<GroupUpdate> updates = matrix.findGroupUpdates();
    assertThat(updates).hasSize(2);

    assertThat(updates.get(0).getRelease()).isEqualTo(foo11);
    assertThat(updates.get(0).isCompatible()).isTrue();
    assertThat(updates.get(1).getRelease()).isEqualTo(foo12);
    assertThat(updates.get(1).isCompatible()).isFalse();
    assertThat(updates.get(1).requiresSonarUpgrade()).isTrue();
  }

  @Test
  public void no_plugin_updates_if_last_release_is_installed() {
    UpdateCenterMatrix matrix = new UpdateCenterMatrix(center, Version.create("2.3"));
    matrix.registerInstalledPlugin("foo", Version.create("1.2"));
    assertThat(matrix.findGroupUpdates()).isEmpty();
  }

  @Test
  public void available_plugins_are_only_the_best_releases() {
    UpdateCenterMatrix matrix = new UpdateCenterMatrix(center, Version.create("2.2"));
    matrix.registerInstalledPlugin("foo", Version.create("1.0"));

    List<GroupUpdate> availables = matrix.findAvailableGroups();

    // bar 1.0 is compatible with the installed sonar
    // bar 1.1 requires sonar to be upgraded to 2.2.2 or 2.3
    // => available plugin to install is bar 1.0
    assertThat(availables.size()).isEqualTo(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(bar10);
    assertThat(availables.get(0).isCompatible()).isTrue();
  }

  @Test
  public void available_plugins_require_sonar_upgrade() {
    UpdateCenterMatrix matrix = new UpdateCenterMatrix(center, Version.create("2.2.1"));
    matrix.registerInstalledPlugin("foo", Version.create("1.0"));

    List<GroupUpdate> availables = matrix.findAvailableGroups();

    // bar 1.0 is not compatible with the installed sonar
    // bar 1.1 requires sonar to be upgraded to 2.2.2 or 2.3
    // => available plugin to install is bar 1.1
    assertThat(availables.size()).isEqualTo(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(bar11);
    assertThat(availables.get(0).requiresSonarUpgrade()).isTrue();
  }

  @Test
  public void find_sonar_updates() {
    center.getSonar().addRelease(Version.create("2.3"));
    center.getSonar().addRelease(Version.create("2.4"));

    UpdateCenterMatrix matrix = new UpdateCenterMatrix(center, Version.create("2.2"));
    List<SonarUpdate> updates = matrix.findSonarUpdates();

    // no plugins are installed, so both sonar versions are compatible
    assertThat(updates).hasSize(2);
    assertThat(updates.get(0).hasWarnings()).isFalse();
    assertThat(updates.get(1).hasWarnings()).isFalse();
  }

  @Test
  public void warnings_on_sonar_updates() {
    center.getSonar().addRelease(Version.create("2.3"));
    center.getSonar().addRelease(Version.create("2.4"));

    UpdateCenterMatrix matrix = new UpdateCenterMatrix(center, Version.create("2.2"));
    matrix.registerInstalledPlugin("foo", Version.create("1.0"));
    matrix.registerInstalledPlugin("bar", Version.create("1.0"));
    List<SonarUpdate> updates = matrix.findSonarUpdates();

    assertThat(updates).hasSize(2);

    // sonar 2.3 supports foo 1.1/1.2 and bar 1.1
    // => 2 plugin upgrades are required
    assertThat(updates.get(0).hasWarnings()).isTrue();
    assertThat(updates.get(0).requiresPluginUpgrades()).isTrue();
    assertThat(updates.get(0).getPluginsToUpgrade()).hasSize(2);

    // sonar 2.4 supports no plugins
    assertThat(updates.get(1).hasWarnings()).isTrue();
    assertThat(updates.get(1).isIncompatible()).isTrue();
    assertThat(updates.get(1).getIncompatiblePlugins()).hasSize(2);
  }

  @Test
  public void exclude_pending_downloads_from_plugin_updates() {
    UpdateCenterMatrix matrix = new UpdateCenterMatrix(center, Version.create("2.1"));
    matrix.registerInstalledPlugin("foo", Version.create("1.0"));
    matrix.registerPendingPluginsByFilename("foo-1.0.jar");
    List<GroupUpdate> updates = matrix.findGroupUpdates();
    assertThat(updates).hasSize(0);
  }

  @Test
  public void exclude_pending_downloads_from_available_plugins() {
    UpdateCenterMatrix matrix = new UpdateCenterMatrix(center, Version.create("2.1"));
    matrix.registerPendingPluginsByFilename("foo-1.0.jar");
    matrix.registerPendingPluginsByFilename("bar-1.1.jar");
    List<GroupUpdate> updates = matrix.findAvailableGroups();
    assertThat(updates).hasSize(0);
  }
}