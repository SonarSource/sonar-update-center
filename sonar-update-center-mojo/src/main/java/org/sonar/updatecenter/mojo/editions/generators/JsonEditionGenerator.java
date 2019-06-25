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
package org.sonar.updatecenter.mojo.editions.generators;

import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.mojo.editions.Edition;

import static java.nio.charset.StandardCharsets.UTF_8;

public class JsonEditionGenerator implements EditionGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonEditionGenerator.class);
  static final String FILE_NAME = "editions.json";
  private final String downloadBaseUrl;

  public JsonEditionGenerator(String downloadBaseUrl) {
    this.downloadBaseUrl = downloadBaseUrl;
  }

  @Override
  public void generate(File outputDir, List<Edition> editions) throws IOException {
    File jsonOutput = new File(outputDir, FILE_NAME);
    try (Writer jsonWriter = new OutputStreamWriter(new FileOutputStream(jsonOutput), UTF_8)) {
      LOGGER.info("Generate {}", jsonOutput.getAbsolutePath());
      write(editions, downloadBaseUrl, jsonWriter);
    }
  }

  private void write(List<Edition> editions, String downloadBaseUrl, Writer writer) throws IOException {
    SortedMap<String, SortedSet<Edition>> editionsPerVersion = new TreeMap<>();
    for (Edition edition : editions) {
      editionsPerVersion
        .computeIfAbsent(edition.getSonarQubeVersion(), k -> new TreeSet<>(Comparator.comparing(Edition::getKey)))
        .add(edition);
    }

    JsonWriter json = new JsonWriter(writer);
    json.setIndent("  ");
    json.beginObject();
    for (Map.Entry<String, SortedSet<Edition>> entry : editionsPerVersion.entrySet()) {
      json.name(entry.getKey());
      json.beginArray();
      for (Edition e : entry.getValue()) {
        json.beginObject();
        json.name("key").value(e.getKey());
        json.name("name").value(e.getName());
        json.name("textDescription").value(e.getTextDescription());
        json.name("homeUrl").value(e.getHomeUrl());
        json.name("licenseRequestUrl").value(e.getRequestUrl());
        String downloadUrl = e.getDownloadUrl(downloadBaseUrl);
        if (downloadUrl != null) {
          json.name("downloadUrl").value(downloadUrl);
        }
        json.endObject();
      }
      json.endArray();
    }
    json.endObject();
    json.close();
  }
}
