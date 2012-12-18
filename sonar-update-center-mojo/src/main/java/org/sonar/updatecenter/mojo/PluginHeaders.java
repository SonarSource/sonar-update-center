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
package org.sonar.updatecenter.mojo;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.UpdateCenter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

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
    FileUtils.copyURLToFile(getClass().getResource("/style.css"), new File(outputDirectory, "style.css"));
  }

  void generateHtml() throws IOException {
    init();
    Set<Plugin> plugins = center.getPlugins();
    for (Plugin plugin : plugins) {
      File file = new File(outputDirectory, plugin.getKey() + ".html");
      log.info("Generate html header of plugin " + plugin.getKey() + " in: " + file);
      PluginHeader pluginHeader = new PluginHeader(plugin);
      print(pluginHeader, file);
    }
  }

  private void print(PluginHeader pluginHeader, File toFile) {
    Writer writer = null;
    try {
      freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);
      freemarker.template.Configuration cfg = new freemarker.template.Configuration();
      cfg.setClassForTemplateLoading(PluginHeader.class, "");
      cfg.setObjectWrapper(new DefaultObjectWrapper());

      Map<String, Object> root = Maps.newHashMap();
      root.put("pluginHeader", pluginHeader);

      Template template = cfg.getTemplate("plugin-template.html.ftl");
      writer = new FileWriter(toFile);
      template.process(root, writer);
      writer.flush();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to generate HTML header to: " + toFile, e);

    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

}
