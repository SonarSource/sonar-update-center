/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2024 SonarSource SA
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.annotation.Nullable;

public class SonarVersionModel {
  private final String realVersion;
  private final String displayVersion;
  private final boolean isLta;
  @Nullable
  private final Date releaseDate;

  public SonarVersionModel(String realVersion, String displayVersion, @Nullable Date releaseDate, boolean isLta) {
    this.realVersion = realVersion;
    this.isLta = isLta;
    this.displayVersion = displayVersion;
    this.releaseDate = releaseDate;
  }

  /**
   * @return Version with major and minor numbers. Example: 6.7
   */
  public String getDisplayVersion() {
    return displayVersion;
  }

  /**
   * @return The precise version of the release. Example: 6.7.4
   */
  public String getRealVersion() {
    return realVersion;
  }

  public boolean isLta() {
    return isLta;
  }

  public String getReleaseDate() {
    return releaseDate != null ? formatDate(releaseDate) : null;
  }

  private static String formatDate(Date date) {
    return (new SimpleDateFormat("MMM yyyy", Locale.ENGLISH)).format(date);
  }
}
