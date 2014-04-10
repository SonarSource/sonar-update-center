/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.updatecenter.common;

public class Sonar extends Artifact {

  private Release ltsVersion;

  public Sonar() {
    super("sonar");
  }

  public void setLtsRelease(String ltsVersion) {
    this.ltsVersion = new Release(this, Version.create(ltsVersion));
  }

  public Release getLtsRelease() {
    return ltsVersion;
  }

  /**
   * shortcut only for sonar, no need to have other fields than version
   */
  public Sonar setReleases(String[] versions) {
    for (String version : versions) {
      addRelease(new Release(this, Version.create(version)));
    }
    return this;
  }

  /**
   * 
   * @param versionOrAliases Any version or keywords "DEV", "LTS" or "LATEST_RELEASE"
   * @return
   */
  @Override
  public Release getRelease(String versionOrAliases) {
    if ("LTS".equals(versionOrAliases)) {
      return getLtsRelease();
    }
    return super.getRelease(versionOrAliases);
  }
}
