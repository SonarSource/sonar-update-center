/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2025 SonarSource Sàrl
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

import java.util.Calendar;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import java.text.ParseException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class FormatUtilsTest {

  @Test
  public void test_to_date() {
    Date date = FormatUtils.toDate("2010-05-18");
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);

    assertThat(cal.get(Calendar.DAY_OF_MONTH)).isEqualTo(18);
  }

  @Test
  public void test_to_date_time() {
    Date date = FormatUtils.toDateTime("2010-05-18T10:30:00+0000");
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);

    assertThat(cal.get(Calendar.DAY_OF_MONTH)).isEqualTo(18);
  }

  @Test
  public void ignore_null_and_empty_date() {
    assertThat(FormatUtils.toDate(null)).isNull();
    assertThat(FormatUtils.toDate("")).isNull();
  }

  @Test
  public void ignore_null_and_empty_date_time() {
    assertThat(FormatUtils.toDateTime(null)).isNull();
    assertThat(FormatUtils.toDateTime("")).isNull();
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_throw_exception_on_invalid_date_format() {
    FormatUtils.toDate("2010");
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_throw_exception_on_invalid_date_time_format() {
    FormatUtils.toDateTime("2010");
  }

  @Test
  public void test_to_date_string() throws ParseException {
    Date date = DateUtils.parseDate("2010-05-18", "yyyy-MM-dd");
    assertThat(FormatUtils.toDateString(date)).isEqualTo("2010-05-18");
  }

  @Test
  public void test_to_date_time_string() throws ParseException {
    Date date = DateUtils.parseDate("2010-05-18 10:30:00", "yyyy-MM-dd HH:mm:ss");
    assertThat(FormatUtils.toDateTimeString(date)).isNotNull();
  }

  @Test
  public void should_return_null_if_no_date() {
    assertThat(FormatUtils.toDateString(null)).isNull();
    assertThat(FormatUtils.toDateTimeString(null)).isNull();
  }

}
