/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2024 SonarSource SA
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

import java.io.CharArrayReader;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.sonar.updatecenter.common.exception.DependencyCycleException;
import org.sonar.updatecenter.common.exception.IncompatiblePluginVersionException;
import org.sonar.updatecenter.common.exception.PluginNotFoundException;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.updatecenter.common.PluginReferential.PLUGINS_BUNDLED_IN_LTS;

public class PluginReferentialTest {

  private static final String PLUGIN_LICENSE_KEY = "license";

  @Test
  public void get_and_set_plugins() {
    Plugin foo = Plugin.factory("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1");
    foo.addRelease(foo10);

    Plugin bar = Plugin.factory("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(asList(foo, bar));

    assertThat(pluginReferential.findPlugin("foo")).isEqualTo(foo);
    assertThat(pluginReferential.getLastMasterReleasePlugins()).hasSize(2);
  }

  @Test(expected = NoSuchElementException.class)
  public void should_throw_exception_if_plugin_is_not_found() {
    Plugin foo = Plugin.factory("foo");
    PluginReferential pluginReferential = PluginReferential.create(asList(foo));
    pluginReferential.findPlugin("not_found");
  }

  @Test
  public void should_return_releases_keys_to_remove() {
    // Standalone plugin
    Plugin test = Plugin.factory("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1");
    test.addRelease(test10);

    Plugin foo = Plugin.factory("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1");
    foo.addRelease(foo10);

    // foobis depends upon foo
    Plugin foobis = Plugin.factory("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1");
    foobis.addRelease(foobis10);

    // bar has one child and depends upon foobis
    Plugin bar = Plugin.factory("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1");
    bar.addRelease(bar10);
    Plugin barbis = Plugin.factory("barbis");
    Release barbis10 = new Release(barbis, "1.0").addRequiredSonarVersions("2.1");
    barbis.addRelease(barbis10);

    PluginReferential pluginReferential = PluginReferential.create(asList(foo, foobis, bar, test));
    pluginReferential.addOutgoingDependency(foobis10, "foo", "1.0");
    pluginReferential.addOutgoingDependency(bar10, "foo", "1.0");

    List<String> installablePlugins = pluginReferential.findLastReleasesWithDependencies("foo");
    assertThat(installablePlugins).containsOnly("foo", "foobis", "bar");
  }

  @Test
  public void should_add_dependency() {
    Plugin foo = createPluginWithSingleRelease("foo");

    Plugin bar = Plugin.factory("bar");
    Release bar10 = new Release(bar, "1.0");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(asList(bar, foo));
    pluginReferential.addOutgoingDependency(bar10, "foo", "1.0");
    assertThat(pluginReferential.findPlugin("bar").getRelease(Version.create("1.0")).getOutgoingDependencies().iterator().next().getVersion().getName()).isEqualTo("1.0");
  }


  @Test
  public void ignore_add_license_dependency() {
    Set<Plugin> bundledPlugins = generateBundledPlugins();
    Plugin dependentPlugin = createPluginWithSingleRelease("dependentPlugin");
    Release dependentRelease = Objects.requireNonNull(dependentPlugin.getLastRelease());

    List<Plugin> allPlugins = new ArrayList<>(bundledPlugins);
    allPlugins.add(dependentPlugin);

    PluginReferential pluginReferential = PluginReferential.create(allPlugins);
    Version version = Version.create("1.0");
    PLUGINS_BUNDLED_IN_LTS.forEach(bundledPlugin -> pluginReferential.addOutgoingDependency(dependentRelease, bundledPlugin, version.getName()));

    assertThat(pluginReferential.findPlugin(dependentPlugin.getKey()).getRelease(version).getOutgoingDependencies()).isEmpty();
  }

  private static Set<Plugin> generateBundledPlugins() {
    Set<Plugin> bundledPlugins = PLUGINS_BUNDLED_IN_LTS.stream()
      .map(PluginReferentialTest::createPluginWithSingleRelease)
      .collect(toSet());
    return bundledPlugins;
  }

  private static Plugin createPluginWithSingleRelease(String pluginLicenseKey) {
    Plugin licensePlugin = Plugin.factory(pluginLicenseKey);
    Release licenseRelease = new Release(licensePlugin, "1.0");
    licensePlugin.addRelease(licenseRelease);
    return licensePlugin;
  }

  @Test
  public void should_add_dependency_to_the_closest_compatible_version() {
    Plugin foo = Plugin.factory("foo");
    Release foo12 = new Release(foo, "1.2");
    Release foo13 = new Release(foo, "1.3");
    foo.addRelease(foo12);
    foo.addRelease(foo13);

    Plugin bar = Plugin.factory("bar");
    Release bar10 = new Release(bar, "1.0");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(asList(bar, foo));
    pluginReferential.addOutgoingDependency(bar10, "foo", "1.1");
    assertThat(pluginReferential.findPlugin("bar").getRelease(Version.create("1.0")).getOutgoingDependencies().iterator().next().getVersion().getName()).isEqualTo("1.2");
  }

  @Test(expected = PluginNotFoundException.class)
  public void should_throw_exception_if_dependency_does_not_exist() {
    Plugin bar = Plugin.factory("bar");
    Release bar10 = new Release(bar, "1.0");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(asList(bar));
    pluginReferential.addOutgoingDependency(bar10, "foo", "1.1");
  }

  @Test(expected = IncompatiblePluginVersionException.class)
  public void should_throw_exception_if_required_release_does_not_exist_with_minimal_version() {
    Plugin foo = Plugin.factory("foo");
    Release foo10 = new Release(foo, "1.0");
    foo.addRelease(foo10);

    Plugin bar = Plugin.factory("bar");
    Release bar10 = new Release(bar, "1.0");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(asList(bar, foo));
    pluginReferential.addOutgoingDependency(bar10, "foo", "1.1");
  }

  @Test
  public void should_check_dependency_cycle() {
    Plugin foo = Plugin.factory("foo");
    Release foo10 = new Release(foo, "1.0");
    Release foo11 = new Release(foo, "1.1");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    Plugin bar = Plugin.factory("bar");
    Release bar10 = new Release(bar, "1.0");
    Release bar11 = new Release(bar, "1.1");
    bar.addRelease(bar10);
    bar.addRelease(bar11);

    PluginReferential pluginReferential = PluginReferential.create(asList(bar, foo));
    pluginReferential.addOutgoingDependency(bar10, "foo", "1.1");
    try {
      pluginReferential.addOutgoingDependency(foo11, "bar", "1.0");
      fail();
    } catch (DependencyCycleException e) {
      assertThat(e.getMessage()).contains("There is a dependency cycle between plugins 'bar', 'foo' that must be cut.");
    }
  }

}
