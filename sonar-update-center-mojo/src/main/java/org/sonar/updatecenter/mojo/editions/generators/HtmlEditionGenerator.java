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
package org.sonar.updatecenter.mojo.editions.generators;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.mojo.FreeMarkerUtils;
import org.sonar.updatecenter.mojo.SQVersionInMatrix;
import org.sonar.updatecenter.mojo.editions.Edition;
import org.sonar.updatecenter.mojo.editions.EditionsMatrix;

public class HtmlEditionGenerator implements EditionGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(HtmlEditionGenerator.class);
  private final UpdateCenter updateCenter;
  private final String downloadBaseUrl;

  public HtmlEditionGenerator(UpdateCenter updateCenter, String downloadBaseUrl) {
    this.updateCenter = updateCenter;
    this.downloadBaseUrl = downloadBaseUrl;

  }

  @Override
  public void generate(File outputDir, List<Edition> editions) {
    LOGGER.info("Generate HTML files");

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

    printEditionsHtml(outputDir, matrix);
  }

  private void printEditionsHtml(File outputDir, EditionsMatrix matrix) {
    File editionsHtmlOutput = new File(outputDir, "editions.html");

    Map<String, Object> dataModel = new HashMap<>();
    dataModel.put("matrix", matrix);
    dataModel.put("downloadBaseUrl", downloadBaseUrl);
    FreeMarkerUtils.print(dataModel, editionsHtmlOutput, "editions/editions-template.html.ftl");
  }
}
