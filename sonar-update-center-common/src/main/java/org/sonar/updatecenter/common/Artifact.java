/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2025 SonarSource SA
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
import javax.annotation.Nullable;

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

  /**
   * @throws java.util.NoSuchElementException if release could not be found
   */
  public final Release getRelease(Version version) {
    return getRelease(version, null);
  }

  /**
   * @throws java.util.NoSuchElementException if release could not be found
   */
  public final Release getRelease(Version version, @Nullable Product product) {
    for (Release release : getAllReleases(product)) {
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
    return getRelease(versionOrAliases, null);
  }

  public Release getRelease(String versionOrAliases, @Nullable Product product) {
    if ("DEV".equals(versionOrAliases)) {
      return getDevRelease();
    }
    if ("LATEST_RELEASE".equals(versionOrAliases)) {
      return getLastRelease();
    }
    return getRelease(Version.create(versionOrAliases), product);
  }

  /**
   * Don't include dev version.
   */
  public final SortedSet<Release> getReleases() {
    return releases;
  }

  public final SortedSet<Release> getReleases(@Nullable Product product) {
    return releases.stream().filter(r -> r.getProduct() == product).collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
  }

  /**
   * Shortcut for Ruby code (?)
   */
  public final SortedSet<Release> getReleasesGreaterThan(String version, @Nullable Product product) {
    return getReleasesGreaterThan(Version.create(version), product);
  }

  public final SortedSet<Release> getReleasesGreaterThan(Version version, @Nullable Product product) {
    SortedSet<Release> result = new TreeSet<>();
    for (Release release : getAllReleases(product)) {
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
    return getPublicVersions(null);
  }

  public final SortedSet<Version> getPublicVersions(@Nullable Product product) {
    SortedSet<Version> versions = new TreeSet<>();
    for (Release release : releases) {
      if (release.getProduct() == product && release.isPublic()) {
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
    return getLastRelease(null);
  }

  @CheckForNull
  public final Release getLastRelease(@Nullable Product product) {
    return getReleases(product).isEmpty() ? null : getReleases(product).last();
  }

  @CheckForNull
  public final Release getLastCompatible(Release release) {
    return getLastCompatible(release.getVersion(), release.getProduct());
  }

  @CheckForNull
  public final Release getLastCompatible(Version version, Product product) {
    Release result = null;
    for (Release r : getReleases()) {
      if (r.supportSonarVersion(version, product)) {
        result = r;
      }
    }
    return result;
  }

  /**
   * Same as {@link #getLastCompatible(Release)} but include {@link #devRelease} if available
   */
  @CheckForNull
  public final Release getLastCompatibleIncludingDev(Version sonarVersion, Product product) {
    Release result = null;
    for (Release r : getAllReleases()) {
      if (r.supportSonarVersion(sonarVersion, product)) {
        result = r;
      }
    }
    return result;
  }

  /**
   * Lowest plugin version (including dev) that is compatible with provided SQ version
   */
  @CheckForNull
  public final Release getFirstCompatible(Version sonarVersion, Product product) {
    for (Release r : getAllReleases()) {
      if (r.supportSonarVersion(sonarVersion, product)) {
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
  public final Release getLastCompatibleReleaseIfUpgrade(Version sonarVersion, Product product) {
    Release result = null;
    for (Release r : getAllReleases()) {
      if (r.getLastRequiredSonarVersion(product) != null && r.getLastRequiredSonarVersion(product).compareToIgnoreQualifier(sonarVersion) >= 0) {
        result = r;
      }
    }
    return result;
  }

  /**
   * Returns the union of releases and dev release
   */
  public SortedSet<Release> getAllReleases() {
    return getAllReleases(null);
  }

  public SortedSet<Release> getAllReleases(@Nullable Product product) {
    SortedSet<Release> all = new TreeSet<>();
    all.addAll(getReleases(product));
    if (getDevRelease() != null) {
      all.add(getDevRelease());
    }
    return all;
  }

  /**
   * Keep only latest patch version. For example for 3.7, 3.7.1, 3.7.2 we keep only 3.7.2
   */
  public SortedSet<Release> getMajorReleases() {
    return getMajorReleases(null);
  }

  public SortedSet<Release> getMajorReleases(@Nullable Product product) {
    Map<String, Release> majorVersions = new LinkedHashMap<>();
    for (Release sq : getAllReleases(product)) {
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
