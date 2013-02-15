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

import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public final class PluginReferential {

  private Sonar sonar;
  private Set<Plugin> plugins;
  private Date date;

  private PluginReferential(Sonar sonar, Date date) {
    this.date = date;
    this.plugins = newHashSet();
    this.sonar = sonar;
  }

  public static PluginReferential create(Sonar sonar, Date date) {
    return new PluginReferential(sonar, date);
  }

  public static PluginReferential create(List<Plugin> pluginList, Sonar sonar, Date date) {
    PluginReferential pluginReferential = new PluginReferential(sonar, date);

    for (Plugin plugin : pluginList) {
      if (plugin.isMaster()) {
        pluginReferential.add(plugin);
      } else {
        pluginReferential.addChild(plugin, pluginList);
      }
    }
    return pluginReferential;
  }

  public static PluginReferential create(List<Plugin> pluginList, Sonar sonar) {
    return PluginReferential.create(pluginList, sonar, null);
  }

  public static PluginReferential create(List<Plugin> pluginList) {
    return PluginReferential.create(pluginList, new Sonar(), null);
  }

  public Set<Plugin> getPlugins() {
    return plugins;
  }

  @Nullable
  public Plugin getPlugin(final String key) {
    return Iterables.find(plugins, new Predicate<Plugin>() {
      public boolean apply(Plugin input) {
        return input.getKey().equals(key);
      }
    }, null);
  }

  public Sonar getSonar() {
    return sonar;
  }

  private PluginReferential setSonar(Sonar sonar) {
    this.sonar = sonar;
    return this;
  }

  public Date getDate() {
    return date;
  }

  public PluginReferential setDate(Date date) {
    this.date = date;
    return this;
  }

  public PluginReferential add(Plugin plugin) {
    this.plugins.add(plugin);
    return this;
  }

  public Release findRelease(String pluginKey, String version) {
    Plugin plugin = getPlugin(pluginKey);
    return plugin.getRelease(Version.create(version));
  }

  private void addChild(final Plugin plugin, List<Plugin> pluginList) {
    Plugin pluginParent = Iterables.find(pluginList, new Predicate<Plugin>() {
      public boolean apply(Plugin input) {
        return input.getKey().equals(plugin.getParent().getKey());
      }
    });
    pluginParent.addChild(plugin);
  }

}
