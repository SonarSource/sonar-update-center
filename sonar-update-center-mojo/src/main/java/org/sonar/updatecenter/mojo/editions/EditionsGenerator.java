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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;
import org.sonar.updatecenter.mojo.FreeMarkerUtils;
import org.sonar.updatecenter.mojo.SQVersionInMatrix;

import static java.nio.charset.StandardCharsets.UTF_8;

public class EditionsGenerator {

  private static final Version MIN_SUPPORTED_SQ_VERSION = Version.create("6.6");
  private static final Logger LOGGER = LoggerFactory.getLogger(EditionsGenerator.class);

  private final UpdateCenter updateCenter;
  private final EditionTemplatesLoader templatesLoader;
  private final File jarsDir;
  private final String downloadBaseUrl;
  private final String editionBuildNumber;

  public EditionsGenerator(UpdateCenter updateCenter, EditionTemplatesLoader templatesLoader, File jarsDir,
    String downloadBaseUrl, String editionBuildNumber) {
    this.updateCenter = updateCenter;
    this.templatesLoader = templatesLoader;
    this.jarsDir = jarsDir;
    this.downloadBaseUrl = downloadBaseUrl;
    this.editionBuildNumber = editionBuildNumber;
    if (!jarsDir.exists()) {
      throw new IllegalArgumentException("Directory does not exist: " + jarsDir.getAbsolutePath());
    }
  }

  public void generateZipsAndJson(File outputDir) throws IOException {
    FileUtils.forceMkdir(outputDir);
    FileUtils.cleanDirectory(outputDir);

    File jsonOutput = new File(outputDir, "editions.json");
    List<Edition> editions = generateZips(outputDir);
    try (Writer jsonWriter = new OutputStreamWriter(new FileOutputStream(jsonOutput), UTF_8)) {
      LOGGER.info("Generate {}", jsonOutput.getAbsolutePath());
      new EditionsJson().write(editions, downloadBaseUrl, jsonWriter);
    }
    generateEditionsHtml(outputDir, editions);
  }

  private void generateEditionsHtml(File outputDir, List<Edition> editions) {
    File editionsHtmlOutput = new File(outputDir, "editions.html");
    LOGGER.info("Generate {}", editionsHtmlOutput);
    EditionsMatrix matrix = new EditionsMatrix();
    Map<String, Release> majorVersions = new LinkedHashMap<>();
    for (Release sq : updateCenter.getSonar().getMajorReleases()) {
      majorVersions.put(sq.getVersion().getName(), sq);
    }
    for (Edition edition : editions) {
      // We want to keep only latest patch version. For example for 3.7, 3.7.1, 3.7.2 we keep only 3.7.2
      if (majorVersions.keySet().contains(edition.getSonarQubeVersion())) {
        matrix.getSqVersionsByVersion().computeIfAbsent(edition.getSonarQubeVersion(), v -> {
          Release sq = majorVersions.get(v);
          return new SQVersionInMatrix(sq, updateCenter.getSonar().getLtsRelease().equals(sq));
        });
      }
      matrix.getEditionsByKey().computeIfAbsent(edition.getKey(), k -> new EditionsMatrix.EditionInMatrix(edition.getName()))
        .getCompatibleEditionBySqVersion().put(edition.getSonarQubeVersion(), edition);
    }
    Map<String, Object> dataModel = new HashMap<>();
    dataModel.put("matrix", matrix);
    dataModel.put("downloadBaseUrl", downloadBaseUrl);
    FreeMarkerUtils.print(dataModel, editionsHtmlOutput, "editions/editions-template.html.ftl");
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

      File zipFile = new File(outputDir, template.getKey() + "-edition-" + EditionVersion.create(sqVersion, editionBuildNumber).toString() + ".zip");
      builder
        .setKey(template.getKey())
        .setName(template.getName())
        .setTextDescription(template.getTextDescription())
        .setHomeUrl(template.getHomeUrl())
        .setRequestUrl(template.getRequestUrl())
        .setSonarQubeVersion(sqVersion.getName())
        .setTargetZip(zipFile);

      boolean missingPlugin = false;
      for (String pluginKey : template.getPluginKeys()) {
        Plugin plugin = updateCenter.getUpdateCenterPluginReferential().findPlugin(pluginKey);
        Release pluginRelease = plugin.getLastCompatibleRelease(sqVersion);
        if (pluginRelease == null) {
          LOGGER.warn("Plugin {} has no release compatible with SonarQube {}.", pluginKey, sqVersion);
          missingPlugin = true;
        } else {
          builder.addJar(new File(jarsDir, pluginRelease.getFilename()));
        }
      }

      if (!missingPlugin) {
        editions.add(builder.build());
      }
    }

    return editions;
  }
}
