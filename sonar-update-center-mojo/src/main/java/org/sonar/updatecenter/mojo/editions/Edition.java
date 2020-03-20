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
package org.sonar.updatecenter.mojo.editions;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;

import static java.util.Objects.requireNonNull;

public class Edition {

  private final String sonarQubeVersion;
  private final String key;
  private final String name;
  private final String textDescription;
  private final String homeUrl;
  private final String requestUrl;
  private final String zipFileName;
  private final Set<String> jars;

  private Edition(Builder builder) {
    this.sonarQubeVersion = requireNonNull(builder.sonarQubeVersion);
    this.key = requireNonNull(builder.key);
    this.name = requireNonNull(builder.name);
    this.textDescription = requireNonNull(builder.textDescription);
    this.homeUrl = requireNonNull(builder.homeUrl);
    this.requestUrl = requireNonNull(builder.requestUrl);
    this.zipFileName = builder.zipFileName;
    this.jars = requireNonNull(builder.jars);
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

  @CheckForNull
  public String getZipFileName() {
    return zipFileName;
  }

  public Set<String> jars() {
    return jars;
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
    private String zipFileName;
    private final Set<String> jars = new LinkedHashSet<>();

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

    public Builder addJar(String jar) {
      this.jars.add(jar);
      return this;
    }

    public Builder setZipFileName(String zipFileName) {
      this.zipFileName = zipFileName;
      return this;
    }

    public Edition build() {
      return new Edition(this);
    }
  }

  @CheckForNull
  public String getDownloadUrl(String downloadBaseUrl) {
    if (getZipFileName() == null) {
      return null;
    }
    return String.format("%s/%s", StringUtils.removeEnd(downloadBaseUrl, "/"), getZipFileName());
  }
}
