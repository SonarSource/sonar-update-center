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


public class PluginTest {

  @Test
  public void should_merge_with_manifest() {
    Plugin plugin = new Plugin("squid").setLicense("LGPL2").setOrganization("SonarSource");
    PluginManifest manifest = new PluginManifest().setKey("squid").setLicense("LGPL3").setDescription("Parser").setParent("parent");

    plugin.merge(manifest);

    assertThat(plugin.getLicense()).isEqualTo("LGPL2"); // initial definition is reference
    assertThat(plugin.getOrganization()).isEqualTo("SonarSource");
    assertThat(plugin.getDescription()).isEqualTo("Parser");
  }

  @Test
  public void should_not_merge_with_manifest_plugin_key_is_different() {
    Plugin plugin = new Plugin("squid").setOrganization("SonarSource");
    PluginManifest manifest = new PluginManifest().setKey("another_key").setOrganization("Other");

    plugin.merge(manifest);

    assertThat(plugin.getLicense());
    assertThat(plugin.getKey()).isEqualTo("squid");
    assertThat(plugin.getOrganization()).isEqualTo("SonarSource");
  }

  @Test
  public void should_add_developers() {
    Plugin plugin = new Plugin("squid");
    PluginManifest manifest = new PluginManifest().setKey("squid").setDevelopers(new String[]{"Dev1"});

    plugin.merge(manifest);

    assertThat(plugin.getDevelopers()).contains("Dev1");
  }

  @Test
  public void should_add_sources_url() {
    Plugin plugin = new Plugin("squid");
    PluginManifest manifest = new PluginManifest().setKey("squid").setSourcesUrl("sourcesUrl");

    plugin.merge(manifest);

    assertThat(plugin.getSourcesUrl()).isEqualTo("sourcesUrl");
  }

  @Test
  public void shoukd_return_string() {
    Plugin plugin = new Plugin("squid");

    assertThat(plugin.toString()).isEqualTo("squid");
  }

}
