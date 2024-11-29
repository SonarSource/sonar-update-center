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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
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
  private Release foo13;
  private Release foo14;
  private Plugin bar;
  private Release bar10;
  private Release bar11;
  private Sonar sonar;

  @Before
  public void initCenter() {
    foo = Plugin.factory("foo");
    foo10 = new Release(foo, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1", "2.2").setDownloadUrl("http://server/foo-1.0.jar");
    foo11 = new Release(foo, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1", "2.2", "2.3").setDownloadUrl("http://server/foo-1.1.jar");
    foo12 = new Release(foo, "1.2").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.3").setDownloadUrl("http://server/foo-1.2.jar");
    foo13 = new Release(foo, "1.3").addRequiredSonarVersions(Product.SONARQUBE_SERVER, "2.4").setDownloadUrl("http://server/foo-1.3.jar");
    foo14 = new Release(foo, "1.4").addRequiredSonarVersions(Product.SONARQUBE_SERVER, "2.5").setDownloadUrl("http://server/foo-1.3.jar");
    foo.addRelease(foo10);
    foo.addRelease(foo11);
    foo.addRelease(foo12);
    foo.addRelease(foo13);
    foo.addRelease(foo14);

    bar = Plugin.factory("bar");
    bar10 = new Release(bar, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1", "2.2").setDownloadUrl("http://server/bar-1.0.jar");
    bar11 = new Release(bar, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.2.2", "2.3").setDownloadUrl("http://server/bar-1.1.jar");
    bar.addRelease(bar10);
    bar.addRelease(bar11);

    sonar = new Sonar();
    pluginReferential = PluginReferential.create(asList(foo, bar));
  }

  @Test
  public void find_scanner_correctly() {
    ArrayList<Scanner> scanners = new ArrayList<>();
    UpdateCenter center = UpdateCenter.create(pluginReferential, scanners, sonar, Product.OLD_SONARQUBE);
    assertThat(center.getScanners()).isEqualTo(scanners);
  }

  @Test
  public void find_plugin_installed() {
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.OLD_SONARQUBE).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
      PluginReferential.create(asList(createPlugin("foo", "1.0"))));

    PluginReferential installed = updateCenter.getInstalledPluginReferential();
    assertThat(installed.getLastMasterReleasePlugins()).hasSize(1);
    assertThat(installed.getLastMasterReleasePlugins().get(0).getKey()).isEqualTo("foo");
  }

  @Test
  public void find_plugin_updates() {
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.OLD_SONARQUBE)
      .setInstalledSonarVersion(Version.create("2.1"))
      .registerInstalledPlugins(PluginReferential.create(asList(createPlugin("foo", "1.0"))));

    List<PluginUpdate> updates = updateCenter.findPluginUpdates();
    assertThat(updates).hasSize(4);

    assertThat(updates.get(0).getRelease()).isEqualTo(foo11);
    assertThat(updates.get(0).isCompatible()).isTrue();
    assertThat(updates.get(1).getRelease()).isEqualTo(foo12);
    assertThat(updates.get(1).isCompatible()).isFalse();
    assertThat(updates.get(1).requiresSonarUpgrade()).isTrue();
    assertThat(updates.get(2).isCompatible()).isFalse();
    assertThat(updates.get(2).requiresSonarUpgrade()).isFalse();
    assertThat(updates.get(3).isCompatible()).isFalse();
    assertThat(updates.get(3).requiresSonarUpgrade()).isFalse();
  }

  @Test
  public void find_plugin_updates_for_sonarqube_server() {
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.SONARQUBE_SERVER)
      .setInstalledSonarVersion(Version.create("2.3"))
      .registerInstalledPlugins(PluginReferential.create(asList(createPlugin("foo", "1.3"))));

    List<PluginUpdate> updates = updateCenter.findPluginUpdates();
    assertThat(updates).hasSize(1);

    assertThat(updates.get(0).getRelease()).isEqualTo(foo14);
    assertThat(updates.get(0).isCompatible()).isFalse();
  }

  @Test
  public void find_plugin_updates_with_sonar_upgrade() {
    Plugin fooLocal = Plugin.factory("foo");
    Release foo10Local = new Release(fooLocal, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11Local = new Release(fooLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.1.jar");
    fooLocal.addRelease(foo10Local);
    fooLocal.addRelease(foo11Local);

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(fooLocal));
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
      PluginReferential.create(asList(createPlugin("foo", "1.0"))));

    List<PluginUpdate> updates = updateCenter.findPluginUpdates();
    assertThat(updates).hasSize(1);
    assertThat(updates.get(0).getRelease()).isEqualTo(foo11Local);
    assertThat(updates.get(0).isCompatible()).isTrue();
  }

  @Test
  public void find_plugin_updates_with_sonar_upgrade_for_community_build() {
    Plugin fooLocal = Plugin.factory("foo");
    Release foo10Local = new Release(fooLocal, "1.0").addRequiredSonarVersions(Product.SONARQUBE_COMMUNITY_BUILD, "39.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11Local = new Release(fooLocal, "1.1").addRequiredSonarVersions(Product.SONARQUBE_COMMUNITY_BUILD, "39.1").setDownloadUrl("http://server/foo-1.1.jar");
    fooLocal.addRelease(foo10Local);
    fooLocal.addRelease(foo11Local);

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("39.1", sonarLocal, Product.SONARQUBE_COMMUNITY_BUILD);
    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(fooLocal));
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.SONARQUBE_COMMUNITY_BUILD)
      .setInstalledSonarVersion(Version.create("39.1"))
      .registerInstalledPlugins(PluginReferential.create(asList(createPlugin("foo", "1.0"))));

    List<PluginUpdate> updates = updateCenter.findPluginUpdates();
    assertThat(updates).hasSize(1);
    assertThat(updates.get(0).getRelease()).isEqualTo(foo11Local);
    assertThat(updates.get(0).isCompatible()).isTrue();
  }

  @Test
  public void find_plugin_updates_with_dependencies() {
    Plugin test = Plugin.factory("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/test-1.0.jar");
    Release test11 = new Release(test, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/test-1.1.jar");
    test.addRelease(test10);
    test.addRelease(test11);

    Plugin fooLocal = Plugin.factory("foo");
    Release foo10Local = new Release(fooLocal, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11Local = new Release(fooLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.1.jar");
    fooLocal.addRelease(foo10Local);
    fooLocal.addRelease(foo11Local);

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(fooLocal, test));
    pluginReferentialLocal.addOutgoingDependency(foo11Local, "test", "1.1");

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
      PluginReferential.create(asList(createPlugin("foo", "1.0"))));

    List<PluginUpdate> updates = updateCenter.findPluginUpdates();
    assertThat(updates).hasSize(1);
    assertThat(updates.get(0).getRelease()).isEqualTo(foo11Local);
    assertThat(updates.get(0).getDependencies()).hasSize(1);
  }

  @Test
  public void find_plugin_updates_with_dependencies_needed_sonar_upgrade() {
    Plugin test = Plugin.factory("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/test-1.0.jar");
    Release test11 = new Release(test, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.2").setDownloadUrl("http://server/test-1.1.jar");
    test.addRelease(test10);
    test.addRelease(test11);

    Plugin fooLocal = Plugin.factory("foo");
    Release foo10Local = new Release(fooLocal, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11Local = new Release(fooLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.1.jar");
    fooLocal.addRelease(foo10Local);
    fooLocal.addRelease(foo11Local);

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(fooLocal, test));
    pluginReferentialLocal.addOutgoingDependency(foo11Local, "test", "1.1");

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
      PluginReferential.create(asList(createPlugin("foo", "1.0"))));

    List<PluginUpdate> updates = updateCenter.findPluginUpdates();
    assertThat(updates).hasSize(1);
    assertThat(updates.get(0).getRelease()).isEqualTo(foo11Local);
    assertThat(updates.get(0).requiresSonarUpgradeForDependencies()).isTrue();
    assertThat(updates.get(0).getDependencies()).isEmpty();
  }

  @Test
  public void no_plugin_updates_if_last_release_is_installed() {
    PluginReferential fooPlugin = PluginReferential.create(asList(createPlugin("foo", "1.4")));
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.SONARQUBE_SERVER)
      .setInstalledSonarVersion(Version.create("2.5"))
      .registerInstalledPlugins(fooPlugin);

    assertThat(updateCenter.findPluginUpdates()).isEmpty();
  }

  @Test
  public void available_plugins_are_only_the_best_releases() {
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(),  sonar, Product.OLD_SONARQUBE).setInstalledSonarVersion(Version.create("2.2")).registerInstalledPlugins(
      PluginReferential.create(asList(createPlugin("foo", "1.0"))));

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
    PluginReferential pluginRef = PluginReferential.create(asList(createPlugin("foo", "1.0")));
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.OLD_SONARQUBE)
      .setInstalledSonarVersion(Version.create("2.2.1")).registerInstalledPlugins(pluginRef);

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
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/test-1.0.jar");
    Release test11 = new Release(test, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.2").setDownloadUrl("http://server/test-1.1.jar");
    test.addRelease(test10);
    test.addRelease(test11);

    Plugin fooLocal = Plugin.factory("foo");
    Release foo10Local = new Release(fooLocal, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.0.jar");
    fooLocal.addRelease(foo10Local);

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(fooLocal, test));
    pluginReferentialLocal.addOutgoingDependency(foo10Local, "test", "1.1");

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
      PluginReferential.create(asList(createPlugin("test", "1.0"))));

    List<PluginUpdate> availables = updateCenter.findAvailablePlugins();
    assertThat(availables).hasSize(1);
    assertThat(availables.get(0).getRelease()).isEqualTo(foo10Local);
    assertThat(availables.get(0).requiresSonarUpgradeForDependencies()).isTrue();
    assertThat(availables.get(0).getDependencies()).isEmpty();
  }

  @Test
  public void return_latest_releases_to_download() {
    Plugin barLocal = Plugin.factory("bar");
    Release bar10Local = new Release(barLocal, "1.0").addRequiredSonarVersions(Product.SONARQUBE_SERVER, "2.1").setDownloadUrl("http://server/bar-1.0.jar");
    Release bar11Local = new Release(barLocal, "1.1").addRequiredSonarVersions(Product.SONARQUBE_SERVER, "2.1").setDownloadUrl("http://server/bar-1.1.jar");
    barLocal.addRelease(bar10Local);
    barLocal.addRelease(bar11Local);

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    UpdateCenter updateCenter = UpdateCenter.create(
      PluginReferential.create(asList(barLocal)), new ArrayList<>(), sonarLocal, Product.SONARQUBE_SERVER)
      .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.0"));
    assertThat(installablePlugins).hasSize(1);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void return_releases_compatible_with_installed_sonar_version_to_download() {
    Plugin barLocal = Plugin.factory("bar");
    // bar 1.0 est not compatible with sonar 2.1
    Release bar10Local = new Release(barLocal, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.0").setDownloadUrl("http://server/bar-1.0.jar");
    barLocal.addRelease(bar10Local);

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    UpdateCenter updateCenter = UpdateCenter.create(
      PluginReferential.create(asList(barLocal)), new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE)
      .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.0"));
    assertThat(installablePlugins).isEmpty();
  }

  @Test
  public void return_releases_to_download() {
    Plugin barLocal = Plugin.factory("bar");
    Release bar11Local = new Release(barLocal, "1.1").addRequiredSonarVersions(Product.SONARQUBE_COMMUNITY_BUILD, "2.1").setDownloadUrl("http://server/bar-1.1.jar");
    barLocal.addRelease(bar11Local);

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    UpdateCenter updateCenter = UpdateCenter.create(
      PluginReferential.create(asList(barLocal)), new ArrayList<>(), sonarLocal, Product.SONARQUBE_COMMUNITY_BUILD)
      .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(1);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
  }

  @Test(expected = PluginNotFoundException.class)
  public void throw_exception_if_plugin_to_download_not_found() {
    Plugin barLocal = Plugin.factory("bar");
    Release bar11Local = new Release(barLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/bar-1.1.jar");
    barLocal.addRelease(bar11Local);

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    UpdateCenter updateCenter = UpdateCenter.create(
      PluginReferential.create(asList(barLocal)), new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE)
      .setInstalledSonarVersion(Version.create("2.1"));

    updateCenter.findInstallablePlugins("not_found", Version.create("1.1"));
  }

  @Test
  public void return_release_dependencies_to_download() {
    Plugin fooLocal = Plugin.factory("foo");
    Release foo11Local = new Release(fooLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.1.jar");
    fooLocal.addRelease(foo11Local);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis11);

    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(fooLocal, foobis));
    pluginReferentialLocal.addOutgoingDependency(foobis11, "foo", "1.1");

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE).setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("foobis", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(2);
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();
  }

  @Test(expected = IncompatiblePluginVersionException.class)
  public void throw_exception_if_dependency_not_found() {
    Plugin fooLocal = Plugin.factory("foo");
    Release foo11Local = new Release(fooLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.1.jar");
    fooLocal.addRelease(foo11Local);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis11);

    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(fooLocal, foobis));
    pluginReferentialLocal.addOutgoingDependency(foobis11, "foo", "1.2");

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE).setInstalledSonarVersion(Version.create("2.1"));

    updateCenter.findInstallablePlugins("foobis", Version.create("1.1"));
  }

  @Test
  public void return_release_dependencies_not_already_downloaded_to_download() {
    Plugin fooLocal = Plugin.factory("foo");
    Release foo11Local = new Release(fooLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.1.jar");
    fooLocal.addRelease(foo11Local);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis11);

    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(fooLocal, foobis));
    pluginReferentialLocal.addOutgoingDependency(foobis11, "foo", "1.1");

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    UpdateCenter updateCenter = UpdateCenter.create(
      pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE).setInstalledSonarVersion(Version.create("2.1")).registerInstalledPlugins(
        PluginReferential.create(asList(createPlugin("foo", "1.1"))));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("foobis", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(1);
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNull();
  }

  @Test
  public void not_return_incompatible_releases_to_download() {
    Plugin fooLocal = Plugin.factory("foo");
    Release foo11Local = new Release(fooLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.1.jar");
    Release foo12Local = new Release(fooLocal, "1.2").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.2").setDownloadUrl("http://server/foo-1.2.jar");
    Release foo103 = new Release(fooLocal, "103").addRequiredSonarVersions(Product.SONARQUBE_SERVER, "2.4").setDownloadUrl("http://server/foo-103.jar");
    fooLocal.addRelease(foo11Local);
    fooLocal.addRelease(foo12Local);
    fooLocal.addRelease(foo103);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    // foobis 1.2 should not to be downloaded
    Release foobis12 = new Release(foobis, "1.2").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.2").setDownloadUrl("http://server/foobis-1.2.jar");
    foobis.addRelease(foobis11);
    foobis.addRelease(foobis12);

    // bar depends upon foobis
    Plugin barLocal = Plugin.factory("bar");
    Release bar11Local = new Release(barLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/bar-1.1.jar");
    barLocal.addRelease(bar11Local);

    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(fooLocal, foobis, barLocal));
    pluginReferentialLocal.addOutgoingDependency(foobis11, "foo", "1.1");
    pluginReferentialLocal.addOutgoingDependency(foobis11, "foo", "103");
    pluginReferentialLocal.addOutgoingDependency(foobis12, "foo", "1.1");
    pluginReferentialLocal.addOutgoingDependency(bar11Local, "foobis", "1.1");

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    addReleaseToSonarObject("2.3", sonarLocal, Product.SONARQUBE_SERVER);
    UpdateCenter updateCenter = UpdateCenter.create(
      pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE)
      .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(3);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();


    updateCenter = UpdateCenter.create(pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.SONARQUBE_SERVER).setInstalledSonarVersion(Version.create("2.3"));

    installablePlugins = updateCenter.findInstallablePlugins("foo", Version.create("1.1"));
    assertThat(installablePlugins).isEmpty();
  }

  @Test
  public void return_latest_compatible_releases_with_sonar_version_to_download() {
    Plugin foobis = Plugin.factory("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    Release foobis12 = new Release(foobis, "1.2").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foobis-1.2.jar");
    foobis.addRelease(foobis11);
    foobis.addRelease(foobis12);

    // bar depends upon foobis 1.1
    Plugin barLocal = Plugin.factory("bar");
    Release bar11Local = new Release(barLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/bar-1.1.jar");
    barLocal.addRelease(bar11Local);

    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(foobis, barLocal));
    pluginReferentialLocal.addOutgoingDependency(bar11Local, "foobis", "1.1");

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    UpdateCenter updateCenter = UpdateCenter.create(
      pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE)
      .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(2);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    // foobis 1.2 is compatible with sonar 2.1, so it's this version that will be downloaded
    assertThat(getRelease("foobis", "1.2", installablePlugins)).isNotNull();
  }

  @Test
  public void return_outcoming_and_incoming_dependencies_to_download() {
    Plugin fooLocal = Plugin.factory("foo");
    Release foo10Local = new Release(fooLocal, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11Local = new Release(fooLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.1.jar");
    fooLocal.addRelease(foo10Local);
    fooLocal.addRelease(foo11Local);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foobis-1.0.jar");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis10);
    foobis.addRelease(foobis11);

    // bar depends upon foobis
    Plugin barLocal = Plugin.factory("bar");
    Release bar10Local = new Release(barLocal, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/bar-1.0.jar");
    Release bar11Local = new Release(barLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/bar-1.1.jar");
    barLocal.addRelease(bar10Local);
    barLocal.addRelease(bar11Local);

    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(fooLocal, foobis, barLocal));
    pluginReferentialLocal.addOutgoingDependency(foobis10, "foo", "1.0");
    pluginReferentialLocal.addOutgoingDependency(foobis11, "foo", "1.1");
    pluginReferentialLocal.addOutgoingDependency(bar10Local, "foobis", "1.0");
    pluginReferentialLocal.addOutgoingDependency(bar11Local, "foobis", "1.1");

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    UpdateCenter updateCenter = UpdateCenter.create(
      pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE)
      .setInstalledSonarVersion(Version.create("2.1"))
      .registerInstalledPlugins(PluginReferential.create(asList(createPlugin("foobis", "1.0"), createPlugin("bar", "1.0"))));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("foobis", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(3);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void return_two_levels_of_outcoming_dependencies_to_download() {
    Plugin fooLocal = Plugin.factory("foo");
    Release foo11Local = new Release(fooLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.1.jar");
    fooLocal.addRelease(foo11Local);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis11);

    // bar depends upon foobis
    Plugin barLocal = Plugin.factory("bar");
    Release bar11Local = new Release(barLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/bar-1.1.jar");
    barLocal.addRelease(bar11Local);

    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(fooLocal, foobis, barLocal));
    pluginReferentialLocal.addOutgoingDependency(foobis11, "foo", "1.1");
    pluginReferentialLocal.addOutgoingDependency(bar11Local, "foobis", "1.1");

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE)
      .setInstalledSonarVersion(Version.create("2.1"));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(3);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void return_dependencies_to_download_with_some_installed_plugins_to_latest_version() {
    Plugin fooLocal = Plugin.factory("foo");
    Release foo10Local = new Release(fooLocal, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11Local = new Release(fooLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.1.jar");
    fooLocal.addRelease(foo10Local);
    fooLocal.addRelease(foo11Local);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foobis-1.0.jar");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis10);
    foobis.addRelease(foobis11);

    // bar depends upon foobis
    Plugin barLocal = Plugin.factory("bar");
    Release bar10Local = new Release(barLocal, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/bar-1.0.jar");
    Release bar11Local = new Release(barLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/bar-1.1.jar");
    barLocal.addRelease(bar10Local);
    barLocal.addRelease(bar11Local);

    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(fooLocal, foobis, barLocal));
    pluginReferentialLocal.addOutgoingDependency(foobis10, "foo", "1.0");
    pluginReferentialLocal.addOutgoingDependency(foobis11, "foo", "1.1");
    pluginReferentialLocal.addOutgoingDependency(bar10Local, "foobis", "1.0");
    pluginReferentialLocal.addOutgoingDependency(bar11Local, "foobis", "1.1");

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE)
      .setInstalledSonarVersion(Version.create("2.1"))
      .registerInstalledPlugins(PluginReferential.create(asList(
        createPlugin("foo", "1.1"),
        createPlugin("foobis", "1.1"))));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("bar", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(1);
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNotNull();
  }

  @Test
  public void return_incoming_not_already_installed_dependencies_to_download() {
    Plugin fooLocal = Plugin.factory("foo");
    Release foo10Local = new Release(fooLocal, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.0.jar");
    Release foo11Local = new Release(fooLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foo-1.1.jar");
    fooLocal.addRelease(foo10Local);
    fooLocal.addRelease(foo11Local);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foobis-1.0.jar");
    Release foobis11 = new Release(foobis, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/foobis-1.1.jar");
    foobis.addRelease(foobis10);
    foobis.addRelease(foobis11);

    // bar depends upon foobis
    Plugin barLocal = Plugin.factory("bar");
    Release bar10Local = new Release(barLocal, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/bar-1.0.jar");
    Release bar11Local = new Release(barLocal, "1.1").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1").setDownloadUrl("http://server/bar-1.1.jar");
    barLocal.addRelease(bar10Local);
    barLocal.addRelease(bar11Local);

    PluginReferential pluginReferentialLocal = PluginReferential.create(asList(fooLocal, foobis, barLocal));
    pluginReferentialLocal.addOutgoingDependency(foobis10, "foo", "1.0");
    pluginReferentialLocal.addOutgoingDependency(foobis11, "foo", "1.1");
    pluginReferentialLocal.addOutgoingDependency(bar10Local, "foobis", "1.0");
    pluginReferentialLocal.addOutgoingDependency(bar11Local, "foobis", "1.1");

    Sonar sonarLocal = new Sonar();
    addReleaseToSonarObject("2.1", sonarLocal);
    UpdateCenter updateCenter = UpdateCenter.create(
        pluginReferentialLocal, new ArrayList<>(), sonarLocal, Product.OLD_SONARQUBE)
      .setInstalledSonarVersion(Version.create("2.1"))
      .registerInstalledPlugins(PluginReferential.create(asList(createPlugin("foo", "1.0"), createPlugin("foobis", "1.0"))));

    List<Release> installablePlugins = updateCenter.findInstallablePlugins("foo", Version.create("1.1"));
    assertThat(installablePlugins).hasSize(2);
    assertThat(getRelease("foo", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("foobis", "1.1", installablePlugins)).isNotNull();
    assertThat(getRelease("bar", "1.1", installablePlugins)).isNull();
  }

  @Test
  public void find_sonar_updates() {
    addReleaseToSonarObject("2.3", sonar);
    addReleaseToSonarObject("2.4", sonar);

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.OLD_SONARQUBE).setInstalledSonarVersion(Version.create("2.2"));

    List<SonarUpdate> updates = updateCenter.findSonarUpdates();

    // no plugins are installed, so both sonar versions are compatible
    assertThat(updates).hasSize(2);
    assertThat(updates.get(0).hasWarnings()).isFalse();
    assertThat(updates.get(1).hasWarnings()).isFalse();
  }

  @Test
  public void findSonarUpdatesForCommunityBuild_whenNoSonarQubeServerReleasesAndNewCommunityBuild_returnIt() {
    addReleaseToSonarObject("1", sonar, Product.SONARQUBE_COMMUNITY_BUILD);
    addReleaseToSonarObject("2", sonar, Product.SONARQUBE_COMMUNITY_BUILD);

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.SONARQUBE_COMMUNITY_BUILD).setInstalledSonarVersion(Version.create("1"));

    List<SonarUpdate> updates = updateCenter.findSonarUpdates();

    assertThat(updates).hasSize(1);
    assertThat(updates.get(0).getRelease().getVersion()).isEqualTo(Version.create("2"));
  }

  @Test
  public void findSonarUpdatesForCommunityBuild_whenOldSonarQubeServerAndNewCommunityBuild_returnIt() {
    addReleaseToSonarObject("1", sonar, Product.SONARQUBE_COMMUNITY_BUILD, "01-21");
    addReleaseToSonarObject("2", sonar, Product.SONARQUBE_COMMUNITY_BUILD, "01-22");
    addReleaseToSonarObject("10", sonar, Product.SONARQUBE_SERVER, "01-20");

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.SONARQUBE_COMMUNITY_BUILD).setInstalledSonarVersion(Version.create("1"));

    List<SonarUpdate> updates = updateCenter.findSonarUpdates();

    assertThat(updates).hasSize(1);
    assertThat(updates.get(0).getRelease().getVersion()).isEqualTo(Version.create("2"));
  }

  @Test
  public void findSonarUpdatesForCommunityBuild_whenSonarQubeServerAndCommunityReleasedOnTheSameDay_returnIt() {
    addReleaseToSonarObject("1", sonar, Product.SONARQUBE_COMMUNITY_BUILD, "01-21");
    addReleaseToSonarObject("10", sonar, Product.SONARQUBE_SERVER, "01-21");

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.SONARQUBE_COMMUNITY_BUILD)
      .setInstalledSonarVersion(Version.create("1"));

    List<SonarUpdate> updates = updateCenter.findSonarUpdates();

    assertThat(updates).hasSize(1);
    assertThat(updates.get(0).getRelease().getVersion()).isEqualTo(Version.create("10"));
  }

  @Test
  public void findSonarUpdatesForCommunityBuild_whenCommunityBuildNoLongerInTheUpdateCenter_returnLatestSonarQubeServer() {
    addReleaseToSonarObject("10", sonar, Product.SONARQUBE_SERVER, "01-21");

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.SONARQUBE_COMMUNITY_BUILD)
      .setInstalledSonarVersion(Version.create("1"));

    List<SonarUpdate> updates = updateCenter.findSonarUpdates();

    assertThat(updates).hasSize(1);
    assertThat(updates.get(0).getRelease().getVersion()).isEqualTo(Version.create("10"));
  }

  @Test
  public void findSonarUpdatesForCommunityBuild_whenSQSPatchVersionReleased_returnNothing() {
    addReleaseToSonarObject("10.0.1", sonar, Product.SONARQUBE_SERVER, "01-21");

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.SONARQUBE_COMMUNITY_BUILD)
      .setInstalledSonarVersion(Version.create("1"));

    List<SonarUpdate> updates = updateCenter.findSonarUpdates();

    assertThat(updates).isEmpty();
  }

  private void addReleaseToSonarObject(String version, Sonar sonar) {
    addReleaseToSonarObject(version, sonar, Product.OLD_SONARQUBE);
  }

  private void addReleaseToSonarObject(String version, Sonar sonar, Product product) {
    addReleaseToSonarObject(version, sonar, product, "01-01");
  }

  private void addReleaseToSonarObject(String version, Sonar sonar, Product product, String dayAndMonth) {
    LocalDate localDate = LocalDate.of(2025, Integer.parseInt(dayAndMonth.split("-")[0]), Integer.parseInt(dayAndMonth.split("-")[1]));
    Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

    Release release = new Release(sonar, Version.create(version));
    release.setDate(date);
    release.setProduct(product);
    sonar.addRelease(release);
  }

  @Test
  public void find_no_plugin_to_upgrade_on_already_compatible_plugins_with_sonar_updates() {
    Release sonarRelease = new Release(sonar, Version.create("2.3"));
    sonarRelease.setProduct(Product.OLD_SONARQUBE);
    sonar.addRelease(sonarRelease);

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.OLD_SONARQUBE)
      .setInstalledSonarVersion(Version.create("2.2"))
      .registerInstalledPlugins(
        PluginReferential.create(asList(createPlugin("foo", "1.2"), createPlugin("foo", "1.1"))));
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
    sonar.addRelease(new SonarRelease(sonar, Version.create("2.3"), Product.OLD_SONARQUBE));
    sonar.addRelease(new SonarRelease(sonar, Version.create("2.4"), Product.OLD_SONARQUBE));

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.OLD_SONARQUBE)
      .setInstalledSonarVersion(Version.create("2.2"))
      .registerInstalledPlugins(PluginReferential.create(asList(createPlugin("foo", "1.0"), createPlugin("bar", "1.0"))));
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

  private Plugin createPlugin(String name, String version) {
    Plugin plugin = Plugin.factory(name);
    plugin.addRelease(new Release(plugin, Version.create(version)));
    return plugin;
  }

  @Test
  public void find_compatible_plugins() {
    List<Plugin> allCompatiblePlugins = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.OLD_SONARQUBE).setInstalledSonarVersion(Version.create("2.1")).findAllCompatiblePlugins();

    assertThat(allCompatiblePlugins).containsOnly(foo, bar);

    allCompatiblePlugins = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.OLD_SONARQUBE).setInstalledSonarVersion(Version.create("2.2.2")).findAllCompatiblePlugins();

    assertThat(allCompatiblePlugins).containsOnly(bar);
  }

  @CheckForNull
  public Release getRelease(final String key, final String version, List<Release> releases) {
    return releases.stream()
      .filter(rel -> rel.getArtifact().getKey().equals(key) && rel.getVersion().getName().equals(version))
      .findFirst()
      .orElse(null);
  }

}
