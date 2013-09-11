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

public class Sonar extends Artifact {

  private Release nextRelease;
  private Release ltsRelease;

  public Sonar() {
    super("sonar");
  }

  public void setNextRelease(String nextVersion) {
    this.nextRelease = new Release(this, Version.create(nextVersion));
  }

  public Release getNextRelease() {
    return nextRelease;
  }

  public void setLtsRelease(String ltsVersion) {
    this.ltsRelease = new Release(this, Version.create(ltsVersion));
  }

  public Release getLtsRelease() {
    return ltsRelease;
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
}
