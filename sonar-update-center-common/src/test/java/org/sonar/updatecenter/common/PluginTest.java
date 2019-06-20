/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.updatecenter.common;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginTest {

  @Test
  public void should_merge_with_manifest() {
    Plugin plugin = Plugin.factory("squid")
        .setLicense("LGPL2")
        .setDescription("description")
        .setOrganization("organization")
        .setOrganizationUrl("organizationUrl")
        .setTermsConditionsUrl("termsconditions")
        .setIssueTrackerUrl("issueTrackerUrl")
        .setHomepageUrl("homepage")
        .setSourcesUrl("sourceURL");

    PluginManifest manifest = new PluginManifest()
        .setKey("squid")
        .setLicense("LGPL3")
        .setOrganization("organization_manifest")
        .setOrganizationUrl("organizationUrl_manifest")
        .setTermsConditionsUrl("termsconditions_manifest")
        .setDescription("description_manifest")
        .setHomepage("homepage_manifest")
        .setSourcesUrl("sourceURL_manifest")
        .setIssueTrackerUrl("issueTrackerUrl_manifest");

    plugin.merge(manifest);

    // precedence to the manifest file (=POM)
    assertThat(plugin.getLicense()).isEqualTo("LGPL3"); // UPC-32 don't override manifest
    assertThat(plugin.getOrganization()).isEqualTo("organization_manifest");
    assertThat(plugin.getOrganizationUrl()).isEqualTo("organizationUrl_manifest");
    assertThat(plugin.getTermsConditionsUrl()).isEqualTo("termsconditions_manifest");

    // precedence to the update center
    assertThat(plugin.getDescription()).isEqualTo("description");
    assertThat(plugin.getIssueTrackerUrl()).isEqualTo("issueTrackerUrl");
    assertThat(plugin.getHomepageUrl()).isEqualTo("homepage");
    assertThat(plugin.getSourcesUrl()).isEqualTo("sourceURL");
  }

  @Test
  public void should_not_merge_with_manifest_plugin_key_is_different() {
    Plugin plugin = Plugin.factory("squid").setOrganization("SonarSource");
    PluginManifest manifest = new PluginManifest().setKey("another_key").setOrganization("Other");

    plugin.merge(manifest);

    assertThat(plugin.getLicense()).isNull();
    assertThat(plugin.getKey()).isEqualTo("squid");
    assertThat(plugin.getOrganization()).isEqualTo("SonarSource");
  }

  @Test
  public void should_add_developers() {
    Plugin plugin = Plugin.factory("squid");
    PluginManifest manifest = new PluginManifest().setKey("squid").setDevelopers(new String[]{"Dev1"});

    plugin.merge(manifest);

    assertThat(plugin.getDevelopers()).contains("Dev1");
  }

  @Test
  public void should_add_sources_url() {
    Plugin plugin = Plugin.factory("squid");
    PluginManifest manifest = new PluginManifest().setKey("squid").setSourcesUrl("sourcesUrl");

    plugin.merge(manifest);

    assertThat(plugin.getSourcesUrl()).isEqualTo("sourcesUrl");
  }

  @Test
  public void should_return_string() {
    Plugin plugin = Plugin.factory("squid");

    assertThat(plugin.toString()).isEqualTo("squid");
  }

  @Test( expected = IllegalArgumentException.class)
  public void shouldPreventInvalidPluginKey() {
    Plugin.factory("foo-bar");
  }

}
