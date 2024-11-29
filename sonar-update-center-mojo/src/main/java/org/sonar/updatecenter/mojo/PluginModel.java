/*
 * SonarSource :: Update Center :: Maven Plugin
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
package org.sonar.updatecenter.mojo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.Version;

public class PluginModel {

  private Plugin plugin;
  private Sonar sonar;

  public PluginModel(Plugin plugin, Sonar sonar) {
    this.plugin = plugin;
    this.sonar = sonar;
  }

  public String getKey() {
    return plugin.getKey();
  }

  public String getName() {
    return plugin.getName();
  }

  public String getIssueTracker() {
    return plugin.getIssueTrackerUrl();
  }

  public String getSources() {
    return plugin.getSourcesUrl();
  }

  public String getLicense() {
    return plugin.getLicense();
  }

  public String getOrganization() {
    return plugin.getOrganization();
  }

  public String getOrganizationUrl() {
    return plugin.getOrganizationUrl();
  }

  public String getCategory() {
    return plugin.getCategory();
  }

  public List<ReleaseModel> getAllVersions() {
    List<ReleaseModel> result = new LinkedList<>();
    for (Release r : plugin.getAllReleases()) {
      // Add in reverse order to have greater version on top
      result.add(0, new ReleaseModel(r, this.sonar));
    }
    return result;
  }

}
