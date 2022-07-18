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
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;

public class CompatibilityMatrix {
  private static final Comparator<SonarVersionModel> SONAR_VERSION_MODEL_COMPARATOR =
    Comparator.comparing(SonarVersionModel::isLts).thenComparing(SonarVersionModel::getRealVersion).reversed();

  private final File outputDirectory;
  private final UpdateCenter center;
  private final Log log;

  private final List<SonarVersionModel> sqVersions = new ArrayList<>();
  private final List<Plugin> plugins = new ArrayList<>();

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
    FileUtils.copyURLToFile(getClass().getResource("/onde-sonar-16.png"), new File(outputDirectory, "onde-sonar-16.png"));
  }

  public void generateHtml() throws IOException {
    init();
    List<org.sonar.updatecenter.common.Plugin> pluginList = center.getUpdateCenterPluginReferential().getPlugins();

    // We want to keep only latest patch version. For example for 3.7, 3.7.1, 3.7.2 we keep only 3.7.2
    for (Release sq : center.getSonar().getMajorReleases()) {
      String displayVersion = sq.getVersion().getMajor() + "." + sq.getVersion().getMinor();
      Date releaseDate = sq.getDate();
      boolean isLts = center.getSonar().getLtsRelease().equals(sq);
      getSqVersions().add(new SonarVersionModel(sq.getVersion().toString(), displayVersion, releaseDate, isLts));
    }

    getSqVersions().sort(SONAR_VERSION_MODEL_COMPARATOR);

    for (org.sonar.updatecenter.common.Plugin plugin : pluginList) {
      PluginModel pluginModel = new PluginModel(plugin, center.getSonar());
      Map<String, Object> dataModel = new HashMap<>();
      dataModel.put("pluginHeader", pluginModel);

      CompatibilityMatrix.Plugin matrixPlugin = new CompatibilityMatrix.Plugin(plugin.getName(), plugin.getHomepageUrl(), plugin.isSupportedBySonarSource());
      getPlugins().add(matrixPlugin);

      for (Release sq : center.getSonar().getMajorReleases()) {
        Release lastCompatible = plugin.getLastCompatible(sq.getVersion());
        if (isNotArchived(lastCompatible)) {
          matrixPlugin.getCompatibleVersionBySqVersion().put(sq.getVersion().toString(), lastCompatible.getVersion().toString());
        }
      }
    }

    getPlugins().sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

    if (!getPlugins().isEmpty()) {
      File file = new File(outputDirectory, "compatibility-matrix.html");
      Map<String, Object> dataModel = new HashMap<>();
      dataModel.put("matrix", this);
      log.info("Generate compatibility matrix in: " + file);
      FreeMarkerUtils.print(dataModel, file, "matrix-template.html.ftl");
    }
  }

  private static boolean isNotArchived(@Nullable Release lastCompatible) {
    return lastCompatible != null && !lastCompatible.isArchived();
  }


  public List<SonarVersionModel> getSqVersions() {
    return sqVersions;
  }

  public List<Plugin> getPlugins() {
    return plugins;
  }

  public static class Plugin {

    private final String name;
    private final String homepageUrl;
    private final Map<String, String> compatibleVersionBySqVersion = new HashMap<>();

    private final boolean isSupportedBySonarSource;

    public Plugin(String name, String homepageUrl, boolean isSupportedBySonarSource) {
      this.name = name;
      this.homepageUrl = homepageUrl;
      this.isSupportedBySonarSource = isSupportedBySonarSource;
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

    public boolean isSupportedBySonarSource() {
      return isSupportedBySonarSource;
    }

  }

}
