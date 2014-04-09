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

import org.apache.commons.lang.StringUtils;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.Version;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PluginHeader {

  public class PluginHeaderVersion {
    private final Release release;

    public PluginHeaderVersion(Release release) {
      this.release = release;
    }

    public String getVersion() {
      return release.getVersion().getName();
    }

    public String getDate() {
      return formatDate(release.getDate());
    }

    public String getDownloadUrl() {
      return release.getDownloadUrl();
    }

    public String getSonarVersion() {
      return release.getMinimumRequiredSonarVersion().getName();
    }

    public boolean compatibleWithLts() {
      String lts = getSonarLtsVersion();
      if (lts == null) {
        throw new IllegalStateException("Unable to determine if plugin is compatible wth LTS as LTS version is not defined");
      }
      return release.supportSonarVersion(Version.create(lts));
    }
  }

  private Plugin plugin;
  private Sonar sonar;

  public PluginHeader(Plugin plugin, Sonar sonar) {
    this.plugin = plugin;
    this.sonar = sonar;
  }

  public String getName() {
    return plugin.getName();
  }

  public String getIssueTracker() {
    return formatLink(plugin.getIssueTrackerUrl());
  }

  public String getSources() {
    return formatLink(plugin.getSourcesUrl());
  }

  public String getLicense() {
    return plugin.getLicense();
  }

  public String getDevelopers() {
    return formatDevelopers(plugin.getDevelopers());
  }

  private static String formatLink(String url) {
    return StringUtils.isNotBlank(url) ? "<a href=\"" + url + "\" target=\"_top\">" + url + "</a>" : null;
  }

  private static String formatDate(Date date) {
    return (new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)).format(date);
  }

  private String formatDevelopers(List<String> developers) {
    if (developers == null || developers.isEmpty()) {
      return null;
    }
    return StringUtils.join(developers, ", ");
  }

  public PluginHeaderVersion getLatestVersion() {
    return new PluginHeaderVersion(plugin.getLastRelease());
  }

  public PluginHeaderVersion getLtsVersion() {
    if (sonar.getLtsRelease() == null) {
      return null;
    }
    Release lastCompatibleWithLts = plugin.getLastCompatibleRelease(sonar.getLtsRelease().getVersion());
    if (lastCompatibleWithLts != null && !plugin.getLastRelease().equals(lastCompatibleWithLts)) {
      return new PluginHeaderVersion(lastCompatibleWithLts);
    }
    return null;
  }

  public String getSonarLtsVersion() {
    return sonar.getLtsRelease() != null ? sonar.getLtsRelease().getVersion().toString() : null;
  }
}
