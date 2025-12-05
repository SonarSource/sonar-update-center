/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2025 SonarSource Sàrl
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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginManifest;
import org.sonar.updatecenter.common.PluginReferential;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.UpdateCenterSerializer;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

import static org.apache.commons.io.FileUtils.forceMkdir;

class Generator {

  private static final String HTML_HEADER_DIR = "html";
  private static final String JSON_DIR = "json";
  private final Configuration configuration;
  private final Log log;

  Generator(Configuration configuration, Log log) {
    this.configuration = configuration;
    this.log = log;
  }

  private static void mergeFromManifest(Plugin plugin, @Nullable File masterJar) throws IOException {
    // the last release is the master version for loading metadata included in manifest
    if (masterJar != null) {
      PluginManifest manifest = new PluginManifest(masterJar);
      if (!StringUtils.equals(plugin.getKey(), manifest.getKey())) {
        throw new IllegalStateException(
          "Plugin " + masterJar.getName() + " is declared with key '" + manifest.getKey() + "' in its MANIFEST, but with key '" + plugin.getKey() + "' in the update center");
      }
      plugin.merge(manifest);
    }
  }

  private static void updateReleaseRequirePluginsParentPropertiesAndDisplayVersion(PluginReferential pluginReferential, File jar, Release release) throws IOException {
    PluginManifest releaseManifest = new PluginManifest(jar);
    if (releaseManifest.getRequirePlugins() != null) {
      for (String requirePlugin : releaseManifest.getRequirePlugins()) {
        String[] split = requirePlugin.split(":");
        String requiredPluginReleaseKey = split[0];
        String requiredMinimumReleaseVersion = split[1];
        pluginReferential.addOutgoingDependency(release, requiredPluginReleaseKey, requiredMinimumReleaseVersion);
      }
    }
    pluginReferential.findPlugin(release.getKey())
      .getRelease(release.getVersion())
      .setDisplayVersion(releaseManifest.getDisplayVersion());
  }

  private static File ensureDirectory(File baseDirectory, String directory) {
    File outputDir = new File(baseDirectory, directory);
    try {
      forceMkdir(outputDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create the working directory: " + outputDir.getAbsolutePath(), e);
    }
    return outputDir;
  }

  void generateHtml() throws IOException {
    UpdateCenter center = configuration.getUpdateCenter();
    downloadReleases(center);
    generateHtmlMatrix(center);
  }

  void generateMetadata() throws IOException {
    UpdateCenter center = configuration.getUpdateCenter();
    downloadReleases(center);
    generateMetadata(center);
  }

  void generateJson() throws IOException {
    UpdateCenter center = configuration.getUpdateCenter();
    downloadReleases(center);
    prepareDirectoryAndOutputJson(center);
  }

  private void downloadReleases(UpdateCenter center) throws IOException {
    HttpDownloader downloader = new HttpDownloader(configuration.getOutputDir(), configuration.mustCheckDownloadUrls(), log);
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
      mergeFromManifest(plugin, masterJar);
    }
  }

  private void generateMetadata(UpdateCenter center) {
    log.info("Generate output: " + configuration.getOutputFile());
    UpdateCenterSerializer.toProperties(center, configuration.getOutputFile());
  }

  private void generateHtmlMatrix(UpdateCenter center) throws IOException {
    File htmlOutputDir = ensureDirectory(configuration.getOutputDir(), HTML_HEADER_DIR);
    CompatibilityMatrix matrix = new CompatibilityMatrix(center, htmlOutputDir, log);
    matrix.generateHtmls();
  }

  private void prepareDirectoryAndOutputJson(UpdateCenter center) throws IOException {
    File jsonOutputDir = ensureDirectory(configuration.getOutputDir(), JSON_DIR);
    PluginsJsonGenerator.create(center, jsonOutputDir, log).generateJsonFiles();
    ScannerJsonGenerator.create(center, jsonOutputDir, log).generateJsonFiles();
  }
}
