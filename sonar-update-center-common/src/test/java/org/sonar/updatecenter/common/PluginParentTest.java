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

import static org.fest.assertions.Assertions.assertThat;

public class PluginParentTest {

  @Test
  public void should_register_plugins() {
    PluginParent pluginParent = new PluginParent("java");
    pluginParent.addPlugin(new Plugin("java").setParent("java"));
    pluginParent.addPlugin(new Plugin("plugin").setParent("java"));

    assertThat(pluginParent.getChildren()).hasSize(2);
    assertThat(pluginParent.getMasterPlugin().getKey()).isEqualTo("java");
  }

  @Test(expected = NoSuchElementException.class)
  public void should_get_master_plugin_throw_exception_if_not_existing() {
    PluginParent pluginParent = new PluginParent("java");
    pluginParent.addPlugin(new Plugin("plugin").setParent("java"));

    assertThat(pluginParent.getMasterPlugin()).isNull();
  }
}
