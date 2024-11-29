/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.updatecenter.common;

import java.util.List;

public abstract class Component extends Artifact {

  protected String name;
  protected String description;
  protected String homepageUrl;
  protected String license;
  protected String organization;
  protected String organizationUrl;
  protected String termsConditionsUrl;
  protected String category;
  protected String issueTrackerUrl;
  protected String sourcesUrl;
  protected List<String> developers;

  protected Component(String key) {
    super(key);
  }

  public String getName() {
    return name;
  }

  public Component setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Component setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getHomepageUrl() {
    return homepageUrl;
  }

  public Component setHomepageUrl(String url) {
    this.homepageUrl = url;
    return this;
  }

  public String getLicense() {
    return license;
  }

  public Component setLicense(String license) {
    this.license = license;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  public Component setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganizationUrl() {
    return organizationUrl;
  }

  public Component setOrganizationUrl(String url) {
    this.organizationUrl = url;
    return this;
  }

  public String getCategory() {
    return category;
  }

  public Component setCategory(String category) {
    this.category = category;
    return this;
  }

  public String getTermsConditionsUrl() {
    return termsConditionsUrl;
  }

  public Component setTermsConditionsUrl(String url) {
    this.termsConditionsUrl = url;
    return this;
  }

  public String getIssueTrackerUrl() {
    return issueTrackerUrl;
  }

  public Component setIssueTrackerUrl(String url) {
    this.issueTrackerUrl = url;
    return this;
  }

  public String getSourcesUrl() {
    return sourcesUrl;
  }

  public Component setSourcesUrl(String sourcesUrl) {
    this.sourcesUrl = sourcesUrl;
    return this;
  }

  public List<String> getDevelopers() {
    return developers;
  }

  public Component setDevelopers(List<String> developers) {
    this.developers = developers;
    return this;
  }

  abstract boolean needArtifact();

  abstract boolean needSqVersion();


  @Override
  public String toString() {
    return new StringBuilder()
      .append(key)
      .toString();
  }
}
