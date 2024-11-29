/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.updatecenter.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.SortedSet;
import org.junit.Test;
import org.sonar.updatecenter.common.UpdateCenterDeserializer.Mode;
import org.sonar.updatecenter.common.exception.SonarVersionRangeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class UpdateCenterDeserializerTest {

  @Test
  public void read_infos_from_properties() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      assertThat(center.getSonar().getVersions()).contains(Version.create("2.2"), Version.create("2.3"), Version.create("2039.11"),
        Version.create("2039.12"), Version.create("39.11"), Version.create("39.12"));
      assertThat(center.getSonar().getRelease(Version.create("2.2"), Product.OLD_SONARQUBE).getDownloadUrl()).isEqualTo("http://dist.sonar.codehaus.org/sonar-2.2.zip");
      assertThat(center.getSonar().getRelease(Version.create("2.2"), Product.OLD_SONARQUBE).getDownloadUrl(Release.Edition.DEVELOPER)).isEqualTo("http://dist.sonar.codehaus.org/sonar-developer-2.2.zip");
      assertThat(center.getSonar().getRelease(Version.create("2.2"), Product.OLD_SONARQUBE).getDownloadUrl(Release.Edition.ENTERPRISE)).isEqualTo("http://dist.sonar.codehaus.org/sonar-enterprise-2.2.zip");
      assertThat(center.getSonar().getRelease(Version.create("2.2"), Product.OLD_SONARQUBE).getDownloadUrl(Release.Edition.DATACENTER)).isEqualTo("http://dist.sonar.codehaus.org/sonar-datacenter-2.2.zip");
      assertThat(center.getSonar().getRelease(Version.create("2.2"), Product.OLD_SONARQUBE).getDisplayVersion()).isEqualTo("2.2");
      assertThat(center.getSonar().getRelease(Version.create("2.3"), Product.OLD_SONARQUBE).getDisplayVersion()).isEqualTo("2.3 (build 42)");
      assertThat(center.getSonar().getRelease(Version.create("2.3"), Product.OLD_SONARQUBE).getDownloadUrl(Release.Edition.DATACENTER)).isNullOrEmpty();

      Plugin clirr = center.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getName()).isEqualTo("Clirr");
      assertThat(clirr.getDescription()).isEqualTo("Clirr Plugin");
      assertThat(clirr.getIssueTrackerUrl()).isEqualTo("http://jira.codehaus.org/issueTracker/for/clirr");
      assertThat(clirr.getVersions()).contains(Version.create("1.0"), Version.create("1.1"));

      assertThat(clirr.getSourcesUrl()).isNull();
      assertThat(clirr.getDevelopers()).isEmpty();

      Release clirr1_0 = clirr.getRelease(Version.create("1.0"));
      assertThat(clirr1_0.getDownloadUrl()).isEqualTo("http://dist.sonar-plugins.codehaus.org/clirr-1.0.jar");
      assertThat(clirr1_0.getDisplayVersion()).isEqualTo("1.0");
      assertThat(clirr1_0.getMinimumRequiredSonarVersion(Product.OLD_SONARQUBE)).isEqualTo(Version.create("2.2"));
      assertThat(clirr1_0.getLastRequiredSonarVersion(Product.OLD_SONARQUBE)).isEqualTo(Version.create("2.2"));

      assertThat(clirr.getRelease(Version.create("1.1")).getDisplayVersion()).isEqualTo("1.1 (build 42)");

      Plugin motionchart = center.getUpdateCenterPluginReferential().findPlugin("motionchart");
      assertThat(motionchart.getRelease(Version.create("1.7")).getDisplayVersion()).isNull();

      Version paidSonarQubeVersion = Version.create("2039.11");
      assertThat(center.getSonar().getRelease(paidSonarQubeVersion, Product.SONARQUBE_SERVER).getDownloadUrl()).isNull();
      assertThat(center.getSonar().getRelease(paidSonarQubeVersion, Product.SONARQUBE_SERVER).getDownloadUrl(Release.Edition.DEVELOPER))
        .isEqualTo("http://dist.sonar.codehaus.org/sonar-developer-11.zip");
      assertThat(center.getSonar().getRelease(paidSonarQubeVersion, Product.SONARQUBE_SERVER).getDownloadUrl(Release.Edition.ENTERPRISE))
        .isEqualTo("http://dist.sonar.codehaus.org/sonar-enterprise-11.zip");
      assertThat(center.getSonar().getRelease(paidSonarQubeVersion, Product.SONARQUBE_SERVER).getDownloadUrl(Release.Edition.DATACENTER))
        .isEqualTo("http://dist.sonar.codehaus.org/sonar-datacenter-11.zip");
      assertThat(center.getSonar().getRelease(paidSonarQubeVersion, Product.SONARQUBE_SERVER).getDisplayVersion()).isEqualTo("2039.11");

      Version communityBuildVersion = Version.create("39.11");
      assertThat(center.getSonar().getRelease(communityBuildVersion, Product.SONARQUBE_COMMUNITY_BUILD).getDownloadUrl()).isEqualTo("http://dist.sonar.codehaus.org/sonar-3911.zip");
      assertThat(center.getSonar().getRelease(communityBuildVersion, Product.SONARQUBE_COMMUNITY_BUILD).getDownloadUrl(Release.Edition.DEVELOPER)).isNull();
      assertThat(center.getSonar().getRelease(communityBuildVersion, Product.SONARQUBE_COMMUNITY_BUILD).getDownloadUrl(Release.Edition.ENTERPRISE)).isNull();
      assertThat(center.getSonar().getRelease(communityBuildVersion, Product.SONARQUBE_COMMUNITY_BUILD).getDownloadUrl(Release.Edition.DATACENTER)).isNull();
      assertThat(center.getSonar().getRelease(communityBuildVersion, Product.SONARQUBE_COMMUNITY_BUILD).getDisplayVersion()).isEqualTo("39.11");

      Plugin bestPlugin = center.getUpdateCenterPluginReferential().findPlugin("bestplugin");
      Release bestPlugin10 = bestPlugin.getRelease(Version.create("1.0"));
      assertThat(bestPlugin10.getDownloadUrl()).isEqualTo("http://best-plugin.org/best-plugin-1.0.jar");
      assertThat(bestPlugin10.getDisplayVersion()).isEqualTo("1.0");
      assertThat(bestPlugin10.getMinimumRequiredSonarVersion(Product.OLD_SONARQUBE)).isNull();
      assertThat(bestPlugin10.getLastRequiredSonarVersion(Product.OLD_SONARQUBE)).isNull();
      assertThat(bestPlugin10.getMinimumRequiredSonarVersion(Product.SONARQUBE_COMMUNITY_BUILD)).isEqualTo(Version.create("39.11"));
      assertThat(bestPlugin10.getLastRequiredSonarVersion(Product.SONARQUBE_COMMUNITY_BUILD)).isEqualTo(Version.create("39.11"));

      Release bestPlugin11 = bestPlugin.getRelease(Version.create("1.1"));
      assertThat(bestPlugin11.getDownloadUrl()).isEqualTo("http://best-plugin.org/best-plugin-1.1.jar");
      assertThat(bestPlugin11.getDisplayVersion()).isEqualTo("1.1 (build 42)");
      assertThat(bestPlugin11.getMinimumRequiredSonarVersion(Product.OLD_SONARQUBE)).isNull();
      assertThat(bestPlugin11.getLastRequiredSonarVersion(Product.OLD_SONARQUBE)).isNull();
      assertThat(bestPlugin11.getMinimumRequiredSonarVersion(Product.SONARQUBE_SERVER)).isEqualTo(Version.create("2039.12"));
      assertThat(bestPlugin11.getLastRequiredSonarVersion(Product.SONARQUBE_SERVER)).isEqualTo(Version.create("2039.12"));

      assertThat(clirr.getRelease(Version.create("1.1")).getDisplayVersion()).isEqualTo("1.1 (build 42)");
    }
  }

  @Test
  public void should_add_developers() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-developers.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      Plugin clirr = center.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getDevelopers()).hasSize(3);
      assertThat(clirr.getDevelopers()).contains("Mike Haller", "Freddy Mallet", "Simon Brandhof");
    }
  }

  @Test
  public void should_parse_scanner() throws IOException, URISyntaxException {
    URL url = getClass().getResource("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/splitFileFormat/nominal/update-center.properties");
    UpdateCenter center = new UpdateCenterDeserializer(Mode.DEV, false).fromManyFiles(new File(url.toURI()));
    List<Scanner> scanners = center.getScanners();
    assertThat(scanners.size()).isEqualTo(1);
    Scanner scanner = scanners.get(0);
    assertThat(scanner.getLicense()).isEqualTo("GNU LGPL 3");
  }

  @Test
  public void should_add_sources_url() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-scm.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      Plugin clirr = center.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getSourcesUrl()).isEqualTo("scm:svn:https://svn.codehaus.org/sonar-plugins/tags/sonar-clirr-plugin-1.1");
    }
  }

  @Test
  public void should_add_dependencies() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-requires-plugins.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      assertThat(pluginReferential.getUpdateCenterPluginReferential().getLastMasterReleasePlugins()).hasSize(3);

      Plugin clirr = pluginReferential.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getName()).isEqualTo("Clirr");
      List<Release> requiredReleases = new ArrayList<>(clirr.getRelease(Version.create("1.1")).getOutgoingDependencies());
      assertThat(requiredReleases).hasSize(2);
      assertThat(requiredReleases.get(0).getArtifact().getKey()).isEqualTo("foo");
      assertThat(requiredReleases.get(0).getVersion().getName()).isEqualTo("1.0");
      assertThat(requiredReleases.get(1).getArtifact().getKey()).isEqualTo("bar");
      assertThat(requiredReleases.get(1).getVersion().getName()).isEqualTo("1.1");
    }
  }

  // UPC-6
  @Test
  public void should_resolve_ranges() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-ranges.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      Plugin clirr = pluginReferential.getUpdateCenterPluginReferential().findPlugin("clirr");
      SortedSet<Version> requiredSonarVersion = clirr.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(6);
      assertThat(requiredSonarVersion.first()).hasToString("2.3");
      assertThat(requiredSonarVersion.last()).hasToString("2.7.1");

      Plugin motionchart = pluginReferential.getUpdateCenterPluginReferential().findPlugin("motionchart");
      requiredSonarVersion = motionchart.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(6);
      assertThat(requiredSonarVersion.first()).hasToString("2.2");
      assertThat(requiredSonarVersion.last()).hasToString("2.8");
      assertThat(requiredSonarVersion).doesNotContain(Version.create("2.7"));
    }
  }

  // UPC-101
  @Test
  public void should_not_allow_wildcard_at_start_of_range() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-incorrect-range-wildcard.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenterDeserializer updateCenterDeserializer = new UpdateCenterDeserializer(Mode.PROD, false);

      assertThatExceptionOfType(SonarVersionRangeException.class)
        .isThrownBy(() -> updateCenterDeserializer.fromProperties(props))
        .withMessage("Cannot use a wildcard version at the start of a range in '[2.4.*,2.6]' (in plugin 'motionchart'). " +
          "If you want to mark this range as compatible with any MAJOR.MINOR.* version, use the MAJOR.MINOR version instead (e.g.: 'sqVersions=[6.7,6.7.*]', 'sqVersions=[6.7,LATEST]').");
    }
  }

  @Test
  public void should_not_allow_LATEST_at_start_of_range() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-incorrect-range-latest.properties")) {
      Properties props = new Properties();
      props.load(input);

      UpdateCenterDeserializer updateCenterDeserializer = new UpdateCenterDeserializer(Mode.PROD, false);
      assertThatExceptionOfType(SonarVersionRangeException.class)
        .isThrownBy(() -> updateCenterDeserializer.fromProperties(props))
        .withMessage("Cannot use LATEST keyword at the start of a range in '[LATEST,LATEST]' (in plugin 'motionchart'). Use 'sqVersions=LATEST' instead.");
    }
  }

  // UPC-7
  @Test
  public void should_resolve_latest() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-latest.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      Plugin clirr = pluginReferential.getUpdateCenterPluginReferential().findPlugin("clirr");
      SortedSet<Version> requiredSonarVersion = clirr.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(7);
      assertThat(requiredSonarVersion.first()).hasToString("2.3");
      assertThat(requiredSonarVersion.last()).hasToString("2.8");

      Plugin motionchart = pluginReferential.getUpdateCenterPluginReferential().findPlugin("motionchart");
      requiredSonarVersion = motionchart.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(4);
      assertThat(requiredSonarVersion.first()).hasToString("2.4");
      assertThat(requiredSonarVersion.last()).hasToString("2.8");

      Plugin bestPlugin = pluginReferential.getUpdateCenterPluginReferential().findPlugin("bestplugin");
      requiredSonarVersion = bestPlugin.getRelease(Version.create("1.0")).getRequiredSonarVersions();
      SortedSet<Version> communitySonarVersions = bestPlugin.getRelease(Version.create("1.0")).getRequiredCommunitySonarVersions();
      assertThat(requiredSonarVersion).isEmpty();
      assertThat(communitySonarVersions.first()).hasToString("39.11");
      assertThat(communitySonarVersions.last()).hasToString("39.12");

      Plugin worstPlugin = pluginReferential.getUpdateCenterPluginReferential().findPlugin("worstplugin");
      requiredSonarVersion = worstPlugin.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      SortedSet<Version> paidSonarVersions = worstPlugin.getRelease(Version.create("1.1")).getRequiredPaidSonarVersions();
      assertThat(requiredSonarVersion).isEmpty();
      assertThat(paidSonarVersions.first()).hasToString("2039.1");
      assertThat(paidSonarVersions.last()).hasToString("2039.2");
    }
  }

  // UPC-15
  @Test
  public void should_resolve_latest_using_sonar_devVersion() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-latest-and-devVersion.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = new UpdateCenterDeserializer(Mode.DEV, false).fromProperties(props);

      Plugin clirr = pluginReferential.getUpdateCenterPluginReferential().findPlugin("clirr");
      SortedSet<Version> requiredSonarVersion = clirr.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(8);
      assertThat(requiredSonarVersion.first()).hasToString("2.3");
      assertThat(requiredSonarVersion.last()).hasToString("3.0");

      Plugin motionchart = pluginReferential.getUpdateCenterPluginReferential().findPlugin("motionchart");
      requiredSonarVersion = motionchart.getRelease(Version.create("1.1")).getRequiredSonarVersions();
      assertThat(requiredSonarVersion).hasSize(4);
      assertThat(requiredSonarVersion.first()).hasToString("2.4");
      assertThat(requiredSonarVersion.last()).hasToString("3.0");
    }
  }

  @Test
  public void should_parse_lts() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/sonar-lts.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);
      assertThat(center.getSonar().getLtsRelease().getVersion()).isEqualTo(Version.create("2.3"));
    }
  }

  @Test
  public void fromProperties_shouldParseLtaVersions() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/sonar-lts.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);
      assertThat(center.getSonar().getLtaVersion().getVersion()).isEqualTo(Version.create("2.3"));
      assertThat(center.getSonar().getPastLtaVersion().getVersion()).isEqualTo(Version.create("1.9.8"));
    }
  }

  @Test
  public void should_throw_if_lts_invalid() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/sonar-lts-invalid.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenterDeserializer updateCenterDeserializer = new UpdateCenterDeserializer(Mode.PROD, false);
      assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> updateCenterDeserializer.fromProperties(props))
        .withMessage("ltsVersion seems wrong as it is not listed in SonarQube versions");
    }
  }

  @Test
  public void should_load_when_LATEST_is_on_latest_plugin_version_and_on_private_version_Prod() throws IOException, URISyntaxException {
    URL url = getClass().getResource("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/splitFileFormat/LATEST-is-on-latest-plugin-version/update-center.properties");
    UpdateCenter center = new UpdateCenterDeserializer(Mode.PROD, false).fromManyFiles(new File(url.toURI()));

    Plugin fooPlugin = center.getUpdateCenterPluginReferential().findPlugin("foo");
    assertThat(fooPlugin.getPublicVersions()).extracting(Version::getName).containsOnly("1.0", "1.1");
  }

  @Test
  public void should_load_when_LATEST_is_on_latest_plugin_version_and_on_private_version_Dev() throws IOException, URISyntaxException {
    URL url = getClass().getResource("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/splitFileFormat/LATEST-is-on-latest-plugin-version/update-center.properties");
    UpdateCenter center = new UpdateCenterDeserializer(Mode.DEV, false).fromManyFiles(new File(url.toURI()));

    Plugin fooPlugin = center.getUpdateCenterPluginReferential().findPlugin("foo");
    assertThat(fooPlugin.getPublicVersions()).extracting(Version::getName).containsOnly("1.0", "1.1");
    assertThat(fooPlugin.getPrivateVersions()).extracting(Version::getName).containsOnly("1.2");
  }

  @Test
  public void should_throw_when_LATEST_is_another_plugin_version_then_latest() throws URISyntaxException {
    URL url = getClass().getResource(
      "/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/splitFileFormat/LATEST_is_another_plugin_version_then_latest/update-center.properties");

    UpdateCenterDeserializer updateCenterDeserializer = new UpdateCenterDeserializer(Mode.PROD, false);
    File mainFile = new File(url.toURI());

    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> updateCenterDeserializer.fromManyFiles(mainFile))
      .withMessage("Only the latest release of plugin foo may depend on LATEST SonarQube");
  }

  // UPC-29
  @Test
  public void should_load_split_format_in_dev_mode() throws IOException, URISyntaxException {
    URL url = getClass().getResource("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/splitFileFormat/nominal/update-center.properties");
    UpdateCenter center = new UpdateCenterDeserializer(Mode.DEV, false).fromManyFiles(new File(url.toURI()));
    assertThat(center.getSonar().getLtsRelease().getVersion()).isEqualTo(Version.create("3.7.1"));

    Plugin abapPlugin = center.getUpdateCenterPluginReferential().findPlugin("abap");
    assertThat(abapPlugin.getDevRelease().getVersion()).isEqualTo(Version.create("2.2.1-SNAPSHOT"));
    assertThat(abapPlugin.getIssueTrackerUrl()).isEqualTo("http://issue.tracker.url/from/properties/file");

    Plugin phpPlugin = center.getUpdateCenterPluginReferential().findPlugin("php");
    assertThat(phpPlugin.getDevRelease().getVersion()).isEqualTo(Version.create("2.3-SNAPSHOT"));
    assertThat(phpPlugin.getPublicVersions()).extracting(Version::getName).containsOnly("2.1", "2.2");
    assertThat(phpPlugin.getPrivateVersions()).extracting(Version::getName).containsOnly("2.2.1");
    assertThat(phpPlugin.getArchivedVersions()).extracting(Version::getName).containsOnly("2.0");

    Plugin ssqvPlugin = center.getUpdateCenterPluginReferential().findPlugin("ssqv");
    assertThat(ssqvPlugin.getDevRelease().getVersion()).isEqualTo(Version.create("1.1-SNAPSHOT"));
    assertThat(ssqvPlugin.getPublicVersions()).extracting(Version::getName).containsOnly("1.0", "1.1");

  }

  // UPC-29
  @Test
  public void should_load_split_format_in_prod_mode() throws IOException, URISyntaxException {
    URL url = getClass().getResource("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/splitFileFormat/nominal/update-center.properties");
    UpdateCenter center = new UpdateCenterDeserializer(Mode.PROD, false).fromManyFiles(new File(url.toURI()));
    assertThat(center.getSonar().getLtsRelease().getVersion()).isEqualTo(Version.create("3.7.1"));
    assertThat(center.getUpdateCenterPluginReferential().findPlugin("abap").getDevRelease()).isNull();
    assertThat(center.getUpdateCenterPluginReferential().findPlugin("php").getDevRelease()).isNull();
    assertThat(center.getUpdateCenterPluginReferential().findPlugin("ssqv").getDevRelease()).isNull();

    Plugin ssqvPlugin = center.getUpdateCenterPluginReferential().findPlugin("ssqv");
    assertThat(ssqvPlugin.getPublicVersions()).extracting(Version::getName).containsOnly("1.0", "1.1");
    SortedSet<Version> requiredSonarVersion10 = ssqvPlugin.getRelease(Version.create("1.0")).getRequiredSonarVersions();
    assertThat(requiredSonarVersion10).hasSize(1);
    assertThat(requiredSonarVersion10.first()).hasToString("3.7");
    assertThat(requiredSonarVersion10.last()).hasToString("3.7");

    SortedSet<Version> requiredSonarVersion11 = ssqvPlugin.getRelease(Version.create("1.1")).getRequiredSonarVersions();
    assertThat(requiredSonarVersion11).hasSize(1);
    assertThat(requiredSonarVersion11.first()).hasToString("4.0");
    assertThat(requiredSonarVersion11.last()).hasToString("4.0");

  }

  @Test
  public void should_discard_plugin_not_compatible_with_any_public_sq_versions() throws URISyntaxException, IOException {
    URL url = getClass().getResource("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/splitFileFormat/nominal/update-center.properties");
    UpdateCenter center = new UpdateCenterDeserializer(Mode.PROD, false).fromManyFiles(new File(url.toURI()));
    assertThatExceptionOfType(NoSuchElementException.class)
      .isThrownBy(() -> center.getUpdateCenterPluginReferential().findPlugin("legacyplugin").getAllReleases());
  }

  // UPC-29
  @Test
  public void should_fail_if_overlap_in_sqVersion_of_public_releases() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-overlap-sqVersion.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenterDeserializer updateCenterDeserializer = new UpdateCenterDeserializer(Mode.PROD, false);

      assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> updateCenterDeserializer.fromProperties(props))
        .withMessage("SQ version 2.7 is declared compatible with two public versions of Clirr plugin: 1.1 and 1.0");
    }
  }

  // UPC-29
  @Test
  public void should_ignore_errors() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-overlap-sqVersion.properties")) {
      Properties props = new Properties();
      props.load(input);
      new UpdateCenterDeserializer(Mode.PROD, true).fromProperties(props);
    }
  }

  // UPC-30
  @Test
  public void should_override_defaults() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-override-defaults.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter updateCenter = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      Release sonar22 = updateCenter.getSonar().getRelease("2.2", Product.OLD_SONARQUBE);
      Release sonar23 = updateCenter.getSonar().getRelease("2.3", Product.OLD_SONARQUBE);
      Release paidSonarQube = updateCenter.getSonar().getRelease("2050.3", Product.SONARQUBE_SERVER);
      assertThat(sonar22.getChangelogUrl()).isEqualTo("http://changelog");
      assertThat(sonar23.getChangelogUrl()).isEqualTo("http://changelog2.3");
      assertThat(paidSonarQube.getChangelogUrl()).isEqualTo("http://changelog2050");

      Plugin clirr = updateCenter.getUpdateCenterPluginReferential().findPlugin("clirr");
      Release clirr10 = clirr.getRelease(Version.create("1.0"));
      Release clirr11 = clirr.getRelease(Version.create("1.1"));
      assertThat(clirr10.getChangelogUrl()).isEqualTo("http://changelog");
      assertThat(clirr11.getChangelogUrl()).isEqualTo("http://changelog1.1");
    }
  }

  // UPC-29
  @Test
  public void should_fail_if_duplicate_sq_version() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/sonar-duplicate.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenterDeserializer updateCenterDeserializer = new UpdateCenterDeserializer(Mode.DEV, false);

      assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> updateCenterDeserializer.fromProperties(props))
        .withMessage("Duplicate version for SonarQube: 2.8");
    }
  }

  // UPC-29
  @Test
  public void should_fail_if_duplicate_plugin_version() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-duplicate-plugin.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenterDeserializer updateCenterDeserializer = new UpdateCenterDeserializer(Mode.DEV, false);

      assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> updateCenterDeserializer.fromProperties(props))
        .withMessage("Duplicate version for plugin clirr: 1.1");
    }
  }

  //UPC-89
  @Test
  public void should_fail_if_plugin_version_archived_and_non_archived() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-plugin-archived-and-non-archived.properties")) {
      Properties props = new Properties();
      props.load(input);
      UpdateCenterDeserializer updateCenterDeserializer = new UpdateCenterDeserializer(Mode.DEV, false);

      assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> updateCenterDeserializer.fromProperties(props))
        .withMessage("Plugin clirr: 1.0 cannot be both public and archived.");
    }
  }

  // UPC-28
  @Test
  public void test_plugin_with_only_private_and_dev_version() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/plugin-with-only-private-and-devVersion.properties")) {
      Properties props = new Properties();
      props.load(input);

      UpdateCenter updateCenterDev = new UpdateCenterDeserializer(Mode.DEV, false).fromProperties(props);

      Plugin clirr = updateCenterDev.getUpdateCenterPluginReferential().findPlugin("clirr");
      clirr.getRelease(Version.create("1.0"));
      clirr.getRelease(Version.create("1.1"));
      clirr.getRelease(Version.create("1.2-SNAPSHOT"));

      UpdateCenter updateCenterProd = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);
      PluginReferential updateCenterPluginReferential = updateCenterProd.getUpdateCenterPluginReferential();

      assertThatExceptionOfType(NoSuchElementException.class)
        .isThrownBy(() -> updateCenterPluginReferential.findPlugin("clirr"))
        .withMessage("Unable to find plugin with key clirr");
    }
  }

  // UPC-38
  @Test
  public void test_plugin_with_archived_version() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/plugin-with-archived-versions.properties")) {
      Properties props = new Properties();
      props.load(input);

      UpdateCenter updateCenter = new UpdateCenterDeserializer(Mode.PROD, false).fromProperties(props);

      Plugin clirr = updateCenter.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getRelease(Version.create("1.0")).isArchived()).isTrue();
      assertThat(clirr.getRelease(Version.create("1.1")).isArchived()).isFalse();
    }
  }

  // UPC-83
  @Test
  public void test_plugin_with_archived_version_and_include_archives_flag() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/plugin-with-archived-versions.properties")) {
      Properties props = new Properties();
      props.load(input);

      UpdateCenter updateCenter = new UpdateCenterDeserializer(Mode.PROD, false, true).fromProperties(props);

      Plugin clirr = updateCenter.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getRelease(Version.create("1.0")).isArchived()).isFalse();
      assertThat(clirr.getRelease(Version.create("1.1")).isArchived()).isFalse();
    }
  }


}
