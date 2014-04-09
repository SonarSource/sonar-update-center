/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.updatecenter.common;

import org.junit.Test;
import org.sonar.updatecenter.common.exception.DependencyCycleException;
import org.sonar.updatecenter.common.exception.IncompatiblePluginVersionException;
import org.sonar.updatecenter.common.exception.PluginNotFoundException;

import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class PluginReferentialTest {

  @Test
  public void get_and_set_plugins() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1");
    foo.addRelease(foo10);

    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, bar));

    assertThat(pluginReferential.findPlugin("foo")).isEqualTo(foo);
    assertThat(pluginReferential.getLastMasterReleasePlugins()).hasSize(2);
  }

  @Test(expected = NoSuchElementException.class)
  public void should_throw_exception_if_plugin_is_not_found() {
    Plugin foo = new Plugin("foo");
    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo));
    pluginReferential.findPlugin("not_found");
  }

  @Test
  public void should_register_groups_containing_plugins() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1");
    foo.addRelease(foo10);

    // foobis depends upon foo
    Plugin foobis = new Plugin("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1");
    foobis.addRelease(foobis10);

    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, foobis, bar));
    pluginReferential.setParent(foobis10, "foo");

    assertThat(pluginReferential.getLastMasterReleasePlugins()).hasSize(2);
    assertThat(pluginReferential.findPlugin("foo").getRelease("1.0").getChildren()).hasSize(1);
    assertThat(pluginReferential.findPlugin("bar").getRelease("1.0").getChildren()).hasSize(0);
  }

  @Test
  public void should_return_releases_keys_to_remove() {
    // Standalone plugin
    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1");
    test.addRelease(test10);

    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1");
    foo.addRelease(foo10);

    // foobis depends upon foo
    Plugin foobis = new Plugin("foobis");
    Release foobis10 = new Release(foobis, "1.0").addRequiredSonarVersions("2.1");
    foobis.addRelease(foobis10);

    // bar has one child and depends upon foobis
    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0").addRequiredSonarVersions("2.1");
    bar.addRelease(bar10);
    Plugin barbis = new Plugin("barbis");
    Release barbis10 = new Release(barbis, "1.0").addRequiredSonarVersions("2.1");
    barbis.addRelease(barbis10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, foobis, bar, test));
    pluginReferential.setParent(barbis10, "bar");
    pluginReferential.addOutgoingDependency(foobis10, "foo", "1.0");
    pluginReferential.addOutgoingDependency(bar10, "foo", "1.0");

    List<String> installablePlugins = pluginReferential.findLastReleasesWithDependencies("foo");
    assertThat(installablePlugins).hasSize(4);
    assertThat(installablePlugins).contains("foo", "foobis", "bar", "barbis");
  }

  @Test(expected = PluginNotFoundException.class)
  public void should_throw_exception_if_plugin_parent_does_not_exist() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0").addRequiredSonarVersions("2.1");
    foo.addRelease(foo10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo));
    pluginReferential.setParent(foo10, "not_found");
  }

  @Test(expected = IncompatiblePluginVersionException.class)
  public void should_throw_exception_if_child_has_not_same_version_as_parent() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0");
    Release foo11 = new Release(foo, "1.1");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.9");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, bar));
    pluginReferential.setParent(bar10, "foo");
  }

  @Test
  public void should_add_dependency() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0");
    foo.addRelease(foo10);

    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(bar, foo));
    pluginReferential.addOutgoingDependency(bar10, "foo", "1.0");
    assertThat(pluginReferential.findPlugin("bar").getRelease(Version.create("1.0")).getOutgoingDependencies().iterator().next().getVersion().getName()).isEqualTo("1.0");
  }

  @Test
  public void should_add_dependency_to_the_closest_compatible_version() {
    Plugin foo = new Plugin("foo");
    Release foo12 = new Release(foo, "1.2");
    Release foo13 = new Release(foo, "1.3");
    foo.addRelease(foo12);
    foo.addRelease(foo13);

    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(bar, foo));
    pluginReferential.addOutgoingDependency(bar10, "foo", "1.1");
    assertThat(pluginReferential.findPlugin("bar").getRelease(Version.create("1.0")).getOutgoingDependencies().iterator().next().getVersion().getName()).isEqualTo("1.2");
  }

  @Test(expected = PluginNotFoundException.class)
  public void should_throw_exception_if_dependency_does_not_exist() {
    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(bar));
    pluginReferential.addOutgoingDependency(bar10, "foo", "1.1");
  }

  @Test(expected = IncompatiblePluginVersionException.class)
  public void should_throw_exception_if_required_release_does_not_exist_with_minimal_version() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0");
    foo.addRelease(foo10);

    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0");
    bar.addRelease(bar10);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(bar, foo));
    pluginReferential.addOutgoingDependency(bar10, "foo", "1.1");
  }

  @Test
  public void should_check_dependency_cycle() {
    Plugin foo = new Plugin("foo");
    Release foo10 = new Release(foo, "1.0");
    Release foo11 = new Release(foo, "1.1");
    foo.addRelease(foo10);
    foo.addRelease(foo11);

    Plugin bar = new Plugin("bar");
    Release bar10 = new Release(bar, "1.0");
    Release bar11 = new Release(bar, "1.1");
    bar.addRelease(bar10);
    bar.addRelease(bar11);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(bar, foo));
    pluginReferential.addOutgoingDependency(bar10, "foo", "1.1");
    try {
      pluginReferential.addOutgoingDependency(foo11, "bar", "1.0");
      fail();
    } catch (DependencyCycleException e) {
      assertThat(e.getMessage()).contains("There is a dependency cycle between plugins 'bar', 'foo' that must be cut.");
    }
  }

}
