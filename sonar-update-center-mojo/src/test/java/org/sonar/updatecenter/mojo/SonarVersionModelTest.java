/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2019 SonarSource SA
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
package org.sonar.updatecenter.mojo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarVersionModelTest {
  @Test
  public void testGetters() {
    LocalDate localDate = LocalDate.of(2017, 11, 15);
    Instant instant = localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
    Date date = Date.from(instant);
    SonarVersionModel model = new SonarVersionModel("6.7.1", "6.7", date, true);

    assertThat(model.getDisplayVersion()).isEqualTo("6.7");
    assertThat(model.getRealVersion()).isEqualTo("6.7.1");
    assertThat(model.getReleaseDate()).isEqualTo("Nov 2017");
    assertThat(model.isLts()).isTrue();
  }
}
