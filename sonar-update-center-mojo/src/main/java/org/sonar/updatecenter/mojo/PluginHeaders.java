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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.mojo.CompatibilityMatrix.SQVersion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class PluginHeaders {

  private final File outputDirectory;
  private final UpdateCenter center;
  private final Log log;

  PluginHeaders(UpdateCenter center, File outputDirectory, Log log) throws IOException {
    this.outputDirectory = outputDirectory;
    this.center = center;
    this.log = log;
  }

  private void init() throws IOException {
    Preconditions.checkArgument(outputDirectory.exists());
    FileUtils.copyURLToFile(getClass().getResource("/style-confluence.css"), new File(outputDirectory, "style-confluence.css"));
    FileUtils.copyURLToFile(getClass().getResource("/error.png"), new File(outputDirectory, "error.png"));
  }

  void generateHtml() throws IOException {
    init();
    List<Plugin> plugins = center.getUpdateCenterPluginReferential().getPlugins();
    CompatibilityMatrix matrix = new CompatibilityMatrix();

    // We want to keep only latest patch version. For example for 3.7, 3.7.1, 3.7.2 we keep only 3.7.2
    Map<String, Release> majorVersions = new LinkedHashMap<String, Release>();
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
      Map<String, Object> dataModel = Maps.newHashMap();
      dataModel.put("pluginHeader", pluginHeader);

      File file = new File(outputDirectory, plugin.getKey() + "-confluence.html");
      log.info("Generate confluence html header of plugin " + plugin.getKey() + " in: " + file);
      print(dataModel, file, "plugin-confluence-template.html.ftl");

      file = new File(outputDirectory, plugin.getKey() + "-sonarsource.html");
      log.info("Generate sonarsource.com html header of plugin " + plugin.getKey() + " in: " + file);
      print(dataModel, file, "plugin-sonarsource-template.html.ftl");

      CompatibilityMatrix.Plugin matrixPlugin = new CompatibilityMatrix.Plugin(plugin.getName(), plugin.getHomepageUrl());
      matrix.getPlugins().add(matrixPlugin);

      for (Release sq : center.getSonar().getAllReleases()) {
        Release lastCompatible = plugin.getLastCompatible(sq.getVersion());
        if (lastCompatible != null) {
          matrixPlugin.getCompatibleVersionBySqVersion().put(sq.getVersion().toString(), lastCompatible.getVersion().toString());
        }
      }
    }

    Collections.sort(matrix.getPlugins(), new Comparator<CompatibilityMatrix.Plugin>() {
      @Override
      public int compare(org.sonar.updatecenter.mojo.CompatibilityMatrix.Plugin o1, org.sonar.updatecenter.mojo.CompatibilityMatrix.Plugin o2) {
        return o1.getName().compareToIgnoreCase(o2.getName());
      }
    });

    if (!matrix.getPlugins().isEmpty()) {
      File file = new File(outputDirectory, "compatibility-matrix.html");
      Map<String, Object> dataModel = Maps.newHashMap();
      dataModel.put("matrix", matrix);
      log.info("Generate compatibility matrix in: " + file);
      print(dataModel, file, "matrix-template.html.ftl");
    }
  }

  private void print(Map<String, Object> dataModel, File toFile, String templateName) {
    Writer writer = null;
    try {
      freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);
      freemarker.template.Configuration cfg = new freemarker.template.Configuration();
      cfg.setClassForTemplateLoading(PluginHeader.class, "");
      cfg.setObjectWrapper(new DefaultObjectWrapper());

      Template template = cfg.getTemplate(templateName);
      writer = new OutputStreamWriter(new FileOutputStream(toFile), Charsets.UTF_8);
      template.process(dataModel, writer);
      writer.flush();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to generate HTML to: " + toFile, e);

    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

}
