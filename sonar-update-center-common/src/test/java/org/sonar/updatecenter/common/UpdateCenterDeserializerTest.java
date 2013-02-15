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

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class UpdateCenterDeserializerTest {

  @Test
  public void read_infos_from_froperties() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      PluginReferential center = UpdateCenterDeserializer.fromProperties(props);

      assertThat(center.getSonar().getVersions()).contains(Version.create("2.2"), Version.create("2.3"));
      assertThat(center.getSonar().getRelease(Version.create("2.2")).getDownloadUrl()).isEqualTo("http://dist.sonar.codehaus.org/sonar-2.2.zip");

      Plugin clirr = center.getPlugin("clirr");
      assertThat(clirr.getName()).isEqualTo("Clirr");
      assertThat(clirr.getDescription()).isEqualTo("Clirr Plugin");
      assertThat(clirr.getVersions()).contains(Version.create("1.0"), Version.create("1.1"));

      assertThat(clirr.getSourcesUrl()).isNull();
      assertThat(clirr.getDevelopers()).isEmpty();

      assertThat(clirr.getRelease(Version.create("1.0")).getDownloadUrl()).isEqualTo("http://dist.sonar-plugins.codehaus.org/clirr-1.0.jar");
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  @Test
  public void should_add_developers() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-developers.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      PluginReferential center = UpdateCenterDeserializer.fromProperties(props);

      Plugin clirr = center.getPlugin("clirr");
      assertThat(clirr.getDevelopers()).hasSize(3);
      assertThat(clirr.getDevelopers()).contains("Mike Haller", "Freddy Mallet", "Simon Brandhof");

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  @Test
  public void should_add_sources_url() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-scm.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      PluginReferential center = UpdateCenterDeserializer.fromProperties(props);

      Plugin clirr = center.getPlugin("clirr");
      assertThat(clirr.getSourcesUrl()).isEqualTo("scm:svn:https://svn.codehaus.org/sonar-plugins/tags/sonar-clirr-plugin-1.1");

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  @Test
  public void should_add_parent() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-parent.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      PluginReferential pluginReferential = UpdateCenterDeserializer.fromProperties(props);

      assertThat(pluginReferential.getPlugins()).hasSize(1);

      Plugin clirr = pluginReferential.getPlugin("clirr");
      assertThat(clirr.getName()).isEqualTo("Clirr");
      assertThat(clirr.getChildren()).hasSize(1);
      assertThat(clirr.getChild("motionchart")).isNotNull();
      assertThat(clirr.getChild("motionchart").getName()).isEqualTo("Motion Chart");
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  @Test
  public void should_add_requires_plugins() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-requires-plugins.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      PluginReferential pluginReferential = UpdateCenterDeserializer.fromProperties(props);

      assertThat(pluginReferential.getPlugins()).hasSize(3);

      Plugin clirr = pluginReferential.getPlugin("clirr");
      assertThat(clirr.getName()).isEqualTo("Clirr");
      assertThat(clirr.getRequiredPlugins()).hasSize(2);
      assertThat(clirr.getRequiredPlugins().get(0).getArtifact().getKey()).isEqualTo("foo");
      assertThat(clirr.getRequiredPlugins().get(0).getVersion().getName()).isEqualTo("1.0");
      assertThat(clirr.getRequiredPlugins().get(1).getArtifact().getKey()).isEqualTo("bar");
      assertThat(clirr.getRequiredPlugins().get(1).getVersion().getName()).isEqualTo("1.1");
    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
