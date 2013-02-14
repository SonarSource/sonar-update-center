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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class PluginParent {

  private String key;
  private List<Plugin> children;

  public PluginParent(String key) {
    this.key = key;
    this.children = newArrayList();
  }

  public String getKey() {
    return key;
  }

  public Plugin getMasterPlugin() {
    return Iterables.find(children, new Predicate<Plugin>() {
      public boolean apply(Plugin plugin) {
        return plugin.getKey().equals(key);
      }
    });
  }

  public PluginParent addPlugin(Plugin plugin) {
    children.add(plugin);
    return this;
  }

  public List<Plugin> getChildren() {
    return children;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PluginParent pluginParent = (PluginParent) o;

    if (key != null ? !key.equals(pluginParent.key) : pluginParent.key != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return key != null ? key.hashCode() : 0;
  }
}
