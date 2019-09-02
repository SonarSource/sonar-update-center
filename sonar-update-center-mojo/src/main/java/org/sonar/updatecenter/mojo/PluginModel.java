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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.Version;

public class PluginModel {

  public class PluginModelVersion {
    private final Release release;

    public PluginModelVersion(Release release) {
      this.release = release;
    }

    public String getVersion() {
      return release.getVersion().getName();
    }

    public String getDate() {
      return formatDate(release.getDate());
    }

    @CheckForNull
    public String getDateAsIsoString() {
      return formatDateToISOString(release.getDate());
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

    @CheckForNull
    public String getSonarVersionRange() {
      Version latest = null;
      Release lastRelease = sonar.getLastRelease();
      if (lastRelease != null) {
        latest = lastRelease.getVersion();
      }

      if (release.getRequiredSonarVersions().isEmpty()) {
        // pathological case but still valid, where no more SQ version for this plugin
        return null;
      } else {
        Version min = release.getMinimumRequiredSonarVersion();
        Version max = release.getLastRequiredSonarVersion();

        StringBuilder sb = new StringBuilder();
        sb.append(min.toString());
        if (max.equals(latest)) {
          sb.append("+");
        } else if (!max.equals(min)) {
          sb.append(" - ").append(max.toString());
        }
        if (release.supportSonarVersion(sonar.getLtsRelease().getVersion())) {
          sb.append(" (Compatible with LTS)");
        }
        return sb.toString();
      }
    }

    @CheckForNull
    private String formatDateToISOString( @Nullable Date date) {
      return date != null ? (new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)).format(date) : null;
    }

    private String formatDate(Date date) {
      return (new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)).format(date);
    }

  }

  private Plugin plugin;
  private Sonar sonar;

  public PluginModel(Plugin plugin, Sonar sonar) {
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

  public String getCategory() {
    return plugin.getCategory();
  }

  public List<PluginModelVersion> getAllVersions() {
    List<PluginModelVersion> result = new LinkedList<>();
    for (Release r : plugin.getAllReleases()) {
      // Add in reverse order to have greater version on top
      result.add(0, new PluginModelVersion(r));
    }
    return result;
  }

  public boolean isSupportedBySonarSource() {
    return plugin.isSupportedBySonarSource();
  }

  public boolean isSonarSourceCommercialPlugin() {
    return isSupportedBySonarSource() && StringUtils.isNotEmpty(plugin.getTermsConditionsUrl());
  }
}
