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

import java.util.SortedSet;
import java.util.TreeSet;

public class Sonar extends Artifact {

  private SortedSet<Release> nextReleases = new TreeSet<Release>();
  private Release ltsRelease;

  public Sonar() {
    super("sonar");
  }

  public final Release addNextRelease(Release release) {
    nextReleases.add(release);
    return release;
  }

  public final Release addNextRelease(Version version) {
    return addNextRelease(new Release(this, version));
  }

  public final Release addNextRelease(String version) {
    return addNextRelease(new Release(this, version));
  }

  public SortedSet<Release> getNextReleases() {
    return nextReleases;
  }

  public void setLtsRelease(String ltsVersion) {
    this.ltsRelease = new Release(this, Version.create(ltsVersion));
  }

  public Release getLtsRelease() {
    return ltsRelease;
  }

  /**
   * Return the concatenation of releases and next releases
   */
  public SortedSet<Release> getAllReleases() {
    SortedSet<Release> all = new TreeSet<Release>();
    all.addAll(getReleases());
    all.addAll(getNextReleases());
    return all;
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
