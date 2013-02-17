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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
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
    PluginCenter pluginCenter = new PluginCenter(pluginReferential, Version.create("2.1"));
    pluginCenter.registerInstalledPlugin("foo", Version.create("1.0"));

    List<Release> installed = pluginCenter.getInstalledReleases();
    assertThat(installed).hasSize(1);
    assertThat(installed.get(0).getArtifact().getKey()).isEqualTo("foo");
  }

  @Test
  public void should_find_plugin_updates() {
    PluginCenter pluginCenter = new PluginCenter(pluginReferential, Version.create("2.1"));
    pluginCenter.registerInstalledPlugin("foo", Version.create("1.0"));

    List<PluginUpdate> updates = pluginCenter.findPluginUpdates();
    assertThat(updates).hasSize(2);

    assertThat(updates.get(0).getRelease()).isEqualTo(foo11);
    assertThat(updates.get(0).isCompatible()).isTrue();
    assertThat(updates.get(1).getRelease()).isEqualTo(foo12);
    assertThat(updates.get(1).isCompatible()).isFalse();
    assertThat(updates.get(1).requiresSonarUpgrade()).isTrue();
  }

  @Test
  public void no_plugin_updates_if_last_release_is_installed() {
    PluginCenter pluginCenter = new PluginCenter(pluginReferential, Version.create("2.3"));
    pluginCenter.registerInstalledPlugin("foo", Version.create("1.2"));
    assertThat(pluginCenter.findPluginUpdates()).isEmpty();
  }

  @Test
  public void available_plugins_are_only_the_best_releases() {
    PluginCenter pluginCenter = new PluginCenter(pluginReferential, Version.create("2.2"));
    pluginCenter.registerInstalledPlugin("foo", Version.create("1.0"));

    List<PluginUpdate> availables = pluginCenter.findAvailablePlugins();

    // bar 1.0 is compatible with the installed sonar
    // bar 1.1 requires sonar to be upgraded to 2.2.2 or 2.3
    // => available plugin to install is bar 1.0
    assertThat(availables.size()).isEqualTo(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(bar10);
    assertThat(availables.get(0).isCompatible()).isTrue();
  }

  @Test
  public void available_plugins_require_sonar_upgrade() {
    PluginCenter pluginCenter = new PluginCenter(pluginReferential, Version.create("2.2.1"));
    pluginCenter.registerInstalledPlugin("foo", Version.create("1.0"));

    List<PluginUpdate> availables = pluginCenter.findAvailablePlugins();

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
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/test-1.0.jar");
    test.addRelease(test10);

    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    foo.addRelease(foo10);

    Plugin fooChild = new Plugin("bar").setParent(foo);
    Release fooChild10 = new Release(fooChild, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-child-1.0.jar");
    fooChild.addRelease(fooChild10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, fooChild, test), (Sonar) (new Sonar().addRelease(Version.createRelease("2.1")).getArtifact()));
    pluginReferential.getSonar().addRelease(Version.create("2.1"));

    PluginCenter pluginCenter = new PluginCenter(pluginReferential, Version.create("2.1"));
    pluginCenter.registerInstalledPlugin("test", Version.create("1.0"));

    List<PluginUpdate> availables = pluginCenter.findAvailablePlugins();
    assertThat(availables).hasSize(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(foo10);
    assertThat(availables.get(0).isCompatible()).isTrue();
    assertThat(availables.get(0).getPlugin().getChildren()).hasSize(1);
  }

  @Test
  public void find_sonar_updates() {
    pluginReferential.getSonar().addRelease(Version.create("2.3"));
    pluginReferential.getSonar().addRelease(Version.create("2.4"));

    PluginCenter pluginCenter = new PluginCenter(pluginReferential, Version.create("2.2"));
    List<SonarUpdate> updates = pluginCenter.findSonarUpdates();

    // no plugins are installed, so both sonar versions are compatible
    assertThat(updates).hasSize(2);
    assertThat(updates.get(0).hasWarnings()).isFalse();
    assertThat(updates.get(1).hasWarnings()).isFalse();
  }

  @Test
  public void warnings_on_sonar_updates() {
    pluginReferential.getSonar().addRelease(Version.create("2.3"));
    pluginReferential.getSonar().addRelease(Version.create("2.4"));

    PluginCenter pluginCenter = new PluginCenter(pluginReferential, Version.create("2.2"));
    pluginCenter.registerInstalledPlugin("foo", Version.create("1.0"));
    pluginCenter.registerInstalledPlugin("bar", Version.create("1.0"));
    List<SonarUpdate> updates = pluginCenter.findSonarUpdates();

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
//    PluginCenter pluginCenter = new PluginCenter(plugins, Version.create("2.1"));
//    pluginCenter.registerInstalledPlugin("foo", Version.create("1.0"));
//    pluginCenter.registerPendingPluginsByFilename("foo-1.0.jar");
//    List<PluginUpdate> updates = pluginCenter.findPluginUpdates();
//    assertThat(updates).hasSize(0);
//  }
//
//  @Test
//  public void exclude_pending_downloads_from_available_plugins() {
//    PluginCenter pluginCenter = new PluginCenter(plugins, Version.create("2.1"));
//    pluginCenter.registerPendingPluginsByFilename("foo-1.0.jar");
//    pluginCenter.registerPendingPluginsByFilename("bar-1.1.jar");
//    List<PluginUpdate> updates = pluginCenter.findAvailablePlugins();
//    assertThat(updates).hasSize(0);
//  }

  @Test
  public void should_return_children_releases_to_download_that_are_not_already_installed() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    foo.addRelease(foo10);

    Plugin foobis = new Plugin("foobis").setParent(foo);
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.0.jar");
    foobis.addRelease(foobis10);

    Plugin bar = new Plugin("bar").setParent(foo);
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, foobis, bar), (Sonar) (new Sonar().addRelease(Version.createRelease("2.1")).getArtifact()));
    PluginCenter pluginCenter = new PluginCenter(pluginReferential, Version.create("2.1"));
    pluginCenter.registerInstalledPlugin("bar", Version.create("1.0"));

    List<Release> installablePlugins = pluginCenter.findInstallablePlugins("foo", Version.create("1.0"));
    assertThat(installablePlugins).hasSize(2);
    assertThat(getRelease("foo", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", installablePlugins)).isNotNull();
    assertThat(getRelease("bar", installablePlugins)).isNull();
  }

  @Test
  public void should_return_dependencies_to_download_that_are_not_already_installed() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    foo.addRelease(foo10);

    Plugin foobis = new Plugin("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.0.jar");
    foobis.addRelease(foobis10);

    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar").addRequiredRelease(foo10).addRequiredRelease(foobis10);
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, foobis, bar), (Sonar) (new Sonar().addRelease(Version.createRelease("2.1")).getArtifact()));
    PluginCenter pluginCenter = new PluginCenter(pluginReferential, Version.create("2.1"));
    pluginCenter.registerInstalledPlugin("foobis", Version.create("1.0"));

    List<Release> installablePlugins = pluginCenter.findInstallablePlugins("bar", Version.create("1.0"));
    assertThat(installablePlugins).hasSize(2);
    assertThat(getRelease("bar", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", installablePlugins)).isNotNull();
  }

  @Test
  public void should_return_plugin_keys_to_remove() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    foo.addRelease(foo10);

    Plugin foobis = new Plugin("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.0.jar");
    foobis.addRelease(foobis10);

    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar").addRequiredRelease(foo10).addRequiredRelease(foobis10);
    bar.addRelease(bar10);
    Plugin barbis = new Plugin("barbis").setParent(bar);
    Release barbis10 = new Release(barbis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/barbis-1.0.jar");
    barbis.addRelease(barbis10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, foobis, bar), (Sonar) (new Sonar().addRelease(Version.createRelease("2.1")).getArtifact()));
    PluginCenter pluginCenter = new PluginCenter(pluginReferential, Version.create("2.1"));

    List<String> installablePlugins = pluginCenter.findRemovablePlugins("bar");
    assertThat(installablePlugins).contains("bar", "barbis", "foo", "foobis");
  }

  @Nullable
  public Release getRelease(final String key, List<Release> releases) {
    return Iterables.find(releases, new Predicate<Release>() {
      public boolean apply(Release input) {
        return input.getArtifact().getKey().equals(key);
      }
    }, null);
  }

}