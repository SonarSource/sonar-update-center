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
package org.sonar.updatecenter.mojo.editions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static java.nio.charset.StandardCharsets.UTF_8;

public class EditionsGenerator {

  private static final Version MIN_SUPPORTED_SQ_VERSION = Version.create("6.5");
  private static final Logger LOGGER = LoggerFactory.getLogger(EditionsGenerator.class);

  private final UpdateCenter updateCenter;
  private final EditionTemplatesLoader templatesLoader;
  private final File jarsDir;

  public EditionsGenerator(UpdateCenter updateCenter, EditionTemplatesLoader templatesLoader, File jarsDir) {
    this.updateCenter = updateCenter;
    this.templatesLoader = templatesLoader;
    this.jarsDir = jarsDir;
    if (!jarsDir.exists()) {
      throw new IllegalArgumentException("Directory " + jarsDir + " does not exist");
    }
  }

  public void generateZipsAndJson(File outputDir, String downloadBaseUrl) throws IOException {
    FileUtils.forceMkdir(outputDir);
    FileUtils.cleanDirectory(outputDir);

    File jsonOutput = new File(outputDir, "editions.json");
    try (Writer jsonWriter = new OutputStreamWriter(new FileOutputStream(jsonOutput), UTF_8)) {
      List<Edition> editions = generateZips(outputDir);

      LOGGER.info("Generate {}", jsonOutput.getAbsolutePath());
      new EditionsJson().write(editions, downloadBaseUrl, jsonWriter);
    }
  }

  /**
   * Example of files generated in outputDir:
   * - edition1-6.7.zip
   * - edition1-7.0.zip
   * - edition2-6.7.zip
   * - edition2-7.0.zip
   */
  List<Edition> generateZips(File outputDir) throws IOException {
    List<EditionTemplate> templates = templatesLoader.load();

    List<Version> sqVersions = updateCenter.getSonar()
      .getAllReleases()
      .stream()
      .filter(r -> r.getVersion().compareTo(MIN_SUPPORTED_SQ_VERSION) >= 0)
      .map(Release::getVersion)
      .collect(Collectors.toList());

    List<Edition> editions = new ArrayList<>();
    for (Version sqVersion : sqVersions) {
      editions.addAll(generateForSqVersion(sqVersion, templates, outputDir));
    }
    return editions;
  }

  private List<Edition> generateForSqVersion(Version sqVersion, List<EditionTemplate> templates, File outputDir) throws IOException {
    List<Edition> editions = new ArrayList<>();
    for (EditionTemplate template : templates) {
      LOGGER.info("Generate edition [{}] for SonarQube {}", template.getKey(), sqVersion);
      Edition.Builder builder = new Edition.Builder();
      File zipFile = new File(outputDir, template.getKey() + "-" + sqVersion.toString() + ".zip");
      builder
        .setKey(template.getKey())
        .setName(template.getName())
        .setTextDescription(template.getTextDescription())
        .setHomeUrl(template.getHomeUrl())
        .setRequestUrl(template.getRequestUrl())
        .setSonarQubeVersion(sqVersion.toString())
        .setTargetZip(zipFile);

      for (String pluginKey : template.getPluginKeys()) {
        Plugin plugin = updateCenter.getUpdateCenterPluginReferential().findPlugin(pluginKey);
        Release pluginRelease = plugin.getLastCompatibleRelease(sqVersion);
        if (pluginRelease == null) {
          LOGGER.warn("Plugin {} has no release compatible with SonarQube {}.", pluginKey, sqVersion);
          continue;
        }
        builder.addJar(new File(jarsDir, pluginRelease.getFilename()));
      }

      editions.add(builder.build());
    }

    return editions;
  }
}
