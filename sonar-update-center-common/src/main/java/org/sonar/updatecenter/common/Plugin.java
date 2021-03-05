/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2021 SonarSource SA
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

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

public class Plugin extends Component {


  private boolean supportedBySonarSource = false;
  private boolean bundled = false;


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

  public boolean isSupportedBySonarSource() {
    return supportedBySonarSource;
  }

  public boolean isBundled() {
    return bundled;
  }

  @Override
  boolean needArtifact() {
    return true;
  }

  @Override
  boolean needSqVersion() {
    return true;
  }


  public Plugin setSupportedBySonarSource(boolean supportedBySonarSource) {
    this.supportedBySonarSource = supportedBySonarSource;
    return this;
  }

  public Plugin setBundled(boolean bundled) {
    this.bundled = bundled;
    return this;
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

      // from the properties file
      // supportedBySonarSource
    }
    return this;
  }

  public Release getReleaseForSonarVersion(String alias, Version sonarVersion) {
    if ("OLDEST_COMPATIBLE".equals(alias)) {
      return getFirstCompatible(sonarVersion);
    }
    throw new UnsupportedOperationException(alias + " is not a supported alias for plugin");
  }

}
