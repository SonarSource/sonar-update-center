/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.updatecenter.common.exception.PluginNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class UpdateCenterDeserializerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void read_infos_from_froperties() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = UpdateCenterDeserializer.fromProperties(props);

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
      UpdateCenter center = UpdateCenterDeserializer.fromProperties(props);

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
      UpdateCenter center = UpdateCenterDeserializer.fromProperties(props);

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
      UpdateCenter pluginReferential = UpdateCenterDeserializer.fromProperties(props);

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
      UpdateCenter pluginReferential = UpdateCenterDeserializer.fromProperties(props);

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
    props.put("foo.name", "Foo");
    props.put("foo.versions", "1.1");
    props.put("foo.1.1.parent", "bar");
    UpdateCenterDeserializer.fromProperties(props);
  }

  // UPC-6
  @Test
  public void should_resolve_ranges() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-ranges.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = UpdateCenterDeserializer.fromProperties(props);

      Plugin clirr = pluginReferential.getUpdateCenterPluginReferential().findPlugin("clirr");
      SortedSet<Version> requiredSonarVersion = clirr.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(5);
      assertThat(requiredSonarVersion.first().toString()).isEqualTo("2.3");
      assertThat(requiredSonarVersion.last().toString()).isEqualTo("2.7");

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
      UpdateCenter pluginReferential = UpdateCenterDeserializer.fromProperties(props);

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
  public void should_resolve_latest_using_sonar_next() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-latest-and-next.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = UpdateCenterDeserializer.fromProperties(props);

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

  // UPC-15
  @Test
  public void should_throw_if_sonar_next_outdated() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/sonar-next-outdated.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("sonar.nextVersions seems outdated. 2.8 is already listed in sonar.versions. Update or remove it.");
      UpdateCenterDeserializer.fromProperties(props);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  // UPC-24
  @Test
  public void should_resolve_latest_with_multiple_sonar_next() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-multiple-next.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = UpdateCenterDeserializer.fromProperties(props);

      Plugin clirr = pluginReferential.getUpdateCenterPluginReferential().findPlugin("clirr");
      SortedSet<Version> requiredSonarVersion = clirr.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(6);
      assertThat(requiredSonarVersion.first().toString()).isEqualTo("3.7");
      assertThat(requiredSonarVersion).onProperty("name").contains("3.7.3");
      assertThat(requiredSonarVersion).onProperty("name").contains("4.0");
      assertThat(requiredSonarVersion.last().toString()).isEqualTo("4.1");

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  // UPC-10
  @Test
  public void should_filter_snapshots() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-snapshots.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = UpdateCenterDeserializer.fromProperties(props, false);

      assertThat(center.getSonar().getVersions()).contains(Version.create("2.2"), Version.create("2.3"), Version.create("2.4-SNAPSHOT"));
      assertThat(center.getSonar().getRelease(Version.create("2.4-SNAPSHOT")).getDownloadUrl()).isEqualTo("http://dist.sonar.codehaus.org/sonar-2.4-SNAPSHOT.zip");

      Plugin clirr = center.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getName()).isEqualTo("Clirr");
      assertThat(clirr.getDescription()).isEqualTo("Clirr Plugin");
      assertThat(clirr.getVersions()).contains(Version.create("1.0"), Version.create("1.1"), Version.create("1.2-SNAPSHOT"));

      assertThat(clirr.getSourcesUrl()).isNull();
      assertThat(clirr.getDevelopers()).isEmpty();

      assertThat(clirr.getRelease(Version.create("1.2-SNAPSHOT")).getDownloadUrl()).isEqualTo("http://dist.sonar-plugins.codehaus.org/clirr-1.2-SNAPSHOT.jar");

      center = UpdateCenterDeserializer.fromProperties(props, true);

      assertThat(center.getSonar().getVersions()).excludes(Version.create("2.4-SNAPSHOT"));
      assertThat(center.getSonar().doesContainVersion(Version.create("2.4-SNAPSHOT"))).isFalse();

      clirr = center.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getVersions()).excludes(Version.create("1.2-SNAPSHOT"));

      assertThat(clirr.doesContainVersion(Version.create("1.2-SNAPSHOT"))).isFalse();

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
      UpdateCenter center = UpdateCenterDeserializer.fromProperties(props);
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
      thrown.expectMessage("sonar.ltsVersion seems wrong as it is not listed in sonar.versions");
      UpdateCenterDeserializer.fromProperties(props);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
