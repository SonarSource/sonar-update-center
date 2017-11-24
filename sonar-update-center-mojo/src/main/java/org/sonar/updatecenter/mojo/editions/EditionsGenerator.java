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
import java.io.IOException;
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
import org.sonar.updatecenter.mojo.editions.generators.EditionGenerator;

public class EditionsGenerator {

  public static final Version MIN_SUPPORTED_SQ_VERSION = Version.create("6.7");
  private static final Logger LOGGER = LoggerFactory.getLogger(EditionsGenerator.class);

  private final UpdateCenter updateCenter;
  private final EditionTemplatesLoader templatesLoader;
  private final String editionBuildNumber;

  public EditionsGenerator(UpdateCenter updateCenter, EditionTemplatesLoader templatesLoader, String editionBuildNumber) {
    this.updateCenter = updateCenter;
    this.templatesLoader = templatesLoader;
    this.editionBuildNumber = editionBuildNumber;
  }

  public void generateZipsJsonHtml(File outputDir, EditionGenerator... generators) throws Exception {
    FileUtils.forceMkdir(outputDir);
    FileUtils.cleanDirectory(outputDir);

    List<Edition> editions = createEditions();
    if (!editions.isEmpty()) {
      for (EditionGenerator generator : generators) {
        generator.generate(outputDir, editions);
      }
    }
  }

  List<Edition> createEditions() throws IOException {
    List<EditionTemplate> templates = templatesLoader.load();

    List<Version> sqVersions = updateCenter.getSonar()
      .getAllReleases()
      .stream()
      .filter(r -> r.getVersion().compareTo(MIN_SUPPORTED_SQ_VERSION) >= 0)
      .map(Release::getVersion)
      .collect(Collectors.toList());

    List<Edition> editions = new ArrayList<>();
    for (Version sqVersion : sqVersions) {
      editions.addAll(generateForSqVersion(sqVersion, templates));
    }
    return editions;
  }

  private List<Edition> generateForSqVersion(Version sqVersion, List<EditionTemplate> templates) {
    List<Edition> editions = new ArrayList<>();

    for (EditionTemplate template : templates) {
      LOGGER.info("Generate edition [{}] for SonarQube {}", template.getKey(), sqVersion);
      Edition.Builder builder = new Edition.Builder();

      builder
        .setKey(template.getKey())
        .setName(template.getName())
        .setTextDescription(template.getTextDescription())
        .setHomeUrl(template.getHomeUrl())
        .setRequestUrl(template.getRequestUrl())
        .setSonarQubeVersion(sqVersion.getName());

      boolean missingPlugin = false;
      boolean generateZip = false;
      for (String pluginKey : template.getPluginKeys()) {
        Plugin plugin = updateCenter.getUpdateCenterPluginReferential().findPlugin(pluginKey);
        Release pluginRelease = plugin.getLastCompatibleRelease(sqVersion);
        if (pluginRelease == null) {
          LOGGER.warn("Plugin {} has no release compatible with SonarQube {}.", pluginKey, sqVersion);
          missingPlugin = true;
        } else {
          builder.addJar(pluginRelease.getFilename());
          generateZip = true;
        }
      }

      if (generateZip) {
        String zipFileName = template.getKey() + "-edition-" + EditionVersion.create(sqVersion, editionBuildNumber).toString() + ".zip";
        builder.setZipFileName(zipFileName);
      }

      if (!missingPlugin) {
        editions.add(builder.build());
      }
    }

    return editions;
  }
}
