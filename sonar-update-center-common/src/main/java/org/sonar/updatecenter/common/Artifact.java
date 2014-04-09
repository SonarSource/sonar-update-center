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

import javax.annotation.CheckForNull;

import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class Artifact implements Comparable<Artifact> {

  protected String key;
  protected SortedSet<Release> releases = new TreeSet<Release>();
  private Release devRelease;

  protected Artifact(String key) {
    this.key = key;
  }

  public final String getKey() {
    return key;
  }

  public final Artifact setKey(String key) {
    this.key = key;
    return this;
  }

  public final Release setDevRelease(Release release) {
    devRelease = release;
    return release;
  }

  public final Release setDevRelease(Version version) {
    return setDevRelease(new Release(this, version));
  }

  public final Release setDevRelease(String version) {
    return setDevRelease(new Release(this, version));
  }

  public Release getDevRelease() {
    return devRelease;
  }

  public final Release addRelease(Release release) {
    releases.add(release);
    return release;
  }

  public final Release addRelease(Version version) {
    return addRelease(new Release(this, version));
  }

  public final Release addRelease(String version) {
    return addRelease(new Release(this, version));
  }

  /**
   * @throws java.util.NoSuchElementException if release could not be found
   */
  public final Release getRelease(Version version) {
    for (Release release : getReleases()) {
      if (release.getVersion().equals(version)) {
        return release;
      }
    }
    throw new NoSuchElementException();
  }

  public boolean doesContainVersion(Version version) {
    for (Release release : getReleases()) {
      if (release.getVersion().equals(version)) {
        return true;
      }
    }
    return false;
  }

  public final Release getRelease(String version) {
    return getRelease(Version.create(version));
  }

  public final SortedSet<Release> getReleases() {
    return releases;
  }

  /**
   * Shortcut for Ruby code
   */
  public final SortedSet<Release> getReleasesGreaterThan(String version) {
    return getReleasesGreaterThan(Version.create(version));
  }

  public final SortedSet<Release> getReleasesGreaterThan(Version version) {
    TreeSet<Release> result = new TreeSet<Release>();
    for (Release release : releases) {
      if (release.getVersion().compareTo(version) > 0) {
        result.add(release);
      }
    }
    return result;
  }

  public final SortedSet<Version> getVersions() {
    SortedSet<Version> versions = new TreeSet<Version>();
    for (Release release : releases) {
      versions.add(release.getVersion());
    }
    return versions;
  }

  public final Release getLastRelease() {
    return releases.isEmpty() ? null : releases.last();
  }

  public final Release getLastCompatibleRelease(Version sonarVersion) {
    Release result = null;
    for (Release r : releases) {
      if (r.supportSonarVersion(sonarVersion)) {
        result = r;
      }
    }
    return result;
  }

  @CheckForNull
  public final Release getMinimalRelease(Version minimalVersion) {
    for (Release r : releases) {
      if (r.getVersion().compareTo(minimalVersion) >= 0) {
        return r;
      }
    }
    return null;
  }

  public final Release getLastCompatibleReleaseIfUpgrade(Version sonarVersion) {
    Release result = null;
    for (Release r : releases) {
      if (r.getLastRequiredSonarVersion() != null && r.getLastRequiredSonarVersion().compareTo(sonarVersion) >= 0) {
        result = r;
      }
    }
    return result;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Artifact)) {
      return false;
    }
    Artifact artifact = (Artifact) o;
    return key.equals(artifact.key);
  }

  @Override
  public final int hashCode() {
    return key.hashCode();
  }

  public final int compareTo(Artifact other) {
    if (key == null) {
      return -1;
    }
    return key.compareTo(other.key);
  }
}
