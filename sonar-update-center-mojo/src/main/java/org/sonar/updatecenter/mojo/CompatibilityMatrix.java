/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CompatibilityMatrix {

  private List<SQVersion> sqVersions = new ArrayList<>();
  private List<Plugin> plugins = new ArrayList<>();

  public List<SQVersion> getSqVersions() {
    return sqVersions;
  }

  public List<Plugin> getPlugins() {
    return plugins;
  }

  public static class SQVersion {
    private final String displayVersion;
    private final String realVersion;
    private final boolean isLts;
    private final Date releaseDate;

    public SQVersion(String displayVersion, String realVersion, boolean isLts, Date releaseDate) {
      this.displayVersion = displayVersion;
      this.realVersion = realVersion;
      this.isLts = isLts;
      this.releaseDate = releaseDate;
    }

    public String getDisplayVersion() {
      return displayVersion;
    }

    public String getRealVersion() {
      return realVersion;
    }

    public boolean isLts() {
      return isLts;
    }

    public String getReleaseDate() {
      return releaseDate != null ? formatDate(releaseDate) : null;
    }

    private static String formatDate(Date date) {
      return (new SimpleDateFormat("MMM yyyy", Locale.ENGLISH)).format(date);
    }
  }

  public static class Plugin {

    private final String name;
    private final String homepageUrl;
    private final Map<String, String> compatibleVersionBySqVersion = new HashMap<>();

    private final boolean isSupportedBySonarSource;

    public Plugin(String name, String homepageUrl, boolean isSupportedBySonarSource) {
      this.name = name;
      this.homepageUrl = homepageUrl;
      this.isSupportedBySonarSource = isSupportedBySonarSource;
    }

    public String getName() {
      return name;
    }

    public String getHomepageUrl() {
      return homepageUrl;
    }

    public Map<String, String> getCompatibleVersionBySqVersion() {
      return compatibleVersionBySqVersion;
    }

    public boolean supports(String sqVersion) {
      return compatibleVersionBySqVersion.containsKey(sqVersion);
    }

    public String supportedVersion(String sqVersion) {
      return compatibleVersionBySqVersion.get(sqVersion);
    }

    public boolean isSupportedBySonarSource() {
      return isSupportedBySonarSource;
    }
  }

}
