/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2025 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.sonar.updatecenter.common.Product;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static org.sonar.updatecenter.common.Product.OLD_SONARQUBE;

public class CompatibilityMatrix {
  private static final Comparator<SonarVersionModel> SONAR_VERSION_MODEL_COMPARATOR =
    Comparator.comparing(SonarVersionModel::isLta).thenComparing(svm -> Version.create(svm.getRealVersion())).reversed();

  private final File outputDirectory;
  private final UpdateCenter center;
  private final Log log;

  private final List<SonarVersionModel> sqVersions = new ArrayList<>();
  private final List<SonarVersionModel> communityBuildVersions = new ArrayList<>();
  private final List<SonarVersionModel> sonarqubeServerVersions = new ArrayList<>();

  private final List<Plugin> pluginsForOldSonarQube = new ArrayList<>();
  private final List<Plugin> pluginsForSonarQubeServer = new ArrayList<>();
  private final List<Plugin> pluginsForCommunityBuild = new ArrayList<>();

  CompatibilityMatrix(UpdateCenter center, File outputDirectory, Log log) {
    this.outputDirectory = outputDirectory;
    this.center = center;
    this.log = log;
  }

  private void init() throws IOException {
    if (!outputDirectory.exists()) {
      throw new IllegalArgumentException("Output directory does not exist: " + outputDirectory);
    }
    FileUtils.copyURLToFile(getClass().getResource("/styles.css"), new File(outputDirectory, "styles.css"));
    FileUtils.copyURLToFile(getClass().getResource("/error.png"), new File(outputDirectory, "error.png"));
  }

  public void generateHtmls() throws IOException {
    init();

    fillSonarVersionModelList(OLD_SONARQUBE);
    fillSonarVersionModelList(Product.SONARQUBE_COMMUNITY_BUILD);
    fillSonarVersionModelList(Product.SONARQUBE_SERVER);

    Map<String, Object> dataModel = new HashMap<>();
    fillPluginsList(OLD_SONARQUBE, pluginsForOldSonarQube, dataModel);
    generateHtml("matrix-template.html.ftl", "compatibility-matrix.html", pluginsForOldSonarQube, dataModel);

    dataModel = new HashMap<>();
    fillPluginsList(Product.SONARQUBE_COMMUNITY_BUILD, pluginsForCommunityBuild, dataModel);
    generateHtml("matrix-template-sqcb.html.ftl", "compatibility-matrix-sqcb.html", pluginsForCommunityBuild, dataModel);

    dataModel = new HashMap<>();
    fillPluginsList(Product.SONARQUBE_SERVER, pluginsForSonarQubeServer, dataModel);
    generateHtml("matrix-template-sqs.html.ftl", "compatibility-matrix-sqs.html", pluginsForSonarQubeServer, dataModel);
    sortPluginsLists();
  }

  private void sortPluginsLists() {
    pluginsForOldSonarQube.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
    pluginsForSonarQubeServer.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
    pluginsForCommunityBuild.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
  }

  private void fillPluginsList(Product product, List<Plugin> plugins, Map<String, Object> dataModel) {
    List<org.sonar.updatecenter.common.Plugin> pluginList = center.getUpdateCenterPluginReferential().getPlugins();
    for (org.sonar.updatecenter.common.Plugin plugin : pluginList) {
      PluginModel pluginModel = new PluginModel(plugin, center.getSonar());
      dataModel.put("pluginHeader", pluginModel);

      CompatibilityMatrix.Plugin matrixPlugin = new CompatibilityMatrix.Plugin(plugin.getName(), plugin.getHomepageUrl());
      plugins.add(matrixPlugin);

      for (Release majorRelease : center.getSonar().getMajorReleases(product)) {
        Release lastCompatible = plugin.getLastCompatible(majorRelease.getVersion(), product);
        if (isNotArchived(lastCompatible)) {
          matrixPlugin.getCompatibleVersionBySqVersion().put(majorRelease.getVersion().toString(), lastCompatible.getVersion().toString());
        }
      }
    }
  }

  private void generateHtml(String templateName, String outputFileName, List<Plugin> plugins, Map<String, Object> dataModel) {
    if (!plugins.isEmpty() && plugins.stream().filter(p -> p.compatibleVersionBySqVersion.size() > 0).count() > 0) {
      File file = new File(outputDirectory, outputFileName);
      dataModel.put("matrix", this);
      log.info("Generate compatibility matrix in: " + file);
      FreeMarkerUtils.print(dataModel, file, templateName);
    }
  }

  private void fillSonarVersionModelList(Product product) {
    // We want to keep only latest patch version. For example for 3.7, 3.7.1, 3.7.2 we keep only 3.7.2
    for (Release sq : center.getSonar().getMajorReleases(product)) {
      String displayVersion = sq.getVersion().getMajor() + "." + sq.getVersion().getMinor();
      Date releaseDate = sq.getDate();
      boolean isLta = center.getSonar().getLtaVersion().getVersion().equals(sq.getVersion());
      SonarVersionModel sonarVersionModel = new SonarVersionModel(sq.getVersion().toString(), displayVersion, releaseDate, isLta);
      switch (product) {
        case OLD_SONARQUBE: {
          sqVersions.add(sonarVersionModel);
          sqVersions.sort(SONAR_VERSION_MODEL_COMPARATOR);
          break;
        }
        case SONARQUBE_COMMUNITY_BUILD: {
          communityBuildVersions.add(sonarVersionModel);
          communityBuildVersions.sort(SONAR_VERSION_MODEL_COMPARATOR);
          break;
        }
        case SONARQUBE_SERVER: {
          sonarqubeServerVersions.add(sonarVersionModel);
          sonarqubeServerVersions.sort(SONAR_VERSION_MODEL_COMPARATOR);
          break;
        }
      }
    }
  }

  private static boolean isNotArchived(@Nullable Release lastCompatible) {
    return lastCompatible != null && !lastCompatible.isArchived();
  }

  /**
   * Used by HTML templating framework. Do not remove it.
   */
  public List<SonarVersionModel> getSqVersions() {
    return sqVersions;
  }

  public List<Plugin> getPluginsForOldSonarQube() {
    return pluginsForOldSonarQube;
  }

  public List<Plugin> getPluginsForSonarQubeServer() {
    return pluginsForSonarQubeServer;
  }

  public List<Plugin> getPluginsForCommunityBuild() {
    return pluginsForCommunityBuild;
  }

  public List<SonarVersionModel> getCommunityBuildVersions() {
    return communityBuildVersions;
  }

  public List<SonarVersionModel> getSonarqubeServerVersions() {
    return sonarqubeServerVersions;
  }

  public static class Plugin {

    private final String name;
    private final String homepageUrl;
    private final Map<String, String> compatibleVersionBySqVersion = new HashMap<>();


    public Plugin(String name, String homepageUrl) {
      this.name = name;
      this.homepageUrl = homepageUrl;
    }

    public String getName() {
      return name;
    }

    public String getHomepageUrl() {
      return homepageUrl;
    }

    public Map<String, String> getCompatibleVersionBySqVersion() {
      return compatibleVersionBySqVersion;
    }

    public boolean supports(String sqVersion) {
      return compatibleVersionBySqVersion.containsKey(sqVersion);
    }

    public String supportedVersion(String sqVersion) {
      return compatibleVersionBySqVersion.get(sqVersion);
    }


  }

}
