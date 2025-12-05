/*
 * SonarSource :: Update Center :: Maven Plugin
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
package org.sonar.updatecenter.mojo;

import org.sonar.updatecenter.common.Product;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.Version;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReleaseModel {

  private final Release release;
  private final Sonar sonar;

  public ReleaseModel(Release release, Sonar sonar) {
    this.release = release;
    this.sonar = sonar;
  }

  @CheckForNull
  private static String formatDateToISOString(@Nullable Date date) {
    return date != null ? (new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)).format(date) : null;
  }

  @CheckForNull
  private static String formatDate(@Nullable Date date) {
    return date != null ? (new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)).format(date) : null;
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

  public List<LabeledUrl> getScannerDownloadUrl() {
    List<Map.Entry<String, URL>> listUrl = release.getScannerDownloadUrl();
    ArrayList<LabeledUrl> outputUrl = new ArrayList<>();
    for (Map.Entry<String, URL> flavor : listUrl) {
      outputUrl.add(new LabeledUrl(release.getFlavorLabel(flavor.getKey()), flavor.getValue().toString()));
    }
    return outputUrl;
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

  /**
   * TODO https://sonarsource.atlassian.net/browse/UPC-145
   */
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
      Version min = release.getMinimumRequiredSonarVersion(Product.OLD_SONARQUBE);
      Version max = release.getLastRequiredSonarVersion(Product.OLD_SONARQUBE);

      StringBuilder sb = new StringBuilder();
      sb.append(min.toString());
      if (max.equals(latest)) {
        sb.append("+");
      } else if (!max.equals(min)) {
        sb.append(" - ").append(max);
      }
      if (release.supportSonarVersion(sonar.getLtaVersion().getVersion(), Product.OLD_SONARQUBE)) {
        sb.append(" (Compatible with LTS)");
      }
      return sb.toString();
    }
  }

  public static class LabeledUrl {
    private String label;
    private String url;

    public LabeledUrl(@Nullable String label, String url) {
      this.label = label;
      this.url = url;
    }

    public String getLabel() {
      return label;
    }

    public String getUrl() {
      return url;
    }
  }

}
