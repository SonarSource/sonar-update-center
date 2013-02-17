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

import java.util.Collections;
import java.util.NoSuchElementException;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

@Deprecated
public class PluginParentTest {

  @Test
  public void should_register_plugins() {
    PluginParent pluginParent = new PluginParent("java");
    pluginParent.addPlugin(new Plugin("java").setParent(new Plugin("java")), Collections.<PluginVersion>emptyList());
    pluginParent.addPlugin(new Plugin("plugin").setParent(new Plugin("java")), Collections.<PluginVersion>emptyList());

    assertThat(pluginParent.getChildren()).hasSize(2);
    assertThat(pluginParent.getMasterPlugin().getKey()).isEqualTo("java");
  }

  @Test
  public void should_add_required_plugins_from_children() {
    PluginParent pluginParent = new PluginParent("java");
    pluginParent.addPlugin(new Plugin("java").setParent(new Plugin("java")), newArrayList(PluginVersion.create(new Plugin("foo"), "1.0")));
    pluginParent.addPlugin(new Plugin("plugin").setParent(new Plugin("java")),
        newArrayList(PluginVersion.create(new Plugin("bar"), "1.1"), PluginVersion.create(new Plugin("bar2"), "1.2"))
    );

    assertThat(pluginParent.getPluginVersions()).hasSize(3);
  }

  @Test(expected = NoSuchElementException.class)
  public void should_get_master_plugin_throw_exception_if_not_existing() {
    PluginParent pluginParent = new PluginParent("java");
    pluginParent.addPlugin(new Plugin("plugin").setParent(new Plugin("java")), Collections.<PluginVersion>emptyList());

    assertThat(pluginParent.getMasterPlugin()).isNull();
  }
}
