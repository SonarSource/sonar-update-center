/*
 * SonarSource :: Update Center :: Maven Plugin
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
package org.sonar.updatecenter.mojo.editions.generators;

import java.util.HashMap;
import java.util.Map;

public class HtmlEditionModel {
  private final String editionName;
  private final Map<String, String> urlByRealVersion = new HashMap<>();

  public HtmlEditionModel(String editionName) {
    this.editionName = editionName;
  }

  public String getName() {
    return editionName;
  }

  public void add(String sqRealVersion, String downloadUrl) {
    urlByRealVersion.put(sqRealVersion, downloadUrl);
  }

  /**
   * @param sqVersion SonarQube version as a string
   * @throws IllegalArgumentException is the provided version is not supported by the edition
   * @return download url as String
   */
  public String getDownloadUrlForSQVersion(String sqVersion) {
    String version = urlByRealVersion.get(sqVersion);
    if (version == null) {
      throw new IllegalArgumentException("Version " + sqVersion + " is not supported by the edition " + editionName);
    }
    return version;
  }

  public boolean supports(String sqVersion) {
    return urlByRealVersion.get(sqVersion) != null;
  }
}
