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
package org.sonar.updatecenter.server;

import org.apache.commons.lang.StringUtils;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PluginHeader {

  private Plugin plugin;

  public PluginHeader(Plugin plugin) {
    this.plugin = plugin;
  }

  private Release getRelease() {
    return plugin.getLastRelease();
  }

  public String getName() {
    return plugin.getName();
  }

  public String getVersion() {
    return getRelease().getVersion().getName();
  }

  public String getDate() {
    return formatDate(getRelease().getDate());
  }

  public String getDownloadUrl() {
    return getRelease().getDownloadUrl();
  }

  public String getSonarVersion() {
    return getRelease().getMinimumRequiredSonarVersion().getName();
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

  private String formatLink(String url) {
    return StringUtils.isNotBlank(url) ? "<a href=\"" + url + "\" target=\"_top\">" + url + "</a>" : null;
  }

  private String formatDate(Date date) {
    return (new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)).format(date);
  }

  private String formatDevelopers(List<String> developers) {
    if (developers == null || developers.isEmpty()) {
      return null;
    }
    return StringUtils.join(developers, ", ");
  }

}
