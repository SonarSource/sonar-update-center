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
    PluginCenter pluginCenter = PluginCenter.create(pluginReferential,
        PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.0").getArtifact())),
        Version.create("2.1"));

    PluginReferential installed = pluginCenter.getInstalledPluginReferential();
    assertThat(installed.getPlugins()).hasSize(1);
    assertThat(installed.getPlugins().get(0).getKey()).isEqualTo("foo");
  }

  @Test
  public void should_find_plugin_updates() {
    PluginCenter pluginCenter = PluginCenter.create(pluginReferential,
        PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.0").getArtifact())),
        Version.create("2.1"));

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
    PluginCenter pluginCenter = PluginCenter.create(pluginReferential,
        PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.2").getArtifact())),
        Version.create("2.3"));
    assertThat(pluginCenter.findPluginUpdates()).isEmpty();
  }

  @Test
  public void available_plugins_are_only_the_best_releases() {
    PluginCenter pluginCenter = PluginCenter.create(pluginReferential,
        PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.0").getArtifact())),
        Version.create("2.2"));

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
    PluginCenter pluginCenter = PluginCenter.create(pluginReferential,
        PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.0").getArtifact())),
        Version.create("2.2.1"));

    List<PluginUpdate> availables = pluginCenter.findAvailablePlugins();

    // bar 1.0 is not compatible with the installed sonar
    // bar 1.1 requires sonar to be upgraded to 2.2.2 or 2.3
    // => available plugin to install is bar 1.1
    assertThat(availables).hasSize(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(bar11);
    assertThat(availables.get(0).requiresSonarUpgrade()).isTrue();
  }

  @Test
  public void should_return_only_master_plugins_when_getting_available_plugins() {
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

    PluginCenter pluginCenter = PluginCenter.create(pluginReferential,
        PluginReferential.create(newArrayList((Plugin) new Plugin("test").addRelease("1.0").getArtifact())),
        Version.create("2.1"));

    List<PluginUpdate> availables = pluginCenter.findAvailablePlugins();
    assertThat(availables).hasSize(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(foo10);
    assertThat(availables.get(0).isCompatible()).isTrue();
    assertThat(availables.get(0).getPlugin().getChildren()).hasSize(1);
  }

  @Test
  public void should_find_latest_compatible_plugin() {
    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/test-1.0.jar");
    Release test11 = new Release(test, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/test-1.1.jar");
    Release test12 = new Release(test, "1.2").addRequiredSonarVersions("2.2").setDownloadUrl("http://server/test-1.1.jar");
    test.addRelease(test10);
    test.addRelease(test11);
    test.addRelease(test12);

    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.2").setDownloadUrl("http://server/foo-1.0.jar");
    foo.addRelease(foo10);

    PluginCenter pluginCenter = PluginCenter.createForUpdateCenterPlugins(
        PluginReferential.create(newArrayList(test, foo), (Sonar) (new Sonar().addRelease(Version.createRelease("2.1")).getArtifact())),
        Version.create("2.1"));

    Release release = pluginCenter.findLatestCompatibleRelease("test");
    assertThat(release).isNotNull();
    assertThat(release.getVersion().getName()).isEqualTo("1.1");

    assertThat(pluginCenter.findLatestCompatibleRelease("foo")).isNull();
    assertThat(pluginCenter.findLatestCompatibleRelease("unkownw")).isNull();
  }

  @Test
  public void should_return_releases_to_download_with_children_and_dependencies_that_are_not_already_downloaded_when_searching_releases_to_install() {
    // Standalone plugin
    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/test-1.0.jar");
    test.addRelease(test10);

    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = new Plugin("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.0.jar").addOutgoingDependency(foo10);
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar").addOutgoingDependency(foo11);
//    Release foobis12 = new Release(foobis, "1.2").addRequiredSonarVersions("2.2").setDownloadUrl("http://server/foobis-1.2.jar").addOutgoingDependency(foo11); // TODO
    foobis.addRelease(foobis10);
    foobis.addRelease(foobis11);
//    foobis.addRelease(foobis12);

    // bar has one child and depends upon foobis
    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar").addOutgoingDependency(foobis10);
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar").addOutgoingDependency(foobis11);
    bar.addRelease(bar10);
    bar.addRelease(bar11);
    Plugin barbis = new Plugin("barbis").setParent(bar);
    Release barbis10 = new Release(barbis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/barbis-1.0.jar");
    Release barbis11 = new Release(barbis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/barbis-1.1.jar");
    barbis.addRelease(barbis10);
    barbis.addRelease(barbis11);

    PluginCenter pluginCenter = PluginCenter.create(
        PluginReferential.create(newArrayList(foo, foobis, bar, test), (Sonar) (new Sonar().addRelease(Version.createRelease("2.1")).getArtifact())),
        PluginReferential.create(newArrayList(
            (Plugin) new Plugin("bar").addRelease("1.0").getArtifact(),
            (Plugin) new Plugin("foo").addRelease("1.1").getArtifact())),
        Version.create("2.1"));

    List<Release> installablePlugins = pluginCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(3);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("barbis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNull();  // foo is already downloaded
  }

  @Test
  public void should_not_return_incompatible_releases_to_download() {
    Plugin foo = new Plugin("foo");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    Release foo12 = new Release(foo, "1.2").addRequiredSonarVersions("2.2").setDownloadUrl("http://server/foo-1.2.jar");
    foo.addRelease(foo11);
    foo.addRelease(foo12);

    // foobis depends upon foo
    Plugin foobis = new Plugin("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar").addOutgoingDependency(foo11);
    // foobis 1.2 should not to be downloaded
    Release foobis12 = new Release(foobis, "1.2").addRequiredSonarVersions("2.2").setDownloadUrl("http://server/foobis-1.2.jar").addOutgoingDependency(foo11);
    foobis.addRelease(foobis11);
    foobis.addRelease(foobis12);

    // bar depends upon foobis
    Plugin bar = new Plugin("bar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar").addOutgoingDependency(foobis11);
    bar.addRelease(bar11);

    PluginCenter pluginCenter = PluginCenter.createForUpdateCenterPlugins(
        PluginReferential.create(newArrayList(foo, foobis, bar), (Sonar) (new Sonar().addRelease(Version.createRelease("2.1")).getArtifact())),
        Version.create("2.1"));

    List<Release> installablePlugins = pluginCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(3);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void should_return_latest_compatible_releases_with_sonar_version_to_download() {
    Plugin foobis = new Plugin("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    Release foobis12 = new Release(foobis, "1.2").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.2.jar");
    foobis.addRelease(foobis11);
    foobis.addRelease(foobis12);

    // bar depends upon foobis 1.1
    Plugin bar = new Plugin("bar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar").addOutgoingDependency(foobis11);
    bar.addRelease(bar11);

    PluginCenter pluginCenter = PluginCenter.createForUpdateCenterPlugins(
        PluginReferential.create(newArrayList(foobis, bar), (Sonar) (new Sonar().addRelease(Version.createRelease("2.1")).getArtifact())),
        Version.create("2.1"));

    List<Release> installablePlugins = pluginCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(2);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.2", installablePlugins)).isNotNull(); // foobis 1.2 is compatible with sonar 2.1, so it's this version that will be downloaded
  }

  @Test
  public void should_return_outcoming_and_incoming_dependencies_when_searching_releases_to_update() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = new Plugin("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.0.jar").addOutgoingDependency(foo10);
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar").addOutgoingDependency(foo11);
    foobis.addRelease(foobis10);
    foobis.addRelease(foobis11);

    // bar has one child and depends upon foobis
    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar").addOutgoingDependency(foobis10);
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar").addOutgoingDependency(foobis11);
    bar.addRelease(bar10);
    bar.addRelease(bar11);
    Plugin barbis = new Plugin("barbis").setParent(bar);
    Release barbis10 = new Release(barbis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/barbis-1.0.jar");
    Release barbis11 = new Release(barbis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/barbis-1.1.jar");
    barbis.addRelease(barbis10);
    barbis.addRelease(barbis11);

    PluginCenter pluginCenter = PluginCenter.create(
        PluginReferential.create(newArrayList(foo, foobis, bar), (Sonar) (new Sonar().addRelease(Version.createRelease("2.1")).getArtifact())),
        PluginReferential.create(newArrayList(
            (Plugin) new Plugin("foo").addRelease("1.0").getArtifact(),
            (Plugin) new Plugin("foobis").addRelease("1.0").getArtifact(),
            (Plugin) new Plugin("bar").addRelease("1.0").getArtifact(),
            (Plugin) new Plugin("barbis").addRelease("1.0").getArtifact()
        )),
        Version.create("2.1"));

    List<Release> installablePlugins = pluginCenter.findInstallablePlugins("foobis", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(4);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("barbis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void should_return_releases_keys_to_remove() {
    // Standalone plugin
    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/test-1.0.jar");
    test.addRelease(test10);

    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    foo.addRelease(foo10);

    // foobis depends upon foo
    Plugin foobis = new Plugin("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.0.jar").addOutgoingDependency(foo10);
    foobis.addRelease(foobis10);

    // bar has one child and depends upon foobis
    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar").addOutgoingDependency(foobis10);
    bar.addRelease(bar10);
    Plugin barbis = new Plugin("barbis").setParent(bar);
    Release barbis10 = new Release(barbis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/barbis-1.0.jar");
    barbis.addRelease(barbis10);

    PluginCenter pluginCenter = PluginCenter.createForInstalledPlugins(
        PluginReferential.create(newArrayList(foo, foobis, bar, test), (Sonar) (new Sonar().addRelease(Version.createRelease("2.1")).getArtifact())),
        Version.create("2.1"));

    List<String> installablePlugins = pluginCenter.findRemovableReleases("foo");
    assertThat(installablePlugins).containsExactly("foo", "foobis", "bar", "barbis"); // test will not be removed
  }

  @Test
  public void find_sonar_updates() {
    pluginReferential.getSonar().addRelease(Version.create("2.3"));
    pluginReferential.getSonar().addRelease(Version.create("2.4"));

    PluginCenter pluginCenter = PluginCenter.createForUpdateCenterPlugins(pluginReferential, Version.create("2.2"));
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

    PluginCenter pluginCenter = PluginCenter.create(pluginReferential,
        PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.0").getArtifact(), (Plugin) new Plugin("bar").addRelease("1.0").getArtifact())),
        Version.create("2.2"));
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

  @Nullable
  public Release getRelease(final String key, final String version, List<Release> releases) {
    return Iterables.find(releases, new Predicate<Release>() {
      public boolean apply(Release input) {
        return input.getArtifact().getKey().equals(key) && input.getVersion().getName().equals(version);
      }
    }, null);
  }

}