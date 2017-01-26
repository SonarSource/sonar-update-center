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

import com.google.common.base.Splitter;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginManifest;
import org.sonar.updatecenter.common.PluginReferential;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.UpdateCenterSerializer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;

import static org.apache.commons.io.FileUtils.forceMkdir;

class Generator {

  private static final String HTML_HEADER_DIR = "html";
  private final Configuration configuration;
  private final Log log;

  Generator(Configuration configuration, Log log) {
    this.configuration = configuration;
    this.log = log;
  }

  void generateHtml() throws IOException, URISyntaxException {
    UpdateCenter center = configuration.getUpdateCenter();
    downloadReleases(center);
    generateHtmlHeader(center);
  }

  void generateMetadata() throws IOException, URISyntaxException {
    UpdateCenter center = configuration.getUpdateCenter();
    downloadReleases(center);
    generateMetadata(center);
  }

  private void downloadReleases(UpdateCenter center) throws IOException, URISyntaxException {
    HttpDownloader downloader = new HttpDownloader(configuration.getOutputDir(), log);
    PluginReferential pluginReferential = center.getUpdateCenterPluginReferential();
    for (Plugin plugin : pluginReferential.getPlugins()) {
      log.info("Load plugin: " + plugin.getKey());

      File masterJar = null;
      for (Release release : plugin.getAllReleases()) {
        if (StringUtils.isNotBlank(release.getDownloadUrl())) {
          boolean forceDownload = release.equals(plugin.getDevRelease());
          File jar = downloader.download(release.getDownloadUrl(), forceDownload);
          if (jar != null && jar.exists()) {
            updateReleaseRequirePluginsParentPropertiesAndDisplayVersion(pluginReferential, jar, release);
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

  private static void updateReleaseRequirePluginsParentPropertiesAndDisplayVersion(PluginReferential pluginReferential, File jar, Release release) throws IOException {
    PluginManifest releaseManifest = new PluginManifest(jar);
    if (releaseManifest.getRequirePlugins() != null) {
      for (String requirePlugin : releaseManifest.getRequirePlugins()) {
        Iterator<String> split = Splitter.on(':').split(requirePlugin).iterator();
        String requiredPluginReleaseKey = split.next();
        String requiredMinimumReleaseVersion = split.next();
        pluginReferential.addOutgoingDependency(release, requiredPluginReleaseKey, requiredMinimumReleaseVersion);
      }
    }
    pluginReferential.findPlugin(release.getKey())
      .getRelease(release.getVersion())
      .setDisplayVersion(releaseManifest.getDisplayVersion());
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
    PluginHeaders pluginHeaders = new PluginHeaders(center, htmlOutputDir, log);
    pluginHeaders.generateHtml();
  }

}
