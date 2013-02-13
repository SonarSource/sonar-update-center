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
    assertThat(center.getPlugins()).hasSize(2);
  }

  @Test
  public void should_register_groups_containing_plugins() {
    Plugin foo = new Plugin("foo").setGroup("foo");
    Plugin fooBis = new Plugin("fooBis").setGroup("foo");

    Plugin bar = new Plugin("bar").setGroup("bar");

    UpdateCenter center = new UpdateCenter();
    center.addPlugin(foo);
    center.addPlugin(fooBis);
    center.addPlugin(bar);

    assertThat(center.getPlugins()).hasSize(3);
    assertThat(center.getPluginsGroups()).hasSize(2);
    assertThat(center.getGroup("foo").getPlugins()).hasSize(2);
    assertThat(center.getGroup("bar").getPlugins()).hasSize(1);
  }

  @Test
  public void should_use_plugin_key_for_group_key_if_plugin_group_is_null() {
    Plugin foo = new Plugin("foo").setGroup("foo");
    Plugin bar = new Plugin("bar").setGroup(null);

    UpdateCenter center = new UpdateCenter();
    center.addPlugin(foo);
    center.addPlugin(bar);

    assertThat(center.getPlugins()).hasSize(2);
    assertThat(center.getPluginsGroups()).hasSize(2);
  }

}
