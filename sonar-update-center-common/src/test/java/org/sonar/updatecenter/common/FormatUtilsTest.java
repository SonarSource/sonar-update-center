/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.junit.Test;

import java.text.ParseException;

import static org.fest.assertions.Assertions.assertThat;

public class FormatUtilsTest {

  @Test
  public void testToDate() throws ParseException {
    assertThat(FormatUtils.toDate("2010-05-18", false).getDate()).isEqualTo(18);
  }

  @Test
  public void ignoreNullDate() {
    assertThat(FormatUtils.toDate(null, true)).isNull();
  }
}
