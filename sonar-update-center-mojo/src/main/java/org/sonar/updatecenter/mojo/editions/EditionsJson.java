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

import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;

public class EditionsJson {

  public void write(List<Edition> editions, String downloadBaseUrl, Writer writer) throws IOException {
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
        json.name("downloadUrl").value(getDownloadUrl(e, downloadBaseUrl));
        json.endObject();
      }
      json.endArray();
    }
    json.endObject();
    json.close();
  }

  private static String getDownloadUrl(Edition edition, String downloadBaseUrl) {
    File zip = edition.getZip();
    return String.format("%s/%s", StringUtils.removeEnd(downloadBaseUrl, "/"), zip.getName());
  }
}
