/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2024 SonarSource SA
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

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import org.apache.maven.plugin.logging.Log;
import org.everit.json.schema.Schema;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.UpdateCenter;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO https://sonarsource.atlassian.net/browse/UPC-145
 */
class PluginsJsonGenerator extends JsonGenerator {

  private PluginsJsonGenerator(
    String resourceFile,
    UpdateCenter center,
    File outputDirectory,
    Log log,
    Gson gson,
    Schema jsonSchema) {
    super(resourceFile, center, outputDirectory, log, gson, jsonSchema);
  }

  public static PluginsJsonGenerator create(UpdateCenter center, File outputDirectory, Log log) {
    return JsonGenerator.create("plugin-schema.json",
      (resourceFile, jsonGenerator, jsonSchema) -> new PluginsJsonGenerator(
        resourceFile,
        center,
        outputDirectory,
        log,
        jsonGenerator,
        jsonSchema)
    );
  }

  void generateJsonFiles() throws IOException {

    List<Plugin> plugins = center.getUpdateCenterPluginReferential().getPlugins();

    for (Plugin plugin : plugins) {
      PluginModel pluginModel = new PluginModel(plugin, center.getSonar());

      final JsonOutput from = JsonOutput.createFrom(pluginModel);
      serializeToFile(plugin, from);
    }
  }

  private static class JsonOutput {
    @Expose
    private String name;
    @Expose
    private String key;
    @Expose
    private OrganizationData organization;
    @Expose
    private String category;
    @Expose
    private String license;
    @Expose
    private URL issueTrackerURL;
    @Expose
    private URL sourcesURL;
    @Expose
    private List<JsonOutputVersion> versions;

    private static class OrganizationData {
      @Expose
      private String name;
      @Expose
      private URL url;

      private OrganizationData(String name, @Nullable URL url) {
        this.name = name;
        this.url = url;
      }
    }

    private static class JsonOutputVersion {
      @Expose
      private String version;
      @Expose
      private String date;
      @Expose
      private String description;
      @Expose
      private Boolean archived;
      @Expose
      private String compatibility;
      @Expose
      private URL downloadURL;
      @Expose
      private URL changeLogUrl;

    }

    private static JsonOutput createFrom(PluginModel pluginModel) {
      JsonOutput returned = new JsonOutput();
      returned.name = pluginModel.getName();
      returned.key = pluginModel.getKey();
      if (pluginModel.getOrganization() != null) {
        returned.organization = new OrganizationData(
          pluginModel.getOrganization(),
          safeCreateURLFromString(pluginModel.getOrganizationUrl()));
      }
      returned.category = pluginModel.getCategory();
      returned.license = pluginModel.getLicense();
      returned.issueTrackerURL = safeCreateURLFromString(pluginModel.getIssueTracker());
      returned.sourcesURL = safeCreateURLFromString(pluginModel.getSources());

      returned.versions = pluginModel.getAllVersions()
        .stream()
        .map(pluginHeaderVersion -> {
          JsonOutputVersion becomeAJsonOutputVersion = new JsonOutput.JsonOutputVersion();
          becomeAJsonOutputVersion.version = pluginHeaderVersion.getVersion();
          becomeAJsonOutputVersion.date = pluginHeaderVersion.getDateAsIsoString();
          becomeAJsonOutputVersion.description = pluginHeaderVersion.getDescription();
          becomeAJsonOutputVersion.archived = pluginHeaderVersion.isArchived();
          becomeAJsonOutputVersion.compatibility = pluginHeaderVersion.getSonarVersionRange();
          becomeAJsonOutputVersion.downloadURL = safeCreateURLFromString(pluginHeaderVersion.getDownloadUrl());
          becomeAJsonOutputVersion.changeLogUrl = safeCreateURLFromString(pluginHeaderVersion.getChangelogUrl());
          return becomeAJsonOutputVersion;
        }).collect(Collectors.toList());

      return returned;
    }


  }
}
