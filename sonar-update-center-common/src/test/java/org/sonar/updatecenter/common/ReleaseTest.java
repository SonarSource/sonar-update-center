/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2022 SonarSource SA
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
    release.addRequiredSonarVersions("2.0");
    assertThat(release.getRequiredSonarVersions()).containsOnly(Version.create("2.0"));

    release.addRequiredSonarVersions((String[]) null);
    assertThat(release.getRequiredSonarVersions()).hasSize(1);

    release.addRequiredSonarVersions((Version[]) null);
    assertThat(release.getRequiredSonarVersions()).hasSize(1);
  }

  @Test
  public void should_return_last_required_sonar_version() {
    Release release = new Release(Plugin.factory("squid"), "1.0");
    release.addRequiredSonarVersions("2.1", "1.9", "2.0");
    assertThat(release.getLastRequiredSonarVersion()).isEqualTo(Version.create("2.1"));

    Release squid10 = new Release(Plugin.factory("squid"), "1.0");
    assertThat(squid10.getLastRequiredSonarVersion()).isNull();
  }

  @Test
  public void should_return_minimum_required_sonar_version() {
    Release release = new Release(Plugin.factory("squid"), "1.0");
    release.addRequiredSonarVersions("2.1", "1.9", "2.0");
    assertThat(release.getMinimumRequiredSonarVersion()).isEqualTo(Version.create("1.9"));

    Release squid10 = new Release(Plugin.factory("squid"), "1.0");
    assertThat(squid10.getMinimumRequiredSonarVersion()).isNull();
  }

  @Test
  public void test_equal() {
    Release squid10 = new Release(Plugin.factory("squid"), "1.0");
    Release squid10bis = new Release(Plugin.factory("squid"), "1.0");
    Release squid20 = new Release(Plugin.factory("squid"), "2.0");

    Release bar10 = new Release(Plugin.factory("bar"), "1.0");

    assertThat(squid10).isEqualTo(squid10bis).isNotEqualTo(squid20).isNotEqualTo(bar10);
  }

  @Test
  public void should_have_version_from_string() {
    Release release = new Release(Plugin.factory("squid"), "1.0");
    release.addRequiredSonarVersions("2.1", "1.9", "2.0");
    assertThat(release.getRequiredSonarVersions()).hasSize(3);
    assertThat(release.getSonarVersionFromString("mystring")).isEmpty();

    release.addRequiredSonarVersions(Version.create("3.0", "mystring"));
    Version[] sqVersions = release.getSonarVersionFromString("mystring");
    assertThat(sqVersions).hasSize(1);
    assertThat(sqVersions[0]).isEqualTo(Version.create("3.0"));
  }

  @Test
  public void should_allow_display_version() {
    Release release = new Release(Plugin.factory("squid"), "1.0");
    String displayVersion = "1.0 (build 1234)";
    assertThat(release.setDisplayVersion(displayVersion).getDisplayVersion()).isEqualTo(displayVersion);

  }

}
