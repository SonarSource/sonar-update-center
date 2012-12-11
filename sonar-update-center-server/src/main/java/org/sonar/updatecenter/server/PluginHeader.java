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
    return plugin.getLicense() != null ? plugin.getLicense() : "";
  }

  public String getDevelopers() {
    return formatDevelopers(plugin.getDevelopers());
  }

  private String formatLink(String url) {
    return StringUtils.isNotBlank(url) ? "<a href=\"" + url + "\" target=\"_top\">" + url + "</a>" : "";
  }

  private String formatDate(Date date) {
    return (new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)).format(date);
  }

  private String formatDevelopers(List<String> developers) {
    if (developers == null || developers.isEmpty()) {
      return "";
    }
    return StringUtils.join(developers, ", ");
  }

}
