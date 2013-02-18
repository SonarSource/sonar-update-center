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

import org.junit.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class PluginReferentialTest {

  @Test
  public void get_and_set_plugins() {
    Plugin foo = new Plugin("foo");
    Plugin bar = new Plugin("bar");
    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, bar));

    assertThat(pluginReferential.findPlugin("foo")).isEqualTo(foo);
    assertThat(pluginReferential.findPlugin("unknown")).isNull();
    assertThat(pluginReferential.getPlugins()).hasSize(2);
  }

  @Test
  public void should_register_groups_containing_plugins() {
    Plugin foo = new Plugin("foo").setParent(new Plugin("foo"));
    Plugin fooBis = new Plugin("fooBis").setParent(new Plugin("foo"));
    Plugin bar = new Plugin("bar").setParent(new Plugin("bar"));

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, fooBis, bar));

    assertThat(pluginReferential.getPlugins()).hasSize(2);
    assertThat(pluginReferential.findPlugin("foo").getChildren()).hasSize(1);
    assertThat(pluginReferential.findPlugin("bar").getChildren()).hasSize(0);
  }

  @Test
  public void should_use_plugin_key_for_group_key_if_plugin_group_is_null() {
    Plugin foo = new Plugin("foo").setParent(new Plugin("foo"));
    Plugin bar = new Plugin("bar").setParent(null);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, bar));

    assertThat(pluginReferential.getPlugins()).hasSize(2);
  }

  @Test(expected = NoSuchElementException.class)
  public void should_throw_exception_if_plugin_parent_does_not_exist() {
    Plugin foo = new Plugin("foo").setParent(new Plugin("not_found"));
    PluginReferential.create(newArrayList(foo));
  }

  @Test
  public void should_find_latest_release() {
    Plugin foo = new Plugin("foo").setParent(new Plugin("foo"));
    foo.addRelease("1.0");
    foo.addRelease("1.1");

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo));
    assertThat(pluginReferential.findLatestRelease("foo").getVersion().getName()).isEqualTo("1.1");
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

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, foobis, bar, test));

    List<String> installablePlugins = pluginReferential.findReleasesWithDependencies("foo");
    assertThat(installablePlugins).containsExactly("foo", "foobis", "bar", "barbis");
  }

}
