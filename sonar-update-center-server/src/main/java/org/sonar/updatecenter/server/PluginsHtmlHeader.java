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
import com.google.common.collect.Maps;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.UpdateCenter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

public class PluginsHtmlHeader {

  private static final Logger LOG = LoggerFactory.getLogger(PluginsHtmlHeader.class);

  private File outputDirectory;
  private UpdateCenter center;

  public PluginsHtmlHeader(UpdateCenter center, File outputDirectory) throws IOException {
    this.outputDirectory = outputDirectory;
    this.center = center;
  }

  private void init() throws IOException {
    Preconditions.checkArgument(outputDirectory.exists());
    FileUtils.copyURLToFile(getClass().getResource("/style.css"), new File(outputDirectory, "style.css"));
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

      Template template = cfg.getTemplate("plugin-template.ftl");
      writer = new FileWriter(toFile);
      template.process(root, writer);
      writer.flush();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to generate Autocontrol HTML report to: " + toFile, e);

    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  public void start() throws IOException {
    init();
    Set<Plugin> plugins = center.getPlugins();
    LOG.info("Start generating html for " + plugins.size() + " plugins in folder :" + outputDirectory);
    for (Plugin plugin : plugins) {
      File file = new File(outputDirectory, plugin.getKey() + ".html");
      LOG.info("Generate html for plugin : " + plugin.getKey() + " in file : " + file);
      PluginHeader pluginHeader = new PluginHeader(plugin);
      print(pluginHeader, file);
    }
  }

}
