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

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PluginsHtmlHeader {

  private static final Logger LOG = LoggerFactory.getLogger(PluginsHtmlHeader.class);

  private File outputDirectory;
  private String pluginInfoWidgetTemplate;
  private UpdateCenter center;

  public PluginsHtmlHeader(UpdateCenter center, File outputDirectory) throws IOException {
    this.outputDirectory = outputDirectory;
    this.center = center;
  }

  private void init() throws IOException {
    Preconditions.checkArgument(outputDirectory.exists());
    InputStream inputStream = null;
    try {
      inputStream = getClass().getResourceAsStream("/plugin-info-widget-template.html");
      pluginInfoWidgetTemplate = IOUtils.toString(inputStream);
      FileUtils.copyURLToFile(getClass().getResource("/style.css"), new File(outputDirectory, "style.css"));
    } catch (Exception e) {
      IOUtils.closeQuietly(inputStream);
    }
  }

  public void start() throws IOException {
    init();

    Set<Plugin> plugins = center.getPlugins();
    LOG.info("Start generating html for " + plugins.size() + " plugins in folder :" + outputDirectory);

    for (Plugin plugin : plugins) {
      String pluginInfoWidget = generatePluginWidgetTemplate(plugin);
      File file = new File(outputDirectory, plugin.getKey() + ".html");
      LOG.info("Generate html for plugin : " + plugin.getKey() + " in file : " + file);
      FileUtils.writeStringToFile(file, pluginInfoWidget, "UTF-8");
    }
  }

  private String generatePluginWidgetTemplate(Plugin plugin) {
    Release lastRelease = plugin.getLastRelease();
    return StringUtils.replaceEach(
        pluginInfoWidgetTemplate,
        new String[]{"%name%", "%version%", "%date%", "%downloadUrl%", "%sonarVersion%", "%issueTracker%", "%sources%", "%license%", "%developers%"},
        new String[]{
            plugin.getName(),
            lastRelease.getVersion().getName(),
            formatDate(lastRelease.getDate()),
            lastRelease.getDownloadUrl(),
            lastRelease.getMinimumRequiredSonarVersion().getName(),
            formatLink(plugin.getIssueTrackerUrl()),
            formatLink(lastRelease.getSourcesUrl()),  // TODO add sources url in plugin manifest
            plugin.getLicense() == null ? "Unknown" : plugin.getLicense(),
            formatDevelopers(lastRelease.getDevelopers()) // TODO add developers in plugin manifest
        }
    );
  }

  private String formatLink(String url) {
    return StringUtils.isBlank(url) ? "Unknown" : "<a href=\"" + url + "\" target=\"_top\">" + url + "</a>";
  }

  private String formatDate(Date date) {
    return (new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)).format(date);
  }

  private String formatDevelopers(List<String> developers) {
    if (developers == null) {
      return "Unknown";
    }
    return StringUtils.join(developers, ", ");
  }

}
