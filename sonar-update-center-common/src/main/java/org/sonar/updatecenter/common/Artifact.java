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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.CheckForNull;

public abstract class Artifact implements Comparable<Artifact> {

  protected String key;
  protected SortedSet<Release> releases = new TreeSet<>();
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

  /**
   * Don't include dev version.
   */
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
    SortedSet<Release> result = new TreeSet<>();
    for (Release release : getAllReleases()) {
      if (release.getVersion().compareToIgnoreQualifier(version) > 0) {
        result.add(release);
      }
    }
    return result;
  }

  /**
   * @return both public and private versions but not archived versions
   */
  public final SortedSet<Version> getVersions() {
    SortedSet<Version> versions = new TreeSet<>();
    for (Release release : releases) {
      if (!release.isArchived()) {
        versions.add(release.getVersion());
      }
    }
    return versions;
  }

  public final SortedSet<Version> getPublicVersions() {
    SortedSet<Version> versions = new TreeSet<>();
    for (Release release : releases) {
      if (release.isPublic()) {
        versions.add(release.getVersion());
      }
    }
    return versions;
  }

  public final SortedSet<Release> getPublicReleases() {
    SortedSet<Release> publicReleases = new TreeSet<>();
    for (Release release : releases) {
      if (release.isPublic()) {
        publicReleases.add(release);
      }
    }
    return publicReleases;
  }

  public final SortedSet<Release> getArchivedReleases() {
    SortedSet<Release> archivedReleases = new TreeSet<>();
    for (Release release : releases) {
      if (release.isArchived()) {
        archivedReleases.add(release);
      }
    }
    return archivedReleases;
  }

  public final SortedSet<Version> getPrivateVersions() {
    SortedSet<Version> versions = new TreeSet<>();
    for (Release release : releases) {
      if (!release.isPublic() && !release.isArchived()) {
        versions.add(release.getVersion());
      }
    }
    return versions;
  }

  public final SortedSet<Version> getArchivedVersions() {
    SortedSet<Version> versions = new TreeSet<>();
    for (Release release : releases) {
      if (release.isArchived()) {
        versions.add(release.getVersion());
      }
    }
    return versions;
  }

  @CheckForNull
  public final Release getLastRelease() {
    return getReleases().isEmpty() ? null : getReleases().last();
  }

  /**
   * @return Greatest plugin release version that is compatible with provided SQ version
   */
  @CheckForNull
  public final Release getLastCompatibleRelease(Version sqVersion) {
    Release result = null;
    for (Release r : getAllReleases()) {
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
    for (Release r : getAllReleases()) {
      if (r.getVersion().compareToIgnoreQualifier(minimalVersion) >= 0) {
        return r;
      }
    }
    return null;
  }

  @CheckForNull
  public final Release getLastCompatibleReleaseIfUpgrade(Version sonarVersion) {
    Release result = null;
    for (Release r : getAllReleases()) {
      if (r.getLastRequiredSonarVersion() != null && r.getLastRequiredSonarVersion().compareToIgnoreQualifier(sonarVersion) >= 0) {
        result = r;
      }
    }
    return result;
  }

  /**
   * Return the concatenation of releases and dev release
   */
  public SortedSet<Release> getAllReleases() {
    SortedSet<Release> all = new TreeSet<>();
    all.addAll(getReleases());
    if (getDevRelease() != null) {
      all.add(getDevRelease());
    }
    return all;
  }

  /**
   * Keep only latest patch version. For example for 3.7, 3.7.1, 3.7.2 we keep only 3.7.2
   */
  public SortedSet<Release> getMajorReleases() {
    Map<String, Release> majorVersions = new LinkedHashMap<>();
    for (Release sq : getAllReleases()) {
      String displayVersion = sq.getVersion().getMajor() + "." + sq.getVersion().getMinor();
      majorVersions.put(displayVersion, sq);
    }
    return new TreeSet<>(majorVersions.values());
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

  @Override
  public final int compareTo(Artifact other) {
    if (key == null) {
      return -1;
    }
    return key.compareTo(other.key);
  }
}
