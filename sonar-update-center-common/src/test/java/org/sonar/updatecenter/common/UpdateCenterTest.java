/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2021 SonarSource SA
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
import javax.annotation.CheckForNull;
import org.junit.Before;
import org.junit.Test;
import org.sonar.updatecenter.common.exception.IncompatiblePluginVersionException;
import org.sonar.updatecenter.common.exception.PluginNotFoundException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

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
    foo = Plugin.factory("foo");
    foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1", "2.2").setDownloadUrl("http://server/foo-1.0.jar");
    foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1", "2.2", "2.3").setDownloadUrl("http://server/foo-1.1.jar");
    foo12 = new Release(foo, "1.2").addRequiredSonarVersions("2.3").setDownloadUrl("http://server/foo-1.2.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);
    foo.addRelease(foo12);

    bar = Plugin.factory("bar");
    bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1", "2.2").setDownloadUrl("http://server/bar-1.0.jar");
    bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.2.2", "2.3").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar10);
    bar.addRelease(bar11);

    sonar = new Sonar();
    pluginReferential = PluginReferential.create(asList(foo, bar));
  }

  @Test
  public void find_scanner_correctly() {
    ArrayList<Scanner> scanners = new ArrayList<>();
    UpdateCenter center = UpdateCenter.create(pluginReferential, scanners, sonar);
    assertThat(center.getScanners()).isEqualTo(scanners);
  }

  @Test
  public void find_plugin_installed() {
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
      PluginReferential.create(asList((Plugin) Plugin.factory("foo").addRelease("1.0").getArtifact())));

    PluginReferential installed = updateCenter.getInstalledPluginReferential();
    assertThat(installed.getLastMasterReleasePlugins()).hasSize(1);
    assertThat(installed.getLastMasterReleasePlugins().get(0).getKey()).isEqualTo("foo");
  }

  @Test
  public void find_plugin_updates() {
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
      PluginReferential.create(asList((Plugin) Plugin.factory("foo").addRelease("1.0").getArtifact())));

    List<PluginUpdate> updates = updateCenter.findPluginUpdates();
    assertThat(updates).hasSize(2);

    assertThat(updates.get(0).getRelease()).isEqualTo(foo11);
    assertThat(updates.get(0).isCompatible()).isTrue();
    assertThat(updates.get(1).getRelease()).isEqualTo(foo12);
    assertThat(updates.get(1).isCompatible()).isFalse();
    assertThat(updates.get(1).requiresSonarUpgrade()).isTrue();
  }

  @Test
  public void find_plugin_updates_with_sonar_upgrade() {
    Plugin foo = Plugin.factory("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    PluginReferential pluginReferential = PluginReferential.create(asList(foo));
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
      PluginReferential.create(asList((Plugin) Plugin.factory("foo").addRelease("1.0").getArtifact())));

    List<PluginUpdate> updates = updateCenter.findPluginUpdates();
    assertThat(updates).hasSize(1);
    assertThat(updates.get(0).getRelease()).isEqualTo(foo11);
    assertThat(updates.get(0).isCompatible()).isTrue();
  }

  @Test
  public void find_plugin_updates_with_dependencies() {
    Plugin test = Plugin.factory("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/test-1.0.jar");
    Release test11 = new Release(test, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/test-1.1.jar");
    test.addRelease(test10);
    test.addRelease(test11);

    Plugin foo = Plugin.factory("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    PluginReferential pluginReferential = PluginReferential.create(asList(foo, test));
    pluginReferential.addOutgoingDependency(foo11, "test", "1.1");

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
      PluginReferential.create(asList((Plugin) Plugin.factory("foo").addRelease("1.0").getArtifact())));

    List<PluginUpdate> updates = updateCenter.findPluginUpdates();
    assertThat(updates).hasSize(1);
    assertThat(updates.get(0).getRelease()).isEqualTo(foo11);
    assertThat(updates.get(0).getDependencies()).hasSize(1);
  }

  @Test
  public void find_plugin_updates_with_dependencies_needed_sonar_upgrade() {
    Plugin test = Plugin.factory("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/test-1.0.jar");
    Release test11 = new Release(test, "1.1").addRequiredSonarVersions("2.2").setDownloadUrl("http://server/test-1.1.jar");
    test.addRelease(test10);
    test.addRelease(test11);

    Plugin foo = Plugin.factory("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    PluginReferential pluginReferential = PluginReferential.create(asList(foo, test));
    pluginReferential.addOutgoingDependency(foo11, "test", "1.1");

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
      PluginReferential.create(asList((Plugin) Plugin.factory("foo").addRelease("1.0").getArtifact())));

    List<PluginUpdate> updates = updateCenter.findPluginUpdates();
    assertThat(updates).hasSize(1);
    assertThat(updates.get(0).getRelease()).isEqualTo(foo11);
    assertThat(updates.get(0).requiresSonarUpgradeForDependencies()).isTrue();
    assertThat(updates.get(0).getDependencies()).isEmpty();
  }

  @Test
  public void no_plugin_updates_if_last_release_is_installed() {
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.3")).registerInstalledPlugins(
      PluginReferential.create(asList((Plugin) Plugin.factory("foo").addRelease("1.2").getArtifact())));
    assertThat(updateCenter.findPluginUpdates()).isEmpty();
  }

  @Test
  public void available_plugins_are_only_the_best_releases() {
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(),  sonar).setInstalledSonarVersion(Version.create("2.2")).registerInstalledPlugins(
      PluginReferential.create(asList((Plugin) Plugin.factory("foo").addRelease("1.0").getArtifact())));

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
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.2.1")).registerInstalledPlugins(
      PluginReferential.create(asList((Plugin) Plugin.factory("foo").addRelease("1.0").getArtifact())));

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
    Plugin test = Plugin.factory("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/test-1.0.jar");
    Release test11 = new Release(test, "1.1").addRequiredSonarVersions("2.2").setDownloadUrl("http://server/test-1.1.jar");
    test.addRelease(test10);
    test.addRelease(test11);

    Plugin foo = Plugin.factory("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    foo.addRelease(foo10);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    PluginReferential pluginReferential = PluginReferential.create(asList(foo, test));
    pluginReferential.addOutgoingDependency(foo10, "test", "1.1");

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
      PluginReferential.create(asList((Plugin) Plugin.factory("test").addRelease("1.0").getArtifact())));

    List<PluginUpdate> availables = updateCenter.findAvailablePlugins();
    assertThat(availables).hasSize(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(foo10);
    assertThat(availables.get(0).requiresSonarUpgradeForDependencies()).isTrue();
    assertThat(availables.get(0).getDependencies()).hasSize(0);
  }

  @Test
  public void return_latest_releases_to_download() {
    Plugin bar = Plugin.factory("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar10);
    bar.addRelease(bar11);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
      PluginReferential.create(asList(bar)), new ArrayList<>(), sonar)
      .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.0"));
    assertThat(installablePlugins).hasSize(1);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void return_releases_compatible_with_installed_sonar_version_to_download() {
    Plugin bar = Plugin.factory("bar");
    // bar 1.0 est not compatible with sonar 2.1
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.0").setDownloadUrl("http://server/bar-1.0.jar");
    bar.addRelease(bar10);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
      PluginReferential.create(asList(bar)), new ArrayList<>(), sonar)
      .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.0"));
    assertThat(installablePlugins).hasSize(0);
  }

  @Test
  public void return_releases_to_download() {
    Plugin bar = Plugin.factory("bar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar11);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
      PluginReferential.create(asList(bar)), new ArrayList<>(), sonar)
      .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(1);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
  }

  @Test(expected = PluginNotFoundException.class)
  public void throw_exception_if_plugin_to_download_not_found() {
    Plugin bar = Plugin.factory("bar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar11);

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
      PluginReferential.create(asList(bar)), new ArrayList<>(), sonar)
      .setInstalledSonarVersion(Version.create("2.1"));

    updateCenter.findInstallablePlugins("not_found", Version.create("1.1"));
  }

  @Test
  public void return_release_dependencies_to_download() {
    Plugin foo = Plugin.factory("foo");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis11);

    PluginReferential pluginReferential = PluginReferential.create(asList(foo, foobis));
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("foobis", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(2);
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();
  }

  @Test(expected = IncompatiblePluginVersionException.class)
  public void throw_exception_if_dependency_not_found() {
    Plugin foo = Plugin.factory("foo");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis11);

    PluginReferential pluginReferential = PluginReferential.create(asList(foo, foobis));
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.2");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.1"));

    updateCenter.findInstallablePlugins("foobis", Version.create("1.1"));
  }

  @Test
  public void return_release_dependencies_not_already_downloaded_to_download() {
    Plugin foo = Plugin.factory("foo");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis11);

    PluginReferential pluginReferential = PluginReferential.create(asList(foo, foobis));
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
      pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
        PluginReferential.create(asList(
          (Plugin) Plugin.factory("foo").addRelease("1.1").getArtifact())));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("foobis", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(1);
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNull();
  }

  @Test
  public void not_return_incompatible_releases_to_download() {
    Plugin foo = Plugin.factory("foo");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    Release foo12 = new Release(foo, "1.2").addRequiredSonarVersions("2.2").setDownloadUrl("http://server/foo-1.2.jar");
    foo.addRelease(foo11);
    foo.addRelease(foo12);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    // foobis 1.2 should not to be downloaded
    Release foobis12 = new Release(foobis, "1.2").addRequiredSonarVersions("2.2").setDownloadUrl("http://server/foobis-1.2.jar");
    foobis.addRelease(foobis11);
    foobis.addRelease(foobis12);

    // bar depends upon foobis
    Plugin bar = Plugin.factory("bar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar11);

    PluginReferential pluginReferential = PluginReferential.create(asList(foo, foobis, bar));
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.1");
    pluginReferential.addOutgoingDependency(foobis12, "foo", "1.1");
    pluginReferential.addOutgoingDependency(bar11, "foobis", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
      pluginReferential, new ArrayList<>(), sonar)
      .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(3);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void return_latest_compatible_releases_with_sonar_version_to_download() {
    Plugin foobis = Plugin.factory("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    Release foobis12 = new Release(foobis, "1.2").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.2.jar");
    foobis.addRelease(foobis11);
    foobis.addRelease(foobis12);

    // bar depends upon foobis 1.1
    Plugin bar = Plugin.factory("bar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar11);

    PluginReferential pluginReferential = PluginReferential.create(asList(foobis, bar));
    pluginReferential.addOutgoingDependency(bar11, "foobis", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
      pluginReferential, new ArrayList<>(), sonar)
      .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(2);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    // foobis 1.2 is compatible with sonar 2.1, so it's this version that will be downloaded
    assertThat(getRelease("foobis", "1.2", installablePlugins)).isNotNull();
  }

  @Test
  public void return_outcoming_and_incoming_dependencies_to_download() {
    Plugin foo = Plugin.factory("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.0.jar");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis10);
    foobis.addRelease(foobis11);

    // bar depends upon foobis
    Plugin bar = Plugin.factory("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar10);
    bar.addRelease(bar11);

    PluginReferential pluginReferential = PluginReferential.create(asList(foo, foobis, bar));
    pluginReferential.addOutgoingDependency(foobis10, "foo", "1.0");
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.1");
    pluginReferential.addOutgoingDependency(bar10, "foobis", "1.0");
    pluginReferential.addOutgoingDependency(bar11, "foobis", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
      pluginReferential, new ArrayList<>(), sonar)
      .setInstalledSonarVersion(Version.create("2.1"))
      .registerInstalledPlugins(PluginReferential.create(asList(
        (Plugin) Plugin.factory("foobis").addRelease("1.0").getArtifact(),
        (Plugin) Plugin.factory("bar").addRelease("1.0").getArtifact())));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("foobis", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(3);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void return_two_levels_of_outcoming_dependencies_to_download() {
    Plugin foo = Plugin.factory("foo");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis11);

    // bar depends upon foobis
    Plugin bar = Plugin.factory("bar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar11);

    PluginReferential pluginReferential = PluginReferential.create(asList(foo, foobis, bar));
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.1");
    pluginReferential.addOutgoingDependency(bar11, "foobis", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar)
      .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(3);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void return_dependencies_to_download_with_some_installed_plugins_to_latest_version() {
    Plugin foo = Plugin.factory("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.0.jar");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis10);
    foobis.addRelease(foobis11);

    // bar depends upon foobis
    Plugin bar = Plugin.factory("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar10);
    bar.addRelease(bar11);

    PluginReferential pluginReferential = PluginReferential.create(asList(foo, foobis, bar));
    pluginReferential.addOutgoingDependency(foobis10, "foo", "1.0");
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.1");
    pluginReferential.addOutgoingDependency(bar10, "foobis", "1.0");
    pluginReferential.addOutgoingDependency(bar11, "foobis", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
      pluginReferential, new ArrayList<>(), sonar)
      .setInstalledSonarVersion(Version.create("2.1"))
      .registerInstalledPlugins(PluginReferential.create(asList(
        (Plugin) Plugin.factory("foo").addRelease("1.1").getArtifact(),
        (Plugin) Plugin.factory("foobis").addRelease("1.1").getArtifact())));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(1);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void return_incoming_not_already_installed_dependencies_to_download() {
    Plugin foo = Plugin.factory("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11 = new Release(foo, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foo-1.1.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.0.jar");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis10);
    foobis.addRelease(foobis11);

    // bar depends upon foobis
    Plugin bar = Plugin.factory("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.0.jar");
    Release bar11 = new Release(bar, "1.1").addRequiredSonarVersions("2.1").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar10);
    bar.addRelease(bar11);

    PluginReferential pluginReferential = PluginReferential.create(asList(foo, foobis, bar));
    pluginReferential.addOutgoingDependency(foobis10, "foo", "1.0");
    pluginReferential.addOutgoingDependency(foobis11, "foo", "1.1");
    pluginReferential.addOutgoingDependency(bar10, "foobis", "1.0");
    pluginReferential.addOutgoingDependency(bar11, "foobis", "1.1");

    Sonar sonar = (Sonar) new Sonar().addRelease("2.1").getArtifact();
    UpdateCenter updateCenter = UpdateCenter.create(
      pluginReferential, new ArrayList<>(), sonar)
      .setInstalledSonarVersion(Version.create("2.1"))
      .registerInstalledPlugins(PluginReferential.create(asList(
        (Plugin) Plugin.factory("foo").addRelease("1.0").getArtifact(),
        (Plugin) Plugin.factory("foobis").addRelease("1.0").getArtifact())));

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

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.2"));

    List<SonarUpdate> updates = updateCenter.findSonarUpdates();

    // no plugins are installed, so both sonar versions are compatible
    assertThat(updates).hasSize(2);
    assertThat(updates.get(0).hasWarnings()).isFalse();
    assertThat(updates.get(1).hasWarnings()).isFalse();
  }

  @Test
  public void find_no_plugin_to_upgrade_on_already_compatible_plugins_with_sonar_updates() {
    sonar.addRelease(Version.create("2.3"));

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar)
      .setInstalledSonarVersion(Version.create("2.2"))
      .registerInstalledPlugins(
        PluginReferential.create(asList((Plugin) Plugin.factory("foo").addRelease("1.2").getArtifact(), (Plugin) Plugin.factory("bar").addRelease("1.1").getArtifact())));
    List<SonarUpdate> updates = updateCenter.findSonarUpdates();

    // sonar 2.3 supports foo 1.2 and bar 1.1
    // => No plugin upgrade is required
    assertThat(updates.get(0).hasWarnings()).isFalse();
    assertThat(updates.get(0).requiresPluginUpgrades()).isFalse();
    assertThat(updates.get(0).getPluginsToUpgrade()).isEmpty();
    assertThat(updates.get(0).getIncompatiblePlugins()).isEmpty();
  }

  @Test
  public void warnings_on_sonar_updates() {
    sonar.addRelease(Version.create("2.3"));
    sonar.addRelease(Version.create("2.4"));

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar)
      .setInstalledSonarVersion(Version.create("2.2"))
      .registerInstalledPlugins(
        PluginReferential.create(asList((Plugin) Plugin.factory("foo").addRelease("1.0").getArtifact(), (Plugin) Plugin.factory("bar").addRelease("1.0").getArtifact())));
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

  @Test
  public void find_compatible_plugins() {
    List<Plugin> allCompatiblePlugins = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.1")).findAllCompatiblePlugins();

    assertThat(allCompatiblePlugins).containsOnly(foo, bar);

    allCompatiblePlugins = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar).setInstalledSonarVersion(Version.create("2.2.2")).findAllCompatiblePlugins();

    assertThat(allCompatiblePlugins).containsOnly(bar);
  }

  @Test
  public void find_oldest_compatible_for_given_sq_version() {
    assertThat(foo.getReleaseForSonarVersion("OLDEST_COMPATIBLE", Version.create("2.2"))).isEqualTo(foo10);
    assertThat(foo.getReleaseForSonarVersion("OLDEST_COMPATIBLE", Version.create("2.3"))).isEqualTo(foo11);
  }

  @CheckForNull
  public Release getRelease(final String key, final String version, List<Release> releases) {
    return releases.stream()
      .filter(rel -> rel.getArtifact().getKey().equals(key) && rel.getVersion().getName().equals(version))
      .findFirst()
      .orElse(null);
  }

}
