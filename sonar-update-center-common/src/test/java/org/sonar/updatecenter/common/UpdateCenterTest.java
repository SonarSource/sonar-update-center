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
import org.junit.Before;
import org.junit.Test;
import org.sonar.updatecenter.common.exception.IncompatiblePluginVersionException;
import org.sonar.updatecenter.common.exception.PluginNotFoundException;

import javax.annotation.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class UpdateCenterTest {

  private PluginReferential pluginReferential;
  private Plugin foo;
  private Release foo10;
  private Release foo11;
  private Release foo12;
  private Plugin bar;
  private Release bar10;
  private Release bar11;
  private Sonar sonar;

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

    sonar = new Sonar();
    pluginReferential = PluginReferential.create(newArrayList(foo, bar));
  }

  @Test
  public void should_find_plugin_installed() {
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
        PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.0").getArtifact())));

    PluginReferential installed = updateCenter.getInstalledPluginReferential();
    assertThat(installed.getPlugins()).hasSize(1);
    assertThat(installed.getPlugins().get(0).getKey()).isEqualTo("foo");
  }

  @Test
  public void should_find_plugin_updates() {
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
        PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.0").getArtifact())));

    List<PluginUpdate> updates = updateCenter.findPluginUpdates();
    assertThat(updates).hasSize(2);

    assertThat(updates.get(0).getRelease()).isEqualTo(foo11);
    assertThat(updates.get(0).isCompatible()).isTrue();
    assertThat(updates.get(1).getRelease()).isEqualTo(foo12);
    assertThat(updates.get(1).isCompatible()).isFalse();
    assertThat(updates.get(1).requiresSonarUpgrade()).isTrue();
  }

  @Test
  public void should_find_plugin_updates_with_sonar_upgrade() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo));
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
        PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.0").getArtifact())));

    List<PluginUpdate> updates = updateCenter.findPluginUpdates();
    assertThat(updates).hasSize(1);
    assertThat(updates.get(0).getRelease()).isEqualTo(foo11);
    assertThat(updates.get(0).isCompatible()).isTrue();
  }

  @Test
  public void should_find_plugin_updates_with_dependencies_needed_sonar_upgrade() {
    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/test-1.0.jar");
    Release test11 = new Release(test, "1.1").addRequiredSonarVersions("2.2").setDownloadUrl("http://server/test-1.1.jar");
    test.addRelease(test10);
    test.addRelease(test11);

    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, test));
    pluginReferential.addOutgoingDependency(foo11, "test", "1.1");

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
        PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.0").getArtifact())));

    List<PluginUpdate> updates = updateCenter.findPluginUpdates();
    assertThat(updates).hasSize(1);
    assertThat(updates.get(0).getRelease()).isEqualTo(foo11);
    assertThat(updates.get(0).requiresSonarUpgradeForDependencies()).isTrue();
  }

  @Test
  public void no_plugin_updates_if_last_release_is_installed() {
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.3")).registerInstalledPlugins(
        PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.2").getArtifact())));
    assertThat(updateCenter.findPluginUpdates()).isEmpty();
  }

  @Test
  public void available_plugins_are_only_the_best_releases() {
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.2")).registerInstalledPlugins(
        PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.0").getArtifact())));

    List<PluginUpdate> availables = updateCenter.findAvailablePlugins();

    // bar 1.0 is compatible with the installed sonar
    // bar 1.1 requires sonar to be upgraded to 2.2.2 or 2.3
    // => available plugin to install is bar 1.0
    assertThat(availables.size()).isEqualTo(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(bar10);
    assertThat(availables.get(0).isCompatible()).isTrue();
  }

  @Test
  public void available_plugins_require_sonar_upgrade() {
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.2.1")).registerInstalledPlugins(
        PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.0").getArtifact())));

    List<PluginUpdate> availables = updateCenter.findAvailablePlugins();

    // bar 1.0 is not compatible with the installed sonar
    // bar 1.1 requires sonar to be upgraded to 2.2.2 or 2.3
    // => available plugin to install is bar 1.1
    assertThat(availables).hasSize(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(bar11);
    assertThat(availables.get(0).requiresSonarUpgrade()).isTrue();
  }

  @Test
  public void available_plugins_require_dependencies_sonar_upgrade() {
    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/test-1.0.jar");
    Release test11 = new Release(test, "1.1").addRequiredSonarVersions("2.2").setDownloadUrl("http://server/test-1.1.jar");
    test.addRelease(test10);
    test.addRelease(test11);

    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    foo.addRelease(foo10);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, test));
    pluginReferential.addOutgoingDependency(foo10, "test", "1.1");

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
        PluginReferential.create(newArrayList((Plugin) new Plugin("test").addRelease("1.0").getArtifact())));

    List<PluginUpdate> availables = updateCenter.findAvailablePlugins();
    assertThat(availables).hasSize(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(foo10);
    assertThat(availables.get(0).requiresSonarUpgradeForDependencies()).isTrue();
  }

  @Test
  public void should_return_only_master_plugins_when_getting_available_plugins() {
    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/test-1.0.jar");
    test.addRelease(test10);

    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    foo.addRelease(foo10);

    Plugin fooChild = new Plugin("bar");
    Release fooChild10 = new Release(fooChild, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-child-1.0.jar");
    fooChild.addRelease(fooChild10);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, fooChild, test));
    pluginReferential.setParent(fooChild, "foo");

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
        PluginReferential.create(newArrayList((Plugin) new Plugin("test").addRelease("1.0").getArtifact())));

    List<PluginUpdate> availables = updateCenter.findAvailablePlugins();
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

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
        PluginReferential.create(newArrayList(test, foo)), sonar)
        .setInstalledSonarVersion(Version.create("2.1"));

    Release release = updateCenter.findLatestCompatibleRelease("test");
    assertThat(release).isNotNull();
    assertThat(release.getVersion().getName()).isEqualTo("1.1");

    assertThat(updateCenter.findLatestCompatibleRelease("foo")).isNull();
    assertThat(updateCenter.findLatestCompatibleRelease("unkownw")).isNull();
  }

  @Test
  public void should_return_latest_releases_to_download() {
    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar10);
    bar.addRelease(bar11);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
        PluginReferential.create(newArrayList(bar)), sonar)
        .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.0"));
    assertThat(installablePlugins).hasSize(1);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void should_return_releases_compatible_with_installed_sonar_version_to_download() {
    Plugin bar = new Plugin("bar");
    // bar 1.0 est not compatible with sonar 2.1
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.0").setDownloadUrl("http://server/bar-1.0.jar");
    bar.addRelease(bar10);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
        PluginReferential.create(newArrayList(bar)), sonar)
        .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.0"));
    assertThat(installablePlugins).hasSize(0);
  }

  @Test
  public void should_return_releases_to_download() {
    Plugin bar = new Plugin("bar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar11);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
        PluginReferential.create(newArrayList(bar)), sonar)
        .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(1);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
  }

  @Test(expected = PluginNotFoundException.class)
  public void should_throw_exception_if_plugin_to_download_not_found() {
    Plugin bar = new Plugin("bar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar11);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
        PluginReferential.create(newArrayList(bar)), sonar)
        .setInstalledSonarVersion(Version.create("2.1"));

    updateCenter.findInstallablePlugins("not_found", Version.create("1.1"));
  }

  @Test
  public void should_return_releases_to_download_with_children() {
    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar10);
    bar.addRelease(bar11);
    Plugin barbis = new Plugin("barbis");
    Release barbis10 = new Release(barbis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/barbis-1.0.jar");
    Release barbis11 = new Release(barbis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/barbis-1.1.jar");
    barbis.addRelease(barbis10);
    barbis.addRelease(barbis11);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(bar));
    pluginReferential.setParent(barbis, "bar");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
        pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
        PluginReferential.create(newArrayList(
            (Plugin) new Plugin("bar").addRelease("1.0").getArtifact()
        )));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(2);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("barbis", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void should_return_release_dependencies_to_download() {
    Plugin foo = new Plugin("foo");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = new Plugin("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis11);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, foobis));
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("foobis", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(2);
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();
  }

  @Test(expected = IncompatiblePluginVersionException.class)
  public void should_throw_exception_if_dependency_not_found() {
    Plugin foo = new Plugin("foo");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = new Plugin("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis11);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, foobis));
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.2");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.1"));

    updateCenter.findInstallablePlugins("foobis", Version.create("1.1"));
  }

  @Test
  public void should_return_release_dependencies_not_already_downloaded_to_download() {
    Plugin foo = new Plugin("foo");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = new Plugin("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis11);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, foobis));
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
        pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
        PluginReferential.create(newArrayList(
            (Plugin) new Plugin("foo").addRelease("1.1").getArtifact()
        )));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("foobis", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(1);
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNull();
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
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    // foobis 1.2 should not to be downloaded
    Release foobis12 = new Release(foobis, "1.2").addRequiredSonarVersions("2.2").setDownloadUrl("http://server/foobis-1.2.jar");
    foobis.addRelease(foobis11);
    foobis.addRelease(foobis12);

    // bar depends upon foobis
    Plugin bar = new Plugin("bar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar11);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, foobis, bar));
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.1");
    pluginReferential.addOutgoingDependency(foobis12, "foo", "1.1");
    pluginReferential.addOutgoingDependency(bar11, "foobis", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
        pluginReferential, sonar)
        .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
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
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar11);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foobis, bar));
    pluginReferential.addOutgoingDependency(bar11, "foobis", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
        pluginReferential, sonar)
        .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(2);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.2", installablePlugins)).isNotNull(); // foobis 1.2 is compatible with sonar 2.1, so it's this version that will be downloaded
  }

  @Test
  public void should_return_outcoming_and_incoming_dependencies_to_download() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = new Plugin("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.0.jar");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis10);
    foobis.addRelease(foobis11);

    // bar has one child and depends upon foobis
    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar10);
    bar.addRelease(bar11);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, foobis, bar));
    pluginReferential.addOutgoingDependency(foobis10, "foo", "1.0");
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.1");
    pluginReferential.addOutgoingDependency(bar10, "foobis", "1.0");
    pluginReferential.addOutgoingDependency(bar11, "foobis", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
        pluginReferential, sonar)
        .setInstalledSonarVersion(Version.create("2.1"))
        .registerInstalledPlugins(PluginReferential.create(newArrayList(
            (Plugin) new Plugin("foobis").addRelease("1.0").getArtifact(),
            (Plugin) new Plugin("bar").addRelease("1.0").getArtifact()
        )));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("foobis", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(3);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void should_return_incoming_not_already_installed_dependencies_to_download() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = new Plugin("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.0.jar");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis10);
    foobis.addRelease(foobis11);

    // bar has one child and depends upon foobis
    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar10);
    bar.addRelease(bar11);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, foobis, bar));
    pluginReferential.addOutgoingDependency(foobis10, "foo", "1.0");
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.1");
    pluginReferential.addOutgoingDependency(bar10, "foobis", "1.0");
    pluginReferential.addOutgoingDependency(bar11, "foobis", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
        pluginReferential, sonar)
        .setInstalledSonarVersion(Version.create("2.1"))
        .registerInstalledPlugins(PluginReferential.create(newArrayList(
            (Plugin) new Plugin("foo").addRelease("1.0").getArtifact(),
            (Plugin) new Plugin("foobis").addRelease("1.0").getArtifact()
        )));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("foo", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(2);
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNull();
  }

  @Test
  public void find_sonar_updates() {
    sonar.addRelease(Version.create("2.3"));
    sonar.addRelease(Version.create("2.4"));

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonar).setInstalledSonarVersion(Version.create("2.2"));

    List<SonarUpdate> updates = updateCenter.findSonarUpdates();

    // no plugins are installed, so both sonar versions are compatible
    assertThat(updates).hasSize(2);
    assertThat(updates.get(0).hasWarnings()).isFalse();
    assertThat(updates.get(1).hasWarnings()).isFalse();
  }

  @Test
  public void warnings_on_sonar_updates() {
    sonar.addRelease(Version.create("2.3"));
    sonar.addRelease(Version.create("2.4"));

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonar)
        .setInstalledSonarVersion(Version.create("2.2"))
        .registerInstalledPlugins(
            PluginReferential.create(newArrayList((Plugin) new Plugin("foo").addRelease("1.0").getArtifact(), (Plugin) new Plugin("bar").addRelease("1.0").getArtifact())));
    List<SonarUpdate> updates = updateCenter.findSonarUpdates();

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