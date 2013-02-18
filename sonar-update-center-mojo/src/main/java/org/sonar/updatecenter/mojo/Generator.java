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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.sonar.updatecenter.common.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.apache.commons.io.FileUtils.forceMkdir;

class Generator {

  private static final String HTML_HEADER_DIR = "html";

  private final Configuration configuration;
  private final Log log;

  Generator(Configuration configuration, Log log) {
    this.configuration = configuration;
    this.log = log;
  }

  void generate() throws IOException, URISyntaxException {
    UpdateCenter center = configuration.getUpdateCenter();
    downloadReleases(center);
    generateMetadata(center);
    generateHtmlHeader(center);
  }

  private void downloadReleases(UpdateCenter center) throws IOException, URISyntaxException {
    HttpDownloader downloader = new HttpDownloader(configuration.getOutputDir(), log);
    for (Plugin plugin : center.getPlugins()) {
      log.info("Load plugin: " + plugin.getKey());

      File masterJar = null;
      for (Release release : plugin.getReleases()) {
        if (StringUtils.isNotBlank(release.getDownloadUrl())) {
          File jar = downloader.download(release.getDownloadUrl(), false);
          if (jar != null && jar.exists()) {
            masterJar = jar;
          } else {
            throw new IllegalStateException("Plugin " + plugin.getKey() + " can't be downloaded at: " + release.getDownloadUrl());
          }

        } else {
          log.warn("Ignored because of missing downloadUrl: plugin " + plugin.getKey() + ", version " + release.getVersion());
        }
      }

      // the last release is the master version for loading metadata included in manifest
      if (masterJar != null) {
        plugin.merge(new PluginManifest(masterJar));
      }
    }
  }

  private void generateMetadata(UpdateCenter center) {
    log.info("Generate output: " + configuration.getOutputFile());
    UpdateCenterSerializer.toProperties(center, configuration.getOutputFile());
  }

  private void generateHtmlHeader(UpdateCenter center) throws IOException {
    File htmlOutputDir = new File(configuration.getOutputDir(), HTML_HEADER_DIR);
    try {
      forceMkdir(htmlOutputDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create the working directory: " + htmlOutputDir.getAbsolutePath(), e);
    }
    PluginHeaders pluginHeaders = new PluginHeaders(center.getUpdateCenterPluginReferential(), htmlOutputDir, log);
    pluginHeaders.generateHtml();
  }

}
