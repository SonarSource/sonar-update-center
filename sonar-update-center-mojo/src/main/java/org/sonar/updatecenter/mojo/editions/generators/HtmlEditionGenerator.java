/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2022 SonarSource SA
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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.mojo.FreeMarkerUtils;
import org.sonar.updatecenter.mojo.SonarVersionModel;
import org.sonar.updatecenter.mojo.editions.Edition;

import static org.sonar.updatecenter.mojo.editions.EditionsGenerator.MIN_SUPPORTED_SQ_VERSION;

/**
 * Generates a HTML section to display download links for an edition, for each SQ version.
 * It considers the latest patches for all versions of sonarqube since {@link org.sonar.updatecenter.mojo.editions.EditionsGenerator#MIN_SUPPORTED_SQ_VERSION}.
 * 
 * It adds a column for each SonarQube version, even if they are not supported by the edition.
 */
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

    Map<String, Release> majorReleases = new LinkedHashMap<>();
    // We want to keep only latest patch version. For example for 3.7, 3.7.1, 3.7.2 we keep only 3.7.2
    for (Release majorRelease : updateCenter.getSonar().getMajorReleases()) {
      majorReleases.put(majorRelease.getVersion().getName(), majorRelease);
    }

    // group editions by key. There is one edition per supported SQ version.
    Map<String, List<Edition>> editionsByKey = editions.stream()
      .collect(Collectors.groupingBy(Edition::getKey));

    for (List<Edition> editionList : editionsByKey.values()) {
      generateEditionHtml(outputDir, majorReleases, editionList);
    }
  }

  private void generateEditionHtml(File outputDir, Map<String, Release> majorReleases, List<Edition> editionList) {
    Edition edition = editionList.iterator().next();
    File editionHtmlOutputFile = new File(outputDir, "edition-" + edition.getKey() + ".html");
    HtmlEditionModel htmlEdition = new HtmlEditionModel(edition.getName());

    for (Edition e : editionList) {
      if (majorReleases.containsKey(e.getSonarQubeVersion())) {
        Release release = majorReleases.get(e.getSonarQubeVersion());
        String downloadUrl = e.getDownloadUrl(downloadBaseUrl);
        if (downloadUrl != null) {
          htmlEdition.add(release.getVersion().toString(), downloadUrl);
        }
      }
    }

    List<SonarVersionModel> htmlSqVersions = majorReleases.values().stream()
      .filter(r -> r.getVersion().compareTo(MIN_SUPPORTED_SQ_VERSION) >= 0)
      .map(this::createHtmlSqVersion)
      .collect(Collectors.toList());

    printEditionHtml(editionHtmlOutputFile, htmlEdition, htmlSqVersions);
  }

  private SonarVersionModel createHtmlSqVersion(Release release) {
    String displayVersion = release.getVersion().getMajor() + "." + release.getVersion().getMinor();
    Date releaseDate = release.getDate();
    boolean isLts = updateCenter.getSonar().getLtsRelease().equals(release);
    return new SonarVersionModel(release.getVersion().toString(), displayVersion, releaseDate, isLts);
  }

  private void printEditionHtml(File htmlFile, HtmlEditionModel htmlEditionModel, List<SonarVersionModel> htmlSqVersions) {
    LOGGER.info("Generate HTML file: {}", htmlFile);
    Map<String, Object> dataModel = new HashMap<>();
    dataModel.put("edition", htmlEditionModel);
    dataModel.put("sqVersions", htmlSqVersions);
    dataModel.put("downloadBaseUrl", downloadBaseUrl);
    FreeMarkerUtils.print(dataModel, htmlFile, "editions/editions-template.html.ftl");
  }
}
