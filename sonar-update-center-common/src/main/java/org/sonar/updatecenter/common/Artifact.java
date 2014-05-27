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
    for (Release release : getAllReleases()) {
      if (release.getVersion().equals(version)) {
        return release;
      }
    }
    throw new NoSuchElementException("Unable to find a release of plugin " + key + " with version " + version);
  }

  public boolean doesContainVersion(Version version) {
    for (Release release : getAllReleases()) {
      if (release.getVersion().equals(version)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 
   * @param versionOrAliases Any version or keywords "DEV" or "LATEST_RELEASE"
   * @throws java.util.NoSuchElementException if release could not be found
   */
  public Release getRelease(String versionOrAliases) {
    if ("DEV".equals(versionOrAliases)) {
      return getDevRelease();
    }
    if ("LATEST_RELEASE".equals(versionOrAliases)) {
      return getLastRelease();
    }
    return getRelease(Version.create(versionOrAliases));
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

  /**
   * @return both public and private versions
   */
  public final SortedSet<Version> getVersions() {
    SortedSet<Version> versions = new TreeSet<Version>();
    for (Release release : releases) {
      versions.add(release.getVersion());
    }
    return versions;
  }

  public final SortedSet<Version> getPublicVersions() {
    SortedSet<Version> versions = new TreeSet<Version>();
    for (Release release : releases) {
      if (release.isPublic()) {
        versions.add(release.getVersion());
      }
    }
    return versions;
  }

  public final SortedSet<Release> getPublicReleases() {
    SortedSet<Release> publicReleases = new TreeSet<Release>();
    for (Release release : releases) {
      if (release.isPublic()) {
        publicReleases.add(release);
      }
    }
    return publicReleases;
  }

  public final SortedSet<Version> getPrivateVersions() {
    SortedSet<Version> versions = new TreeSet<Version>();
    for (Release release : releases) {
      if (!release.isPublic()) {
        versions.add(release.getVersion());
      }
    }
    return versions;
  }

  public final Release getLastRelease() {
    return releases.isEmpty() ? null : releases.last();
  }

  /**
   * @return Greatest plugin release version that is compatible with provided SQ version
   */
  @CheckForNull
  public final Release getLastCompatibleRelease(Version sqVersion) {
    Release result = null;
    for (Release r : releases) {
      if (r.supportSonarVersion(sqVersion)) {
        result = r;
      }
    }
    return result;
  }

  /**
   * Same as {@link #getLastCompatibleRelease(Version)} but include {@link #devRelease} if available
   */
  @CheckForNull
  public final Release getLastCompatible(Version sonarVersion) {
    Release result = null;
    for (Release r : getAllReleases()) {
      if (r.supportSonarVersion(sonarVersion)) {
        result = r;
      }
    }
    return result;
  }

  /**
   * Lowest plugin version (including dev) that is compatible with provided SQ version
   */
  @CheckForNull
  public final Release getFirstCompatible(Version sonarVersion) {
    for (Release r : getAllReleases()) {
      if (r.supportSonarVersion(sonarVersion)) {
        return r;
      }
    }
    return null;
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

  @CheckForNull
  public final Release getLastCompatibleReleaseIfUpgrade(Version sonarVersion) {
    Release result = null;
    for (Release r : releases) {
      if (r.getLastRequiredSonarVersion() != null && r.getLastRequiredSonarVersion().compareTo(sonarVersion) >= 0) {
        result = r;
      }
    }
    return result;
  }

  /**
   * Return the concatenation of releases and dev release
   */
  public SortedSet<Release> getAllReleases() {
    SortedSet<Release> all = new TreeSet<Release>();
    all.addAll(getReleases());
    if (getDevRelease() != null) {
      all.add(getDevRelease());
    }
    return all;
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
