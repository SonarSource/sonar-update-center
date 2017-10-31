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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class EditionTemplate {

  private final String key;
  private final String name;
  private final String description;
  private final String homeUrl;
  private final String requestUrl;
  private final Set<String> pluginKeys;

  private EditionTemplate(Builder builder) {
    this.key = Objects.requireNonNull(builder.key);
    this.name = Objects.requireNonNull(builder.name);
    this.description = Objects.requireNonNull(builder.description);
    this.homeUrl = Objects.requireNonNull(builder.homeUrl);
    this.requestUrl = Objects.requireNonNull(builder.requestUrl);
    this.pluginKeys = Collections.unmodifiableSet(builder.pluginKeys);
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public String getTextDescription() {
    return description;
  }

  public String getHomeUrl() {
    return homeUrl;
  }

  public String getRequestUrl() {
    return requestUrl;
  }

  public Set<String> getPluginKeys() {
    return pluginKeys;
  }

  public static class Builder {
    private String key;
    private String name;
    private String description;
    private String homeUrl;
    private String requestUrl;
    private Set<String> pluginKeys = Collections.emptySet();

    public Builder setKey(String s) {
      this.key = s;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setTextDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder setHomeUrl(String homeUrl) {
      this.homeUrl = homeUrl;
      return this;
    }

    public Builder setRequestLicenseUrl(String requestUrl) {
      this.requestUrl = requestUrl;
      return this;
    }

    public Builder setPluginKeys(Collection<String> pluginKeys) {
      this.pluginKeys = new HashSet<>(pluginKeys);
      return this;
    }

    public EditionTemplate build() {
      return new EditionTemplate(this);
    }
  }

}
