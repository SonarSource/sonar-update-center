/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2018 SonarSource SA
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.UpdateCenter;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;

class PluginsDataJsonGenerator {

  private final File outputDirectory;
  private final UpdateCenter center;
  private final Log log;
  private final Gson gson;

  PluginsDataJsonGenerator(UpdateCenter center, File outputDirectory, Log log) {
    this.outputDirectory = outputDirectory;
    this.center = center;
    this.log = log;
    this.gson = new GsonBuilder()
      .disableHtmlEscaping()
      .setPrettyPrinting()
      .create();

  }

  void generateJsonFiles() throws IOException {

    List<Plugin> plugins = center.getUpdateCenterPluginReferential().getPlugins();

    for (Plugin plugin : plugins) {
      PluginHeader pluginHeader = new PluginHeader(plugin, center.getSonar());

      String jsonOutputString = gson.toJson(JsonOutput.createFrom(pluginHeader));

      // TODO check generate is compatible with schema

      File file = new File(outputDirectory, plugin.getKey() + ".json");
      log.info("Generate json data for plugin " + plugin.getKey() + " in: " + file);

      FileUtils.writeStringToFile(file, jsonOutputString, UTF_8);
    }
  }

  private static class JsonOutput {
    private String name;
    private String key;
    private Boolean isSonarSourceCommercial;
    private OrganizationData organization;
    private String category;
    private String license;
    private URL issueTrackerURL;
    private URL sourcesURL;
    private List<JsonOutputVersion> versions;

    private static class OrganizationData {
      private String name;
      private URL url;

      private OrganizationData(String name, URL url) {
        this.name = name;
        this.url = url;
      }
    }

    private static class JsonOutputVersion {
      private String version;
      private String date;
      private String description;
      private Boolean archived;
      private String compatibilityRange;
      private URL downloadURL;
      private URL changeLogUrl;

    }

    private static JsonOutput createFrom(PluginHeader pluginHeader) {
      JsonOutput returned = new JsonOutput();
      returned.name = pluginHeader.getName();
      returned.key = pluginHeader.getKey();
      returned.isSonarSourceCommercial = pluginHeader.isSonarSourceCommercialPlugin();
      if (pluginHeader.getOrganization() != null) {
        returned.organization = new OrganizationData(
          pluginHeader.getOrganization(),
          safeCreateURLFromString(pluginHeader.getOrganizationUrl()));
      }
      returned.category = pluginHeader.getCategory();
      returned.license = pluginHeader.getLicense();
      returned.issueTrackerURL = safeCreateURLFromString(pluginHeader.getIssueTracker());
      returned.sourcesURL = safeCreateURLFromString(pluginHeader.getSources());

      returned.versions = pluginHeader.getAllVersions()
        .stream()
        .map(pluginHeaderVersion -> {
          JsonOutputVersion becomeAJsonOutputVersion = new JsonOutput.JsonOutputVersion();
          becomeAJsonOutputVersion.version = pluginHeaderVersion.getVersion();
          becomeAJsonOutputVersion.date = pluginHeaderVersion.getDateAsIsoString();
          becomeAJsonOutputVersion.description = pluginHeaderVersion.getDescription();
          becomeAJsonOutputVersion.archived = pluginHeaderVersion.isArchived();
          becomeAJsonOutputVersion.compatibilityRange = pluginHeaderVersion.getSonarVersionRange();
          becomeAJsonOutputVersion.downloadURL = safeCreateURLFromString(pluginHeaderVersion.getDownloadUrl());
          becomeAJsonOutputVersion.changeLogUrl = safeCreateURLFromString(pluginHeaderVersion.getChangelogUrl());
          return becomeAJsonOutputVersion;
        }).collect(Collectors.toList());

      return returned;
    }
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
