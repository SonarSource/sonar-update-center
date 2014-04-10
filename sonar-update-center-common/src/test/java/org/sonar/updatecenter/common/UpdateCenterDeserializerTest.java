/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.updatecenter.common;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.updatecenter.common.UpdateCenterDeserializer.Mode;
import org.sonar.updatecenter.common.exception.PluginNotFoundException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class UpdateCenterDeserializerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void read_infos_from_properties() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      assertThat(center.getSonar().getVersions()).contains(Version.create("2.2"), Version.create("2.3"));
      assertThat(center.getSonar().getRelease(Version.create("2.2")).getDownloadUrl()).isEqualTo("http://dist.sonar.codehaus.org/sonar-2.2.zip");

      Plugin clirr = center.getUpdateCenterPluginReferential().findPlugin("clirr");
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
      UpdateCenter center = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      Plugin clirr = center.getUpdateCenterPluginReferential().findPlugin("clirr");
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
      UpdateCenter center = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      Plugin clirr = center.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getSourcesUrl()).isEqualTo("scm:svn:https://svn.codehaus.org/sonar-plugins/tags/sonar-clirr-plugin-1.1");

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  @Test
  public void should_add_children() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-parent.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      assertThat(pluginReferential.getUpdateCenterPluginReferential().getLastMasterReleasePlugins()).hasSize(1);

      Plugin clirr = pluginReferential.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getName()).isEqualTo("Clirr");
      Release release = clirr.getRelease("1.1");
      assertThat(release.getChildren()).hasSize(1);
      assertThat(release.getChild("motionchart")).isNotNull();
      assertThat(release.getChild("motionchart").getParent().getKey()).isEqualTo("clirr");
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  @Test
  public void should_add_dependencies() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-requires-plugins.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      assertThat(pluginReferential.getUpdateCenterPluginReferential().getLastMasterReleasePlugins()).hasSize(3);

      Plugin clirr = pluginReferential.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getName()).isEqualTo("Clirr");
      List<Release> requiredReleases = newArrayList(clirr.getRelease(Version.create("1.1")).getOutgoingDependencies());
      assertThat(requiredReleases).hasSize(2);
      assertThat(requiredReleases.get(0).getArtifact().getKey()).isEqualTo("foo");
      assertThat(requiredReleases.get(0).getVersion().getName()).isEqualTo("1.0");
      assertThat(requiredReleases.get(1).getArtifact().getKey()).isEqualTo("bar");
      assertThat(requiredReleases.get(1).getVersion().getName()).isEqualTo("1.1");
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  @Test(expected = PluginNotFoundException.class)
  public void should_throw_exception_when_parent_is_missing() {
    Properties props = new Properties();
    props.put("plugins", "foo");
    props.put("ltsVersion", "2.2");
    props.put("devVersion", "2.3-SNAPSHOT");
    props.put("publicVersions", "2.2");
    props.put("2.2.changelogUrl", "http://changelog");
    props.put("2.2.description", "2.2");
    props.put("2.2.downloadUrl", "http://2.2");
    props.put("2.2.date", "12-12-2012");
    props.put("foo.name", "Foo");
    props.put("foo.publicVersions", "1.1");
    props.put("foo.1.1.parent", "bar");
    props.put("foo.1.1.downloadUrl", "http://foo-1.1");
    props.put("foo.1.1.changelogUrl", "http://changelog");
    props.put("foo.1.1.description", "foo");
    props.put("foo.1.1.date", "12-12-2012");
    new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);
  }

  // UPC-6
  @Test
  public void should_resolve_ranges() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-ranges.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      Plugin clirr = pluginReferential.getUpdateCenterPluginReferential().findPlugin("clirr");
      SortedSet<Version> requiredSonarVersion = clirr.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(6);
      assertThat(requiredSonarVersion.first().toString()).isEqualTo("2.3");
      assertThat(requiredSonarVersion.last().toString()).isEqualTo("2.7.1");

      Plugin motionchart = pluginReferential.getUpdateCenterPluginReferential().findPlugin("motionchart");
      requiredSonarVersion = motionchart.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(6);
      assertThat(requiredSonarVersion.first().toString()).isEqualTo("2.2");
      assertThat(requiredSonarVersion.last().toString()).isEqualTo("2.8");
      assertThat(requiredSonarVersion).excludes(Version.create("2.7"));

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  // UPC-7
  @Test
  public void should_resolve_latest() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-latest.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      Plugin clirr = pluginReferential.getUpdateCenterPluginReferential().findPlugin("clirr");
      SortedSet<Version> requiredSonarVersion = clirr.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(7);
      assertThat(requiredSonarVersion.first().toString()).isEqualTo("2.3");
      assertThat(requiredSonarVersion.last().toString()).isEqualTo("2.8");

      Plugin motionchart = pluginReferential.getUpdateCenterPluginReferential().findPlugin("motionchart");
      requiredSonarVersion = motionchart.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(4);
      assertThat(requiredSonarVersion.first().toString()).isEqualTo("2.4");
      assertThat(requiredSonarVersion.last().toString()).isEqualTo("2.8");

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  // UPC-15
  @Test
  public void should_resolve_latest_using_sonar_devVersion() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-latest-and-devVersion.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = new UpdateCenterDeserializer(Mode.DEV, false).fromProperties(props);

      Plugin clirr = pluginReferential.getUpdateCenterPluginReferential().findPlugin("clirr");
      SortedSet<Version> requiredSonarVersion = clirr.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(8);
      assertThat(requiredSonarVersion.first().toString()).isEqualTo("2.3");
      assertThat(requiredSonarVersion.last().toString()).isEqualTo("3.0");

      Plugin motionchart = pluginReferential.getUpdateCenterPluginReferential().findPlugin("motionchart");
      requiredSonarVersion = motionchart.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(4);
      assertThat(requiredSonarVersion.first().toString()).isEqualTo("2.4");
      assertThat(requiredSonarVersion.last().toString()).isEqualTo("3.0");

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  // UPC-19
  @Test
  public void should_parse_lts() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/sonar-lts.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);
      assertThat(center.getSonar().getLtsRelease().getVersion()).isEqualTo(Version.create("2.3"));
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  // UPC-19
  @Test
  public void should_throw_if_lts_invalid() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/sonar-lts-invalid.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("ltsVersion seems wrong as it is not listed in SonarQube versions");
      new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  // UPC-29
  @Test
  public void should_load_new_format_in_dev_mode() throws IOException, URISyntaxException {
    URL url = getClass().getResource("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/newFormat/update-center.properties");
    UpdateCenter center = new UpdateCenterDeserializer(Mode.DEV, false).fromProperties(new File(url.toURI()));
    assertThat(center.getSonar().getLtsRelease().getVersion()).isEqualTo(Version.create("3.7.1"));
    assertThat(center.getUpdateCenterPluginReferential().findPlugin("abap").getDevRelease().getVersion()).isEqualTo(Version.create("2.2.1-SNAPSHOT"));
    Plugin phpPlugin = center.getUpdateCenterPluginReferential().findPlugin("php");
    assertThat(phpPlugin.getDevRelease().getVersion()).isEqualTo(Version.create("2.3-SNAPSHOT"));
    assertThat(phpPlugin.getPublicVersions()).onProperty("name").containsOnly("2.1", "2.2");
    assertThat(phpPlugin.getPrivateVersions()).onProperty("name").containsOnly("2.2.1");
  }

  // UPC-29
  @Test
  public void should_load_new_format_in_prod_mode() throws IOException, URISyntaxException {
    URL url = getClass().getResource("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/newFormat/update-center.properties");
    UpdateCenter center = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(new File(url.toURI()));
    assertThat(center.getSonar().getLtsRelease().getVersion()).isEqualTo(Version.create("3.7.1"));
    assertThat(center.getUpdateCenterPluginReferential().findPlugin("abap").getDevRelease()).isNull();
    assertThat(center.getUpdateCenterPluginReferential().findPlugin("php").getDevRelease()).isNull();
  }

  // UPC-29
  @Test
  public void should_fail_if_overlap_in_sqVersion_of_public_releases() throws IOException {

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("SQ version 2.7.1 is declared compatible with two public versions of Clirr plugin: 1.1 and 1.0");

    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-overlap-sqVersion.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  // UPC-29
  @Test
  public void should_ignore_errors() throws IOException {

    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-overlap-sqVersion.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      new UpdateCenterDeserializer(Mode.PROD, true).fromProperties(props);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
