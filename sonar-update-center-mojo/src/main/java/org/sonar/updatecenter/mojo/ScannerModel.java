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
package org.sonar.updatecenter.mojo;

import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Scanner;
import org.sonar.updatecenter.common.Sonar;

import java.util.LinkedList;
import java.util.List;

public class ScannerModel {
  private Scanner scanner;
  private Sonar sonar;

  public ScannerModel(Scanner scanner, Sonar sonar) {
    this.scanner = scanner;
    this.sonar = sonar;
  }

  public String getKey() {
    return scanner.getKey();
  }

  public String getName() {
    return scanner.getName();
  }

  public String getIssueTracker() {
    return scanner.getIssueTrackerUrl();
  }

  public String getSources() {
    return scanner.getSourcesUrl();
  }

  public String getLicense() {
    return scanner.getLicense();
  }

  public String getOrganization() {
    return scanner.getOrganization();
  }

  public String getOrganizationUrl() {
    return scanner.getOrganizationUrl();
  }

  public String getCategory() {
    return scanner.getCategory();
  }

  public List<ReleaseModel> getAllVersions() {
    List<ReleaseModel> result = new LinkedList<>();
    for (Release r : scanner.getAllReleases()) {
      // Add in reverse order to have greater version on top
      result.add(0, new ReleaseModel(r, this.sonar));
    }
    return result;
  }
}
