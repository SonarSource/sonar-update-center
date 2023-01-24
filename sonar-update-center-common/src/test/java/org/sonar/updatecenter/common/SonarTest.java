/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2023 SonarSource SA
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

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarTest {

  @Test
  public void should_get_declared_releases() {
    Sonar sonar = new Sonar().setReleases(new String[]{"3.1", "3.2"});

    assertThat(sonar.getReleases()).containsOnly(new Release(sonar, "3.1"), new Release(sonar, "3.2"));
    assertThat(sonar.getRelease(Version.create("3.1"))).isNotNull();
    assertThat(sonar.getRelease(Version.create("3.2"))).isNotNull();
    assertThat(sonar.getVersions()).containsOnly(Version.create("3.1"), Version.create("3.2"));
  }

  @Test(expected = NoSuchElementException.class)
  public void should_not_get_undeclared_releases() {
    Sonar sonar = new Sonar().setReleases(new String[]{"3.1", "3.2"});

    sonar.getRelease(Version.create("3.3"));
  }

  @Test
  public void sonar_key() {
    assertThat(new Sonar().getKey()).isEqualTo("sonar");
  }

}
