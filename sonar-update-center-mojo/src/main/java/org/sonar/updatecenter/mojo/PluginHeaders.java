/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2017 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;

class PluginHeaders {

  private final File outputDirectory;
  private final UpdateCenter center;
  private final Log log;

  PluginHeaders(UpdateCenter center, File outputDirectory, Log log) {
    this.outputDirectory = outputDirectory;
    this.center = center;
    this.log = log;
  }

  private void init() throws IOException {
    if (!outputDirectory.exists()) {
      throw new IllegalArgumentException("Output directory does not exist: " + outputDirectory);
    }
    FileUtils.copyURLToFile(getClass().getResource("/style-confluence.css"), new File(outputDirectory, "style-confluence.css"));
    FileUtils.copyURLToFile(getClass().getResource("/error.png"), new File(outputDirectory, "error.png"));
    FileUtils.copyURLToFile(getClass().getResource("/onde-sonar-16.png"), new File(outputDirectory, "onde-sonar-16.png"));
  }

  void generateHtml() throws IOException {
    init();
    List<Plugin> plugins = center.getUpdateCenterPluginReferential().getPlugins();
    CompatibilityMatrix matrix = new CompatibilityMatrix();

    // We want to keep only latest patch version. For example for 3.7, 3.7.1, 3.7.2 we keep only 3.7.2
    for (Release sq : center.getSonar().getMajorReleases()) {
      String displayVersion = sq.getVersion().getMajor() + "." + sq.getVersion().getMinor();
      Date releaseDate = sq.getDate();
      boolean isLts = center.getSonar().getLtsRelease().equals(sq);
      matrix.getSqVersions().add(new HtmlSQVersionModel(sq.getVersion().toString(), displayVersion, releaseDate, isLts));
    }
    for (Plugin plugin : plugins) {
      PluginHeader pluginHeader = new PluginHeader(plugin, center.getSonar());
      Map<String, Object> dataModel = new HashMap<>();
      dataModel.put("pluginHeader", pluginHeader);

      File file = new File(outputDirectory, plugin.getKey() + "-confluence-include.html");
      log.info("Generate confluence html include of plugin " + plugin.getKey() + " in: " + file);
      FreeMarkerUtils.print(dataModel, file, "plugin-confluence-include-template.html.ftl");

      file = new File(outputDirectory, plugin.getKey() + "-sonarsource-include.html");
      log.info("Generate sonarsource.com include of plugin " + plugin.getKey() + " in: " + file);
      FreeMarkerUtils.print(dataModel, file, "plugin-sonarsource-include-template.html.ftl");

      CompatibilityMatrix.Plugin matrixPlugin = new CompatibilityMatrix.Plugin(plugin.getName(), plugin.getHomepageUrl(), plugin.isSupportedBySonarSource());
      matrix.getPlugins().add(matrixPlugin);

      for (Release sq : center.getSonar().getMajorReleases()) {
        Release lastCompatible = plugin.getLastCompatible(sq.getVersion());
        if (lastCompatible != null) {
          matrixPlugin.getCompatibleVersionBySqVersion().put(sq.getVersion().toString(), lastCompatible.getVersion().toString());
        }
      }
    }

    matrix.getPlugins().sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

    if (!matrix.getPlugins().isEmpty()) {
      File file = new File(outputDirectory, "compatibility-matrix.html");
      Map<String, Object> dataModel = new HashMap<>();
      dataModel.put("matrix", matrix);
      log.info("Generate compatibility matrix in: " + file);
      FreeMarkerUtils.print(dataModel, file, "matrix-template.html.ftl");
    }
  }

}
