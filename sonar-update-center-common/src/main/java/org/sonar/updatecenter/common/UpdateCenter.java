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
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public final class UpdateCenter {

  private Sonar sonar = new Sonar();
  private Set<Plugin> plugins;
  private Set<PluginsGroup> pluginsGroups;
  private Date date;

  public UpdateCenter() {
    this(new Date());
  }

  public UpdateCenter(Date date) {
    this.date = date;
    this.plugins = newHashSet();
    this.pluginsGroups = newHashSet();
  }

  public Set<Plugin> getPlugins() {
    return plugins;
  }

  public UpdateCenter setPlugins(Collection<Plugin> plugins) {
    this.plugins.clear();
    for (Plugin plugin : plugins) {
      addPlugin(plugin);
    }
    return this;
  }

  public Set<PluginsGroup> getPluginsGroups() {
    return pluginsGroups;
  }

  @Nullable
  public PluginsGroup getGroup(final String groupKey) {
    return Iterables.find(pluginsGroups, new Predicate<PluginsGroup>() {
      public boolean apply(PluginsGroup pluginsGroup) {
        return pluginsGroup.getKey().equals(groupKey);
      }
    }, null);
  }

  @Nullable
  public Plugin getPlugin(String key) {
    for (Plugin plugin : plugins) {
      if (StringUtils.equals(key, plugin.getKey())) {
        return plugin;
      }
    }
    return null;
  }

  public UpdateCenter addPlugin(Plugin plugin) {
    this.plugins.add(plugin);
    addGroup(plugin);
    return this;
  }

  public Sonar getSonar() {
    return sonar;
  }

  public UpdateCenter setSonar(Sonar sonar) {
    this.sonar = sonar;
    return this;
  }

  public Date getDate() {
    return date;
  }

  public UpdateCenter setDate(Date date) {
    this.date = date;
    return this;
  }

  private void addGroup(Plugin plugin) {
    String groupKey = plugin.getGroup() != null ? plugin.getGroup() : plugin.getKey();
    PluginsGroup pluginsGroup = getGroup(groupKey);
    if (pluginsGroup == null) {
      pluginsGroup = new PluginsGroup(groupKey);
      pluginsGroups.add(pluginsGroup);
    }
    pluginsGroup.addPlugin(plugin);
  }

}
