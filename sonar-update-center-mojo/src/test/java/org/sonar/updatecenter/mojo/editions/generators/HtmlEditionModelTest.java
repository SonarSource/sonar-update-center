/*
 * SonarSource :: Update Center :: Maven Plugin
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
package org.sonar.updatecenter.mojo.editions.generators;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class HtmlEditionModelTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void test_getters() {
    HtmlEditionModel model = new HtmlEditionModel("name");
    model.add("6.7", "6.7-url");
    model.add("6.8", "6.8-url");

    assertThat(model.getName()).isEqualTo("name");
    assertThat(model.supports("6.7")).isTrue();
    assertThat(model.supports("6.7.1")).isFalse();
    assertThat(model.supports("6.8")).isTrue();
    assertThat(model.getDownloadUrlForSQVersion("6.7")).isEqualTo("6.7-url");
    assertThat(model.getDownloadUrlForSQVersion("6.8")).isEqualTo("6.8-url");
  }

  @Test
  public void throw_exception_getDownloadUrlForSQVersion_if_version_not_supported() {
    HtmlEditionModel model = new HtmlEditionModel("name");
    model.add("6.7", "6.7-url");

    exception.expect(IllegalArgumentException.class);
    model.getDownloadUrlForSQVersion("6.8");
  }
}
