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

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ReleaseTest {

  @Test
  public void should_contain_filename() {
    Release release = new Release(new Plugin("fake"), Version.create("1.2"));
    assertThat(release.getFilename()).isNull();

    release.setDownloadUrl("http://dist.sonarsource.org/foo-1.2.jar");
    assertThat(release.getFilename()).isEqualTo("foo-1.2.jar");
  }

  @Test
  public void should_add_dependencies() {
    Release release = new Release(new Plugin("fake"), Version.create("1.2"));
    release.addOutgoingDependency(new Release(new Plugin("foo"), Version.create("1.0")));
    assertThat(release.getOutgoingDependencies()).hasSize(1);

    release.addIncomingDependency(new Release(new Plugin("fake2"), Version.create("1.2")));
    assertThat(release.getIncomingDependencies()).hasSize(1);
  }

  @Test
  public void should_add_required_sonar_versions(){
    Release release = new Release(new Plugin("squid"), "1.0");
    release.addRequiredSonarVersions("2.0");
    assertThat(release.getRequiredSonarVersions()).containsOnly(Version.create("2.0"));

    assertThat(release.getRequiredSonarVersions()).containsOnly(Version.create("2.0"));

    release.addRequiredSonarVersions((String[]) null);
    assertThat(release.getRequiredSonarVersions()).hasSize(1);

    release.addRequiredSonarVersions((Version[]) null);
    assertThat(release.getRequiredSonarVersions()).hasSize(1);
  }

  @Test
  public void should_return_last_required_sonar_version(){
    Release release = new Release(new Plugin("squid"), "1.0");
    release.addRequiredSonarVersions("2.1", "1.9", "2.0");
    assertThat(release.getLastRequiredSonarVersion()).isEqualTo(Version.create("2.1"));

    Release squid10 = new Release(new Plugin("squid"), "1.0");
    assertThat(squid10.getLastRequiredSonarVersion()).isNull();
  }

  @Test
  public void should_return_minimum_required_sonar_version(){
    Release release = new Release(new Plugin("squid"), "1.0");
    release.addRequiredSonarVersions("2.1", "1.9", "2.0");
    assertThat(release.getMinimumRequiredSonarVersion()).isEqualTo(Version.create("1.9"));

    Release squid10 = new Release(new Plugin("squid"), "1.0");
    assertThat(squid10.getMinimumRequiredSonarVersion()).isNull();
  }

  @Test
  public void test_equal(){
    Release squid10 = new Release(new Plugin("squid"), "1.0");
    Release squid10bis = new Release(new Plugin("squid"), "1.0");
    Release squid20 = new Release(new Plugin("squid"), "2.0");

    Release bar10 = new Release(new Plugin("bar"), "1.0");

    assertThat(squid10).isEqualTo(squid10bis);
    assertThat(squid10).isNotEqualTo(squid20);
    assertThat(squid10).isNotEqualTo(bar10);
  }

  @Test
  public void should_have_version_from_string(){
    Release release = new Release(new Plugin("squid"), "1.0");
    release.addRequiredSonarVersions("2.1", "1.9", "2.0");
    assertThat(release.getRequiredSonarVersions()).hasSize(3);
    assertThat(release.getSonarVersionFromString("mystring") ).hasSize(0);

    release.addRequiredSonarVersions( Version.create("3.0", "mystring"));
    assertThat(release.getSonarVersionFromString("mystring") ).hasSize(1);
    // TODO check 3.0
  }

}
