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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReleaseTest {

  @Test
  public void should_contain_filename() {
    Release release = new Release(Plugin.factory("fake"), Version.create("1.2"));
    assertThat(release.getFilename()).isNull();

    release.setDownloadUrl("http://dist.sonarsource.org/foo-1.2.jar");
    assertThat(release.getFilename()).isEqualTo("foo-1.2.jar");

    release.setDownloadUrl("http://dist.sonarsource.org/foo-1.3.jar", Release.Edition.ENTERPRISE);
    assertThat(release.getFilename(Release.Edition.COMMUNITY)).isEqualTo("foo-1.2.jar");
    assertThat(release.getFilename(Release.Edition.ENTERPRISE)).isEqualTo("foo-1.3.jar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_throw_if_badUrl() {
    Release release = new Release(Plugin.factory("fake"), Version.create("1.2"));
    release.setChangelogUrl("badurl");
  }

  @Test
  public void should_add_dependencies() {
    Release release = new Release(Plugin.factory("fake"), Version.create("1.2"));
    release.addOutgoingDependency(new Release(Plugin.factory("foo"), Version.create("1.0")));
    assertThat(release.getOutgoingDependencies()).hasSize(1);

    release.addIncomingDependency(new Release(Plugin.factory("fake2"), Version.create("1.2")));
    assertThat(release.getIncomingDependencies()).hasSize(1);
  }

  @Test
  public void should_add_required_sonar_versions() {
    Release release = new Release(Plugin.factory("squid"), "1.0");
    release.addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.0");
    assertThat(release.getRequiredSonarVersions()).containsOnly(Version.create("2.0"));

    release.addRequiredSonarVersions(Product.OLD_SONARQUBE, (String[]) null);
    assertThat(release.getRequiredSonarVersions()).hasSize(1);

    release.addRequiredSonarVersions(Product.OLD_SONARQUBE, (Version[]) null);
    assertThat(release.getRequiredSonarVersions()).hasSize(1);
  }

  @Test
  public void should_return_last_required_sonar_version() {
    Release release = new Release(Plugin.factory("squid"), "1.0");
    release.addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1", "1.9", "2.0");
    assertThat(release.getLastRequiredSonarVersion(Product.OLD_SONARQUBE)).isEqualTo(Version.create("2.1"));

    Release squid10 = new Release(Plugin.factory("squid"), "1.0");
    assertThat(squid10.getLastRequiredSonarVersion(Product.OLD_SONARQUBE)).isNull();
  }

  @Test
  public void should_return_minimum_required_sonar_version() {
    Release release = new Release(Plugin.factory("squid"), "1.0");
    release.addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1", "1.9", "2.0");
    assertThat(release.getMinimumRequiredSonarVersion(Product.OLD_SONARQUBE)).isEqualTo(Version.create("1.9"));

    Release squid10 = new Release(Plugin.factory("squid"), "1.0");
    assertThat(squid10.getMinimumRequiredSonarVersion(Product.OLD_SONARQUBE)).isNull();
  }

  @Test
  public void test_equal() {
    Release squid10 = new Release(Plugin.factory("squid"), "1.0");
    Release squid10bis = new Release(Plugin.factory("squid"), "1.0");
    Release squid20 = new Release(Plugin.factory("squid"), "2.0");

    Release bar10 = new Release(Plugin.factory("bar"), "1.0");

    assertThat(squid10).isEqualTo(squid10bis);
    assertThat(squid10).isNotEqualTo(squid20);
    assertThat(squid10).isNotEqualTo(bar10);
  }

  @Test
  public void should_have_version_from_string() {
    Release release = new Release(Plugin.factory("squid"), "1.0");
    release.addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1", "1.9", "2.0");
    assertThat(release.getRequiredSonarVersions()).hasSize(3);
    assertThat(release.getSonarVersionFromString(Product.OLD_SONARQUBE, "mystring")).isEmpty();

    release.addRequiredSonarVersions(Product.OLD_SONARQUBE, Version.create("3.0", "mystring"));
    Collection<Version> sqVersions = release.getSonarVersionFromString(Product.OLD_SONARQUBE, "mystring");
    assertThat(sqVersions).hasSize(1);
    assertThat(sqVersions.iterator().next()).isEqualTo(Version.create("3.0"));
  }

  @Test
  public void should_allow_display_version() {
    Release release = new Release(Plugin.factory("squid"), "1.0");
    String displayVersion = "1.0 (build 1234)";
    assertThat(release.setDisplayVersion(displayVersion).getDisplayVersion()).isEqualTo(displayVersion);
  }

  @Test
  public void supportSonarVersion_whenPaidSonarQubeSupported_shouldReturnTrue() {
    Release release = new Release(Plugin.factory("squid"), "1.0");

    release.addRequiredSonarVersions(Product.SONARQUBE_SERVER, "2.1", "1.9", "2.0");

    assertThat(release.supportSonarVersion(Version.create("2.1"), Product.SONARQUBE_SERVER)).isTrue();
    assertThat(release.supportSonarVersion(Version.create("2.1"), Product.OLD_SONARQUBE)).isFalse();
    assertThat(release.supportSonarVersion(Version.create("2.1"), Product.SONARQUBE_COMMUNITY_BUILD)).isFalse();
  }

  @Test
  public void getLastRequiredSonarVersion_testDifferentProducts() {
    Release release = new Release(Plugin.factory("squid"), "1.0");

    release.addRequiredSonarVersions(Product.SONARQUBE_SERVER, "1", "2", "3");
    release.addRequiredSonarVersions(Product.SONARQUBE_COMMUNITY_BUILD, "4", "5", "6");

    assertThat(release.getLastRequiredSonarVersion(Product.SONARQUBE_SERVER)).isEqualTo(Version.create("3"));
    assertThat(release.getLastRequiredSonarVersion(Product.SONARQUBE_COMMUNITY_BUILD)).isEqualTo(Version.create("6"));
    assertThat(release.getLastRequiredSonarVersion(Product.OLD_SONARQUBE)).isNull();
  }

  @Test
  public void getMinimumRequiredSonarVersion_testDifferentProducts() {
    Release release = new Release(Plugin.factory("squid"), "1.0");

    release.addRequiredSonarVersions(Product.SONARQUBE_SERVER, "1", "2", "3");
    release.addRequiredSonarVersions(Product.SONARQUBE_COMMUNITY_BUILD, "4", "5", "6");

    assertThat(release.getMinimumRequiredSonarVersion(Product.SONARQUBE_SERVER)).isEqualTo(Version.create("1"));
    assertThat(release.getMinimumRequiredSonarVersion(Product.SONARQUBE_COMMUNITY_BUILD)).isEqualTo(Version.create("4"));
    assertThat(release.getMinimumRequiredSonarVersion(Product.OLD_SONARQUBE)).isNull();
  }

  @Test
  public void getSonarVersionFromString_testDifferentProducts() {
    Release release = new Release(Plugin.factory("squid"), "1.0");

    release.addRequiredSonarVersions(Product.SONARQUBE_SERVER, "1.0.0");
    release.addRequiredSonarVersions(Product.SONARQUBE_COMMUNITY_BUILD, "4.0.0", "5.0.0");
    release.addRequiredSonarVersions(Product.OLD_SONARQUBE, "9.0.0");

    Set<Version> expectedPaidVersion = new HashSet<>();
    expectedPaidVersion.add(Version.create("1.0.0"));

    Set<Version> expectedCommunityVersions = new HashSet<>();
    expectedCommunityVersions.add(Version.create("4.0.0"));

    Set<Version> expectedOldVersions = new HashSet<>();
    expectedOldVersions.add(Version.create("9.0.0"));

    assertThat(release.getSonarVersionFromString(Product.SONARQUBE_SERVER, "1.0.0")).isEqualTo(expectedPaidVersion);
    assertThat(release.getSonarVersionFromString(Product.SONARQUBE_COMMUNITY_BUILD, "4.0.0")).isEqualTo(expectedCommunityVersions);
    assertThat(release.getSonarVersionFromString(Product.OLD_SONARQUBE, "9.0.0")).isEqualTo(expectedOldVersions);
  }

}
