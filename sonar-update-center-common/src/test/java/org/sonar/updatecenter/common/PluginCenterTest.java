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
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class PluginCenterTest {

  private PluginReferential pluginReferential;

  private Plugin foo;
  private Release foo10;
  private Release foo11;
  private Release foo12;

  private Plugin bar;
  private Release bar10;
  private Release bar11;

  @Before
  public void initCenter() {
    foo = new Plugin("foo");
    foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1", "2.2").setDownloadUrl("http://server/foo-1.0.jar");
    foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1", "2.2", "2.3").setDownloadUrl("http://server/foo-1.1.jar");
    foo12 = new Release(foo, "1.2").addRequiredSonarVersions("2.3").setDownloadUrl("http://server/foo-1.2.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);
    foo.addRelease(foo12);

    bar = new Plugin("bar");
    bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1", "2.2").setDownloadUrl("http://server/bar-1.0.jar");
    bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.2.2", "2.3").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar10);
    bar.addRelease(bar11);

    pluginReferential = PluginReferential.create(newArrayList(foo, bar), new Sonar());
  }

  @Test
  public void should_find_plugin_installed() {
    PluginCenter matrix = new PluginCenter(pluginReferential, Version.create("2.1"));
    matrix.registerInstalledPlugin("foo", Version.create("1.0"));

    List<Plugin> pluginParents = matrix.getInstalledPlugins();
    assertThat(pluginParents).hasSize(1);
    assertThat(pluginParents.get(0).getKey()).isEqualTo("foo");
  }

  @Test
  public void should_find_plugin_updates() {
    PluginCenter matrix = new PluginCenter(pluginReferential, Version.create("2.1"));
    matrix.registerInstalledPlugin("foo", Version.create("1.0"));

    List<PluginUpdate> updates = matrix.findPluginUpdates();
    assertThat(updates).hasSize(2);

    assertThat(updates.get(0).getRelease()).isEqualTo(foo11);
    assertThat(updates.get(0).isCompatible()).isTrue();
    assertThat(updates.get(1).getRelease()).isEqualTo(foo12);
    assertThat(updates.get(1).isCompatible()).isFalse();
    assertThat(updates.get(1).requiresSonarUpgrade()).isTrue();
  }

  @Test
  public void no_plugin_updates_if_last_release_is_installed() {
    PluginCenter matrix = new PluginCenter(pluginReferential, Version.create("2.3"));
    matrix.registerInstalledPlugin("foo", Version.create("1.2"));
    assertThat(matrix.findPluginUpdates()).isEmpty();
  }

  @Test
  public void available_plugins_are_only_the_best_releases() {
    PluginCenter matrix = new PluginCenter(pluginReferential, Version.create("2.2"));
    matrix.registerInstalledPlugin("foo", Version.create("1.0"));

    List<PluginUpdate> availables = matrix.findAvailablePlugins();

    // bar 1.0 is compatible with the installed sonar
    // bar 1.1 requires sonar to be upgraded to 2.2.2 or 2.3
    // => available plugin to install is bar 1.0
    assertThat(availables.size()).isEqualTo(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(bar10);
    assertThat(availables.get(0).isCompatible()).isTrue();
  }

  @Test
  public void available_plugins_require_sonar_upgrade() {
    PluginCenter matrix = new PluginCenter(pluginReferential, Version.create("2.2.1"));
    matrix.registerInstalledPlugin("foo", Version.create("1.0"));

    List<PluginUpdate> availables = matrix.findAvailablePlugins();

    // bar 1.0 is not compatible with the installed sonar
    // bar 1.1 requires sonar to be upgraded to 2.2.2 or 2.3
    // => available plugin to install is bar 1.1
    assertThat(availables).hasSize(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(bar11);
    assertThat(availables.get(0).requiresSonarUpgrade()).isTrue();
  }

  @Test
  public void should_return_only_parent_plugins_when_getting_availables_plugins() {
    Plugin test = new Plugin("test");
    Release test10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/test-1.0.jar");
    test.addRelease(test10);

    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    foo.addRelease(foo10);

    Plugin bar = new Plugin("bar").setParent(foo);
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, bar, test));
    pluginReferential.getSonar().addRelease(Version.create("2.1"));

    PluginCenter matrix = new PluginCenter(pluginReferential, Version.create("2.1"));
    matrix.registerInstalledPlugin("test", Version.create("1.0"));

    List<PluginUpdate> availables = matrix.findAvailablePlugins();
    assertThat(availables).hasSize(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(foo10);
    assertThat(availables.get(0).isCompatible()).isTrue();
    assertThat(availables.get(0).getPlugin().getChildren()).hasSize(1);
  }

  @Test
  public void find_sonar_updates() {
    pluginReferential.getSonar().addRelease(Version.create("2.3"));
    pluginReferential.getSonar().addRelease(Version.create("2.4"));

    PluginCenter matrix = new PluginCenter(pluginReferential, Version.create("2.2"));
    List<SonarUpdate> updates = matrix.findSonarUpdates();

    // no plugins are installed, so both sonar versions are compatible
    assertThat(updates).hasSize(2);
    assertThat(updates.get(0).hasWarnings()).isFalse();
    assertThat(updates.get(1).hasWarnings()).isFalse();
  }

  @Test
  public void warnings_on_sonar_updates() {
    pluginReferential.getSonar().addRelease(Version.create("2.3"));
    pluginReferential.getSonar().addRelease(Version.create("2.4"));

    PluginCenter matrix = new PluginCenter(pluginReferential, Version.create("2.2"));
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

//  @Test
//  public void exclude_pending_downloads_from_plugin_updates() {
//    PluginCenter matrix = new PluginCenter(plugins, Version.create("2.1"));
//    matrix.registerInstalledPlugin("foo", Version.create("1.0"));
//    matrix.registerPendingPluginsByFilename("foo-1.0.jar");
//    List<PluginUpdate> updates = matrix.findPluginUpdates();
//    assertThat(updates).hasSize(0);
//  }
//
//  @Test
//  public void exclude_pending_downloads_from_available_plugins() {
//    PluginCenter matrix = new PluginCenter(plugins, Version.create("2.1"));
//    matrix.registerPendingPluginsByFilename("foo-1.0.jar");
//    matrix.registerPendingPluginsByFilename("bar-1.1.jar");
//    List<PluginUpdate> updates = matrix.findAvailablePlugins();
//    assertThat(updates).hasSize(0);
//  }

  @Test
  public void should_return_children_plugins_to_download() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    foo.addRelease(foo10);

    Plugin bar = new Plugin("bar").setParent(foo);
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, bar));

    PluginCenter matrix = new PluginCenter(pluginReferential, Version.create("2.1"));
    matrix.registerInstalledPlugin("foo", Version.create("1.0"));
    matrix.registerInstalledPlugin("bar", Version.create("1.0"));

    List<Release> installablePlugins = matrix.findInstallablePlugins("foo", Version.create("1.0"));
    assertThat(installablePlugins).hasSize(2);
  }

  @Test
  @Ignore
  public void should_return_dependencies_plugin_to_download() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    foo.addRelease(foo10);

    Plugin bar = new Plugin("bar").addRequired(new Release(foo, "1.0"));
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, bar));

    PluginCenter matrix = new PluginCenter(pluginReferential, Version.create("2.1"));
    matrix.registerInstalledPlugin("foo", Version.create("1.0"));
    matrix.registerInstalledPlugin("bar", Version.create("1.0"));

    List<Release> installablePlugins = matrix.findInstallablePlugins("bar", Version.create("1.0"));
    // TODO
    assertThat(installablePlugins).hasSize(1);
//    assertThat(installablePlugins.get(0).getPlugin().getKey()).isEqualTo("foo");
  }

}