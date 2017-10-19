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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;

import static java.util.Objects.requireNonNull;

public class Edition {

  private final String sonarQubeVersion;
  private final String key;
  private final String name;
  private final String textDescription;
  private final String homeUrl;
  private final String requestUrl;
  private final File zip;

  private Edition(Builder builder) throws IOException {
    this.sonarQubeVersion = requireNonNull(builder.sonarQubeVersion);
    this.key = requireNonNull(builder.key);
    this.name = requireNonNull(builder.name);
    this.textDescription = requireNonNull(builder.textDescription);
    this.homeUrl = requireNonNull(builder.homeUrl);
    this.requestUrl = requireNonNull(builder.requestUrl);

    this.zip = zip(builder.targetZip, builder.jarsInZip);
  }

  public String getSonarQubeVersion() {
    return sonarQubeVersion;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public String getTextDescription() {
    return textDescription;
  }

  public String getHomeUrl() {
    return homeUrl;
  }

  public String getRequestUrl() {
    return requestUrl;
  }

  public File getZip() {
    return zip;
  }

  private static File zip(File zipFile, List<File> files) throws IOException {
    try (ZipOutputStream zipOutput = new ZipOutputStream(new FileOutputStream(zipFile, false))) {
      for (File file : files) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
          ZipEntry entry = new ZipEntry(file.getName());
          zipOutput.putNextEntry(entry);
          IOUtils.copy(in, zipOutput);
          zipOutput.closeEntry();
        }
      }
    }
    return zipFile;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Edition edition = (Edition) o;

    return key.equals(edition.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  public static class Builder {
    private String sonarQubeVersion;
    private String key;
    private String name;
    private String textDescription;
    private String homeUrl;
    private String requestUrl;
    private File targetZip;
    private final List<File> jarsInZip = new ArrayList<>();

    public Builder setSonarQubeVersion(String sonarQubeVersion) {
      this.sonarQubeVersion = sonarQubeVersion;
      return this;
    }

    public Builder setKey(String key) {
      this.key = key;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setTextDescription(String textDescription) {
      this.textDescription = textDescription;
      return this;
    }

    public Builder setHomeUrl(String homeUrl) {
      this.homeUrl = homeUrl;
      return this;
    }

    public Builder setRequestUrl(String requestUrl) {
      this.requestUrl = requestUrl;
      return this;
    }

    public Builder addJar(File jar) {
      if (!jar.exists() || !jar.isFile()) {
        throw new IllegalArgumentException("File does not exist: " + jar);
      }
      this.jarsInZip.add(jar);
      return this;
    }

    public Builder setTargetZip(File zip) {
      this.targetZip = zip;
      return this;
    }

    public Edition build() throws IOException {
      return new Edition(this);
    }
  }
}
