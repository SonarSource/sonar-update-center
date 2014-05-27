/*
 * SonarSource :: Update Center :: Maven Plugin
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
package org.sonar.updatecenter.mojo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompatibilityMatrix {

  private List<SQVersion> sqVersions = new ArrayList<CompatibilityMatrix.SQVersion>();
  private List<Plugin> plugins = new ArrayList<CompatibilityMatrix.Plugin>();

  public List<SQVersion> getSqVersions() {
    return sqVersions;
  }

  public List<Plugin> getPlugins() {
    return plugins;
  }

  public static class SQVersion {
    private final String version;
    private final boolean isLts;
    private final Date releaseDate;

    public SQVersion(String version, boolean isLts, Date releaseDate) {
      this.version = version;
      this.isLts = isLts;
      this.releaseDate = releaseDate;
    }

    public String getVersion() {
      return version;
    }

    public boolean isLts() {
      return isLts;
    }

    public Date getReleaseDate() {
      return releaseDate;
    }
  }

  public static class Plugin {

    private final String name;
    private final String homepageUrl;
    private final Map<String, String> compatibleVersionBySqVersion = new HashMap<String, String>();

    public Plugin(String name, String homepageUrl) {
      this.name = name;
      this.homepageUrl = homepageUrl;
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

  }

}
