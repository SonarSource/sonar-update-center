/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2020 SonarSource SA
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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import org.apache.maven.plugin.logging.Log;
import org.everit.json.schema.Schema;
import org.sonar.updatecenter.common.Scanner;
import org.sonar.updatecenter.common.UpdateCenter;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScannerJsonGenerator extends JsonGenerator{

  protected ScannerJsonGenerator(String resourceFile, UpdateCenter center, File outputDirectory, Log log, Gson gson, Schema jsonSchema) {
    super(resourceFile, center, outputDirectory, log, gson, jsonSchema);
  }

  public static ScannerJsonGenerator create(UpdateCenter center, File outputDirectory, Log log) {
    return JsonGenerator.create("scanner-schema.json",
      (resourceFile, jsonGenerator, jsonSchema) -> new ScannerJsonGenerator(
        resourceFile,
        center,
        outputDirectory,
        log,
        jsonGenerator,
        jsonSchema)
    );
  }

  void generateJsonFiles() throws IOException {

    List<Scanner> scanners = center.getScanners();

    for (Scanner scanner : scanners) {
      ScannerModel scannerModel = new ScannerModel(scanner, center.getSonar());

      final ScannerJsonGenerator.JsonOutput from = ScannerJsonGenerator.JsonOutput.createFrom(scannerModel);
      serializeToFile(scanner, from);
    }
  }

  private static class JsonOutput {
    @Expose
    private String name;
    @Expose
    private String key;
    @Expose
    private String category;
    @Expose
    private String license;
    @Expose
    private URL issueTrackerURL;
    @Expose
    private URL sourcesURL;
    @Expose
    private List<ScannerJsonGenerator.JsonOutput.JsonOutputVersion> versions;
    @Expose
    private ScannerJsonGenerator.JsonOutput.OrganizationData organization;


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

    private static class JsonFlavoredUrl {
      @Expose
      private String label;
      @Expose
      private  URL url;

      public JsonFlavoredUrl(String label, URL url) {
        this.label = label;
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
      private List<JsonFlavoredUrl> downloadURL;
      @Expose
      private URL changeLogUrl;

    }

    private static ScannerJsonGenerator.JsonOutput createFrom(ScannerModel scannerModel) {
      ScannerJsonGenerator.JsonOutput returned = new ScannerJsonGenerator.JsonOutput();
      returned.name = scannerModel.getName();
      returned.key = scannerModel.getKey();
      if (scannerModel.getOrganization() != null) {
        returned.organization = new ScannerJsonGenerator.JsonOutput.OrganizationData(
          scannerModel.getOrganization(),
          safeCreateURLFromString(scannerModel.getOrganizationUrl()));
      }
      returned.category = scannerModel.getCategory();
      returned.license = scannerModel.getLicense();
      returned.issueTrackerURL = safeCreateURLFromString(scannerModel.getIssueTracker());
      returned.sourcesURL = safeCreateURLFromString(scannerModel.getSources());

      returned.versions = scannerModel.getAllVersions()
        .stream()
        .map(scannerHeaderVersion -> {
          JsonOutputVersion becomeAJsonOutputVersion = new JsonOutputVersion();
          becomeAJsonOutputVersion.version = scannerHeaderVersion.getVersion();
          becomeAJsonOutputVersion.date = scannerHeaderVersion.getDateAsIsoString();
          becomeAJsonOutputVersion.description = scannerHeaderVersion.getDescription();
          becomeAJsonOutputVersion.archived = scannerHeaderVersion.isArchived();
          becomeAJsonOutputVersion.compatibility = scannerHeaderVersion.getSonarVersionRange();
          becomeAJsonOutputVersion.downloadURL = Lists.transform(
            scannerHeaderVersion.getScannerDownloadUrl(),
            url -> new JsonFlavoredUrl(url.getLabel(), safeCreateURLFromString(url.getUrl()))
          );
          if (becomeAJsonOutputVersion.downloadURL.isEmpty()) {
            becomeAJsonOutputVersion.downloadURL  = new ArrayList<>();
            becomeAJsonOutputVersion.downloadURL.add(new JsonFlavoredUrl(null, safeCreateURLFromString(scannerHeaderVersion.getDownloadUrl())));
          }
          becomeAJsonOutputVersion.changeLogUrl = safeCreateURLFromString(scannerHeaderVersion.getChangelogUrl());
          return becomeAJsonOutputVersion;
        }).collect(Collectors.toList());

      return returned;
    }

  }
}
