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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginManifest;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.UpdateCenterSerializer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.apache.commons.io.FileUtils.forceMkdir;

public final class Server {

  private static final Logger LOG = LoggerFactory.getLogger(Server.class);

  public void start() throws IOException, URISyntaxException {
    Configuration conf = new Configuration(System.getProperties());
    conf.log();
    HttpDownloader downloader = new HttpDownloader(conf.getWorkingDir());
    UpdateCenter center = buildFromPartialMetadata(conf, downloader);
    downloadReleases(downloader, center);
    generateMetadata(conf, center);
    generateHtmlHeader(conf, center);
  }

  private UpdateCenter buildFromPartialMetadata(Configuration conf, HttpDownloader downloader) {
    return new MetadataFile(conf, downloader).getUpdateCenter();
  }

  private void downloadReleases(HttpDownloader downloader, UpdateCenter center) throws IOException, URISyntaxException {
    for (Plugin plugin : center.getPlugins()) {
      LOG.info("Load plugin: " + plugin.getKey());

      File masterJar = null;
      for (Release release : plugin.getReleases()) {
        if (StringUtils.isNotBlank(release.getDownloadUrl())) {
          File jar = downloader.download(release.getDownloadUrl(), false);
          if (jar != null && jar.exists()) {
            masterJar = jar;
          } else {
            release.setDownloadUrl(null);
            LOG.warn("Ignored because of wrong downloadUrl: plugin " + plugin.getKey() + ", version " + release.getVersion());
          }

        } else {
          LOG.warn("Ignored because of missing downloadUrl: plugin " + plugin.getKey() + ", version " + release.getVersion());
        }
      }

      // the last release is the master version for loading metadata included in manifest
      if (masterJar != null) {
        plugin.merge(new PluginManifest(masterJar));
      }
    }
  }

  private void generateMetadata(Configuration conf, UpdateCenter center) {
    LOG.info("Generate output: " + conf.getOutputFile());
    UpdateCenterSerializer.toProperties(center, conf.getOutputFile());
  }

  private void generateHtmlHeader(Configuration conf, UpdateCenter center) throws IOException {
    File htmlOutputDir = new File(conf.getWorkingDir(), "html");
    try {
      forceMkdir(htmlOutputDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create the working directory: " + htmlOutputDir.getAbsolutePath(), e);
    }
    PluginsHtmlHeader pluginsHtmlHeader = new PluginsHtmlHeader(center, htmlOutputDir);
    pluginsHtmlHeader.start();
  }

  public static void main(String[] args) throws IOException, URISyntaxException {
    new Server().start();
  }

}
