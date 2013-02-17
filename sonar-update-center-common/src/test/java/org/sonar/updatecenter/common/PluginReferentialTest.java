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

import java.util.NoSuchElementException;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class PluginReferentialTest {

  @Test
  public void get_and_set_plugins() {
    Plugin foo = new Plugin("foo");
    Plugin bar = new Plugin("bar");
    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, bar), new Sonar());

    assertThat(pluginReferential.findPlugin("foo")).isEqualTo(foo);
    assertThat(pluginReferential.findPlugin("unknown")).isNull();
    assertThat(pluginReferential.getPlugins()).hasSize(2);
  }

  @Test
  public void should_register_groups_containing_plugins() {
    Plugin foo = new Plugin("foo").setParent(new Plugin("foo"));
    Plugin fooBis = new Plugin("fooBis").setParent(new Plugin("foo"));
    Plugin bar = new Plugin("bar").setParent(new Plugin("bar"));

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, fooBis, bar), new Sonar());

    assertThat(pluginReferential.getPlugins()).hasSize(2);
    assertThat(pluginReferential.findPlugin("foo").getChildren()).hasSize(1);
    assertThat(pluginReferential.findPlugin("bar").getChildren()).hasSize(0);
  }

  @Test
  public void should_use_plugin_key_for_group_key_if_plugin_group_is_null() {
    Plugin foo = new Plugin("foo").setParent(new Plugin("foo"));
    Plugin bar = new Plugin("bar").setParent(null);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, bar), new Sonar());

    assertThat(pluginReferential.getPlugins()).hasSize(2);
  }

  @Test(expected = NoSuchElementException.class)
  public void should_throw_exception_if_plugin_parent_does_not_exist() {
    Plugin foo = new Plugin("foo").setParent(new Plugin("not_found"));
    PluginReferential.create(newArrayList(foo), new Sonar());
  }

}
