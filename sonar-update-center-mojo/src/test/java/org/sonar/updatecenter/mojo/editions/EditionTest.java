/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2017 SonarSource SA
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
package org.sonar.updatecenter.mojo.editions;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class EditionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void build_edition() throws Exception {
    Edition.Builder builder = new Edition.Builder();
    builder.setKey("enterprise");
    builder.setName("Enterprise");
    builder.setTextDescription("Enterprise Edition");
    builder.setSonarQubeVersion("6.7.1");
    builder.setHomeUrl("/home");
    builder.setRequestUrl("/request");
    builder.setZipFileName("enterprise.zip");
    builder.addJar("foo.jar");
    builder.addJar("bar.jar");

    Edition edition = builder.build();

    assertThat(edition.getKey()).isEqualTo("enterprise");
    assertThat(edition.getName()).isEqualTo("Enterprise");
    assertThat(edition.getTextDescription()).isEqualTo("Enterprise Edition");
    assertThat(edition.getHomeUrl()).isEqualTo("/home");
    assertThat(edition.getRequestUrl()).isEqualTo("/request");
    assertThat(edition.getSonarQubeVersion()).isEqualTo("6.7.1");
    assertThat(edition.getZipFileName()).isEqualTo("enterprise.zip");
  }

  @Test
  public void test_equals_and_hashCode() throws Exception {
    Edition foo = newEdition("foo");
    Edition foo1 = newEdition("foo");
    Edition bar = newEdition("bar");

    assertThat(foo.equals(foo)).isTrue();
    assertThat(foo.equals(foo1)).isTrue();
    assertThat(foo.equals(bar)).isFalse();
    assertThat(foo.equals("foo")).isFalse();
    assertThat(foo.equals(null)).isFalse();

    assertThat(foo.hashCode()).isEqualTo(foo.hashCode());
    assertThat(foo.hashCode()).isEqualTo(foo1.hashCode());
  }

  private Edition newEdition(String key) throws IOException {
    return new Edition.Builder()
      .setKey(key)
      .setName(key + "_name")
      .setTextDescription(key + " Edition")
      .setSonarQubeVersion("6.7")
      .setHomeUrl(key + "/home")
      .setRequestUrl(key + "/request")
      .setZipFileName(key + ".zip")
      .build();
  }
}
