/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.updatecenter.common;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

public final class Plugin extends Artifact {

  private String name;
  private String parent;
  private String requiresPlugins;
  private String description;
  private String homepageUrl;
  private String license;
  private String organization;
  private String organizationUrl;
  private String termsConditionsUrl;
  private String category;
  private String issueTrackerUrl;
  private String sourcesUrl;
  private List<String> developers;

  public Plugin(String key) {
    super(key);
  }

  public String getName() {
    return name;
  }

  public Plugin setName(String name) {
    this.name = name;
    return this;
  }

  public String getParent() {
    return parent;
  }

  public Plugin setParent(String parent) {
    this.parent = parent;
    return this;
  }

  public String getRequiresPlugins() {
    return requiresPlugins;
  }

  public Plugin setRequiresPlugins(String requiresPlugins) {
    this.requiresPlugins = requiresPlugins;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Plugin setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getHomepageUrl() {
    return homepageUrl;
  }

  public Plugin setHomepageUrl(String s) {
    this.homepageUrl = s;
    return this;
  }

  public String getLicense() {
    return license;
  }

  public Plugin setLicense(String license) {
    this.license = license;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  public Plugin setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganizationUrl() {
    return organizationUrl;
  }

  public Plugin setOrganizationUrl(String url) {
    this.organizationUrl = url;
    return this;
  }

  public String getCategory() {
    return category;
  }

  public Plugin setCategory(String category) {
    this.category = category;
    return this;
  }

  public String getTermsConditionsUrl() {
    return termsConditionsUrl;
  }

  public Plugin setTermsConditionsUrl(String url) {
    this.termsConditionsUrl = url;
    return this;
  }

  public String getIssueTrackerUrl() {
    return issueTrackerUrl;
  }

  public Plugin setIssueTrackerUrl(String url) {
    this.issueTrackerUrl = url;
    return this;
  }

  public String getSourcesUrl() {
    return sourcesUrl;
  }

  public Plugin setSourcesUrl(String sourcesUrl) {
    this.sourcesUrl = sourcesUrl;
    return this;
  }

  public List<String> getDevelopers() {
    return developers;
  }

  public Plugin setDevelopers(List<String> developers) {
    this.developers = developers;
    return this;
  }

  public Plugin merge(PluginManifest manifest) {
    if (StringUtils.equals(key, manifest.getKey())) {
      name = manifest.getName();
      parent = manifest.getParent();
      requiresPlugins = manifest.getRequiresPlugins();
      description = StringUtils.defaultIfEmpty(description, manifest.getDescription());
      organization = StringUtils.defaultIfEmpty(organization, manifest.getOrganization());
      organizationUrl = StringUtils.defaultIfEmpty(organizationUrl, manifest.getOrganizationUrl());
      issueTrackerUrl = StringUtils.defaultIfEmpty(issueTrackerUrl, manifest.getIssueTrackerUrl());
      license = StringUtils.defaultIfEmpty(license, manifest.getLicense());
      homepageUrl = StringUtils.defaultIfEmpty(homepageUrl, manifest.getHomepage());
      termsConditionsUrl = StringUtils.defaultIfEmpty(termsConditionsUrl, manifest.getTermsConditionsUrl());
      sourcesUrl = StringUtils.defaultIfEmpty(sourcesUrl, manifest.getSourcesUrl());
      developers = Arrays.asList(manifest.getDevelopers());
    }
    return this;
  }
}
