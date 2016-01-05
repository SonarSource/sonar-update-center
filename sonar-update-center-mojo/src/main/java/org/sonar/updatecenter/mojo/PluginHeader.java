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

import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.Version;

import javax.annotation.CheckForNull;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
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

    public String getChangelogUrl() {
      return release.getChangelogUrl();
    }

    public String getDescription() {
      return release.getDescription();
    }

    public boolean isArchived() {
      return release.isArchived();
    }

    public String getSonarVersionRange() {
      String min = release.getMinimumRequiredSonarVersion().toString();
      String max = release.getLastRequiredSonarVersion().toString();
      String latest = null;
      Release lastRelease = sonar.getLastRelease();
      if (lastRelease != null) {
        latest = lastRelease.getVersion().toString();
      }
      StringBuilder sb = new StringBuilder();
      sb.append(min);
      if (max.equals(latest)) {
        sb.append("+");
      } else if (!max.equals(min)) {
        sb.append(" - ").append(max);
      }
      if (release.supportSonarVersion(sonar.getLtsRelease().getVersion())) {
        sb.append(" (LTS)");
      }
      return sb.toString();
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

  public String getKey() {
    return plugin.getKey();
  }

  public String getName() {
    return plugin.getName();
  }

  public String getIssueTracker() {
    return plugin.getIssueTrackerUrl();
  }

  public String getSources() {
    return plugin.getSourcesUrl();
  }

  public String getLicense() {
    return plugin.getLicense();
  }

  public String getOrganization() {
    return plugin.getOrganization();
  }

  public String getOrganizationUrl() {
    return plugin.getOrganizationUrl();
  }

  private static String formatDate(Date date) {
    return (new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)).format(date);
  }

  public List<PluginHeaderVersion> getAllVersions() {
    List<PluginHeaderVersion> result = new LinkedList<>();
    for (Release r : plugin.getAllReleases()) {
      // Add in reverse order to have greater version on top
      result.add(0, new PluginHeaderVersion(r));
    }
    return result;
  }

  public int getNbVersions() {
    return plugin.getAllReleases().size();
  }

  public boolean isSupportedBySonarSource() {
    return plugin.isSupportedBySonarSource();
  }

  @CheckForNull
  public String getLastVersionString() {
    Release lastRelease = plugin.getLastRelease();
    if (lastRelease != null) {
      return lastRelease.getVersion().getName();
    } else {
      return null;
    }
  }

  public String getSonarLtsVersion() {
    return sonar.getLtsRelease() != null ? sonar.getLtsRelease().getVersion().toString() : null;
  }
}
