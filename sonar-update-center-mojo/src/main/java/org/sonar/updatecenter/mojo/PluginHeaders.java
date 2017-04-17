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

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.mojo.CompatibilityMatrix.SQVersion;

import static java.nio.charset.StandardCharsets.UTF_8;

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
    Map<String, Release> majorVersions = new LinkedHashMap<>();
    for (Release sq : center.getSonar().getAllReleases()) {
      String displayVersion = sq.getVersion().getMajor() + "." + sq.getVersion().getMinor();
      majorVersions.put(displayVersion, sq);
    }
    for (Map.Entry<String, Release> sq : majorVersions.entrySet()) {
      matrix.getSqVersions().add(
        new SQVersion(sq.getKey(), sq.getValue().getVersion().toString(), center.getSonar().getLtsRelease().equals(sq.getValue()), sq.getValue().getDate()));
    }
    for (Plugin plugin : plugins) {
      PluginHeader pluginHeader = new PluginHeader(plugin, center.getSonar());
      Map<String, Object> dataModel = new HashMap<>();
      dataModel.put("pluginHeader", pluginHeader);

      File file = new File(outputDirectory, plugin.getKey() + "-confluence-include.html");
      log.info("Generate confluence html include of plugin " + plugin.getKey() + " in: " + file);
      print(dataModel, file, "plugin-confluence-include-template.html.ftl");

      file = new File(outputDirectory, plugin.getKey() + "-sonarsource-include.html");
      log.info("Generate sonarsource.com include of plugin " + plugin.getKey() + " in: " + file);
      print(dataModel, file, "plugin-sonarsource-include-template.html.ftl");

      CompatibilityMatrix.Plugin matrixPlugin = new CompatibilityMatrix.Plugin(plugin.getName(), plugin.getHomepageUrl(), plugin.isSupportedBySonarSource());
      matrix.getPlugins().add(matrixPlugin);

      for (Release sq : center.getSonar().getAllReleases()) {
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
      print(dataModel, file, "matrix-template.html.ftl");
    }
  }

  private void print(Map<String, Object> dataModel, File toFile, String templateName) {
    try (FileOutputStream fileOutputStream = new FileOutputStream(toFile);
      Writer writer = new OutputStreamWriter(fileOutputStream, UTF_8)) {
      freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);
      freemarker.template.Configuration cfg = new freemarker.template.Configuration();
      cfg.setClassForTemplateLoading(PluginHeader.class, "");
      cfg.setObjectWrapper(new DefaultObjectWrapper());

      Template template = cfg.getTemplate(templateName);

      template.process(dataModel, writer);
      writer.flush();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to generate HTML to: " + toFile, e);
    }
  }

}
