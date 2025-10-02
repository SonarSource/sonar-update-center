/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2025 SonarSource SA
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
package org.sonar.updatecenter.common;

import java.util.List;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class Plugin extends Component {

  private Plugin(String key) {
    super(key);
  }

  public static Plugin factory(String key) {
    // in accordance with https://github.com/SonarSource/sonar-packaging-maven-plugin/blob/master/src/main/java/org/sonarsource/pluginpackaging/PluginKeyUtils.java#L44
    if (StringUtils.isAlphanumeric(key)) {
      return new Plugin(key);
    } else {
      throw new IllegalArgumentException("plugin key must be alphanumeric, strictly");
    }
  }

  @Override
  public Plugin setName(String name) {
    return (Plugin) super.setName(name);
  }

  @Override
  public Plugin setDescription(String description) {
    return (Plugin) super.setDescription(description);
  }

  @Override
  public Plugin setHomepageUrl(String url) {
    return (Plugin) super.setHomepageUrl(url);
  }

  @Override
  public Plugin setLicense(String license) {
    return (Plugin) super.setLicense(license);
  }

  @Override
  public Plugin setOrganization(String organization) {
    return (Plugin) super.setOrganization(organization);
  }

  @Override
  public Plugin setOrganizationUrl(String url) {
    return (Plugin) super.setOrganizationUrl(url);
  }

  @Override
  public Plugin setCategory(String category) {
    return (Plugin) super.setCategory(category);
  }

  @Override
  public Plugin setTermsConditionsUrl(String url) {
    return (Plugin) super.setTermsConditionsUrl(url);
  }

  @Override
  public Plugin setIssueTrackerUrl(String url) {
    return (Plugin) super.setIssueTrackerUrl(url);
  }

  @Override
  public Plugin setSourcesUrl(String sourcesUrl) {
    return (Plugin) super.setSourcesUrl(sourcesUrl);
  }

  @Override
  public Plugin setDevelopers(List developers) {
    return (Plugin) super.setDevelopers(developers);
  }

  @Override
  boolean needArtifact() {
    return true;
  }

  @Override
  boolean needSqVersion() {
    return true;
  }

  public Plugin merge(PluginManifest manifest) {
    if (StringUtils.equals(key, manifest.getKey())) {
      // from the manifest
      name = manifest.getName();

      // precedence to the manifest file
      organization = StringUtils.defaultIfEmpty(manifest.getOrganization(), organization);
      organizationUrl = StringUtils.defaultIfEmpty(manifest.getOrganizationUrl(), organizationUrl);
      license = StringUtils.defaultIfEmpty(manifest.getLicense(), license);
      termsConditionsUrl = StringUtils.defaultIfEmpty(manifest.getTermsConditionsUrl(), termsConditionsUrl);
      developers = Arrays.asList(manifest.getDevelopers());

      // precedence to the update center file
      description = StringUtils.defaultIfEmpty(description, manifest.getDescription());
      issueTrackerUrl = StringUtils.defaultIfEmpty(issueTrackerUrl, manifest.getIssueTrackerUrl());
      homepageUrl = StringUtils.defaultIfEmpty(homepageUrl, manifest.getHomepage());
      sourcesUrl = StringUtils.defaultIfEmpty(sourcesUrl, manifest.getSourcesUrl());
    }
    return this;
  }

}
