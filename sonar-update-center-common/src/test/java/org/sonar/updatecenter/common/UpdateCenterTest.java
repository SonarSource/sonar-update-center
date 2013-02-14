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

import static org.fest.assertions.Assertions.assertThat;

public class UpdateCenterTest {

  @Test
  public void get_and_set_plugins() {
    Plugin foo = new Plugin("foo");
    Plugin bar = new Plugin("bar");

    UpdateCenter center = new UpdateCenter();
    center.addPlugin(foo);
    center.addPlugin(bar);

    assertThat(center.getPlugin("foo")).isEqualTo(foo);
    assertThat(center.getPlugin("unknown")).isNull();
    assertThat(center.getAllChildrenPlugins()).hasSize(2);
  }

  @Test
  public void should_register_groups_containing_plugins() {
    Plugin foo = new Plugin("foo").setParent("foo");
    Plugin fooBis = new Plugin("fooBis").setParent("foo");

    Plugin bar = new Plugin("bar").setParent("bar");

    UpdateCenter center = new UpdateCenter();
    center.addPlugin(foo);
    center.addPlugin(fooBis);
    center.addPlugin(bar);

    assertThat(center.getAllChildrenPlugins()).hasSize(3);
    assertThat(center.getPluginParents()).hasSize(2);
    assertThat(center.getParent("foo").getChildren()).hasSize(2);
    assertThat(center.getParent("bar").getChildren()).hasSize(1);
  }

  @Test
  public void should_use_plugin_key_for_group_key_if_plugin_group_is_null() {
    Plugin foo = new Plugin("foo").setParent("foo");
    Plugin bar = new Plugin("bar").setParent(null);

    UpdateCenter center = new UpdateCenter();
    center.addPlugin(foo);
    center.addPlugin(bar);

    assertThat(center.getAllChildrenPlugins()).hasSize(2);
    assertThat(center.getPluginParents()).hasSize(2);
  }

}
