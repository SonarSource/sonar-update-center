/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2019 SonarSource SA
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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.UpdateCenter;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;

class PluginsJsonGenerator {

  private final File outputDirectory;
  private final UpdateCenter center;
  private final Log log;
  private final Gson gson;
  private final Schema jsonSchema;

  private PluginsJsonGenerator(
    UpdateCenter center,
    File outputDirectory,
    Log log,
    Gson gson,
    Schema jsonSchema) {
    this.outputDirectory = outputDirectory;
    this.center = center;
    this.log = log;
    this.gson = gson;
    this.jsonSchema = jsonSchema;
  }

  public static PluginsJsonGenerator create(UpdateCenter center, File outputDirectory, Log log) {
    Gson jsonGenerator = new GsonBuilder()
      .disableHtmlEscaping()
      .setPrettyPrinting()
      .create();

    Schema jsonSchema;
    try (InputStream inputStream = PluginsJsonGenerator.class.getResourceAsStream("/plugin-schema.json")) {
      JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
      jsonSchema = SchemaLoader.load(rawSchema);
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }

    return new PluginsJsonGenerator(
      center,
      outputDirectory,
      log,
      jsonGenerator,
      jsonSchema);
  }

  void generateJsonFiles() throws IOException {

    List<Plugin> plugins = center.getUpdateCenterPluginReferential().getPlugins();

    for (Plugin plugin : plugins) {
      PluginModel pluginModel = new PluginModel(plugin, center.getSonar());

      String jsonOutputString = gson.toJson(JsonOutput.createFrom(pluginModel));

      try {
        checkComplianceWithSchema(jsonOutputString);
      } catch (ValidationException exception) {
        log.error(plugin.getKey() + " json not compliant with schema");
        throw exception;
      }

      File file = new File(outputDirectory, plugin.getKey() + ".json");
      log.info("Generate json data for plugin " + plugin.getKey() + " in: " + file);

      FileUtils.writeStringToFile(file, jsonOutputString, UTF_8);
    }

    // copy the schema
    FileUtils.copyURLToFile(
      PluginsJsonGenerator.class.getResource("/plugin-schema.json"),
      new File(outputDirectory, "plugin-schema.json"));
  }

  private void checkComplianceWithSchema(String inputJson) {
    this.jsonSchema.validate(new JSONObject(inputJson));
  }

  private static class JsonOutput {
    @Expose
    private String name;
    @Expose
    private String key;
    @Expose
    private Boolean isSonarSourceCommercial;
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
      returned.isSonarSourceCommercial = pluginModel.isSonarSourceCommercialPlugin();
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

    @CheckForNull
    private static URL safeCreateURLFromString(@Nullable String mayBeAnURL) {
      if (mayBeAnURL == null) {
        return null;
      }
      try {
        return new URL(mayBeAnURL);
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }
}
