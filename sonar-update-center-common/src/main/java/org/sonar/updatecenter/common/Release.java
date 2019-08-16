/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2019 SonarSource SA
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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import static java.util.Collections.unmodifiableSet;

public class Release implements Comparable<Release> {

  private Artifact artifact;
  private Version version;
  private String displayVersion;
  private String description;
  private URL changelogUrl;
  private boolean isPublic;
  private boolean isArchived;
  private String groupId;
  private String artifactId;

  private EnumMap<Edition, URL> downloadUrl;
  private Set<Release> outgoingDependencies;
  private Set<Release> incomingDependencies;
  /**
   * from oldest to newest sonar versions
   */
  private SortedSet<Version> compatibleSqVersions;
  private Date date;

  public enum Edition {
    COMMUNITY,
    DEVELOPER,
    ENTERPRISE,
    DATACENTER
  }

  public Release(Artifact artifact, Version version) {
    this.artifact = artifact;
    this.version = version;
    this.isPublic = true;
    this.isArchived = false;

    this.downloadUrl = new EnumMap<>(Edition.class);
    this.compatibleSqVersions = new TreeSet<>();
    this.outgoingDependencies = new HashSet<>();
    this.incomingDependencies = new HashSet<>();
  }

  public Release(Artifact artifact, String version) {
    this(artifact, Version.create(version));
  }

  public Artifact getArtifact() {
    return artifact;
  }

  public Version getVersion() {
    return version;
  }

  public Release setVersion(Version version) {
    this.version = version;
    return this;
  }

  public String getDisplayVersion() {
    return displayVersion;
  }

  public Release setDisplayVersion(String displayVersion) {
    this.displayVersion = displayVersion;
    return this;
  }

  @CheckForNull
  public String getDownloadUrl() {
    return getDownloadUrl(Edition.COMMUNITY);
  }

  @CheckForNull
  public String getDownloadUrl(Edition edition) {
    URL value = this.downloadUrl.get(edition);
    return value == null ? null : value.toString();
  }

  public Release setDownloadUrl(@Nullable String downloadUrlString) {
    return setDownloadUrl(downloadUrlString, Edition.COMMUNITY);
  }

  public Release setDownloadUrl(@Nullable String downloadUrlString, Edition edition) {
    URL transformedDownloadUrl = null;
    if (downloadUrlString != null) {
      try {
        // URI does more checks on syntax than URL
        transformedDownloadUrl = new URI(downloadUrlString).toURL();
      } catch (URISyntaxException | MalformedURLException ex) {
        throw new IllegalArgumentException("downloadUrl invalid", ex);
      }
    }
    this.downloadUrl.put(edition, transformedDownloadUrl);
    return this;
  }

  @CheckForNull
  public String getFilename() {
    return getFilename(Edition.COMMUNITY);
  }

  @CheckForNull
  public String getFilename(Edition edition) {
    URL value = this.downloadUrl.get(edition);
    return value == null ? null : StringUtils.substringAfterLast(value.getPath(), "/");
  }

  public SortedSet<Version> getRequiredSonarVersions() {
    return compatibleSqVersions;
  }

  public boolean supportSonarVersion(Version providedSqVersion) {
    // Compare versions without qualifier
    for (Version sqVersion : compatibleSqVersions) {
      if (sqVersion.isCompatibleWith(providedSqVersion)) {
        return true;
      }
    }
    return false;
  }

  public Release addRequiredSonarVersions(@Nullable Version... versions) {
    if (versions != null) {
      compatibleSqVersions.addAll(Arrays.asList(versions));
    }
    return this;
  }

  public Release addRequiredSonarVersions(@Nullable String... versions) {
    if (versions != null) {
      for (String v : versions) {
        compatibleSqVersions.add(Version.create(v));
      }
    }
    return this;
  }

  public Version getLastRequiredSonarVersion() {
    if (!compatibleSqVersions.isEmpty()) {
      return compatibleSqVersions.last();
    }
    return null;
  }

  public Version getMinimumRequiredSonarVersion() {
    if (!compatibleSqVersions.isEmpty()) {
      return compatibleSqVersions.first();
    }
    return null;
  }

  public Version[] getSonarVersionFromString(final String fromString) {

    Collection<Version> versionsWGivenFromString = compatibleSqVersions.stream()
      .filter(Objects::nonNull)
      .filter(sqVersion -> fromString.equals(sqVersion.getFromString()))
      .collect(Collectors.toSet());

    return versionsWGivenFromString.toArray(new Version[versionsWGivenFromString.size()]);
  }

  @CheckForNull
  public Date getDate() {
    return date != null ? new Date(date.getTime()) : null;
  }

  public Release setDate(@Nullable Date date) {
    this.date = date != null ? new Date(date.getTime()) : null;
    return this;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public Release setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  @CheckForNull
  public String getChangelogUrl() {
    return this.changelogUrl == null ? null : changelogUrl.toString();
  }

  public Release setChangelogUrl(@Nullable String changelogUrlString) {
    if (changelogUrlString == null) {
      this.changelogUrl = null;
    } else {
      try {
        this.changelogUrl = new URI(changelogUrlString).toURL();
      } catch (URISyntaxException | MalformedURLException ex) {
        throw new IllegalArgumentException("changelogUrl invalid", ex);
      }
    }
    return this;
  }

  public Set<Release> getOutgoingDependencies() {
    return unmodifiableSet(new HashSet<>(outgoingDependencies));
  }

  public Release addOutgoingDependency(Release required) {
    outgoingDependencies.add(required);
    return this;
  }

  public Set<Release> getIncomingDependencies() {
    return unmodifiableSet(new HashSet<>(incomingDependencies));
  }

  public Release addIncomingDependency(Release required) {
    incomingDependencies.add(required);
    return this;
  }

  public String getKey() {
    return getArtifact().getKey();
  }

  public Version getAdjustedVersion() {
    return version.removeQualifier();
  }

  public boolean isPublic() {
    return isPublic;
  }

  public void setPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }

  public boolean isArchived() {
    return isArchived;
  }

  public void setArchived(boolean isArchived) {
    this.isArchived = isArchived;
  }

  @CheckForNull
  public String groupId() {
    return groupId;
  }

  public void setGroupId(@Nullable String groupId) {
    this.groupId = groupId;
  }

  @CheckForNull
  public String artifactId() {
    return artifactId;
  }

  public void setArtifactId(@Nullable String artifactId) {
    this.artifactId = artifactId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Release release = (Release) o;

    return artifact.equals(release.artifact) && version.equals(release.version);
  }

  @Override
  public int hashCode() {
    int result = artifact.hashCode();
    result = 31 * result + version.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("version", version)
      .append("downloadUrl", downloadUrl)
      .append("changelogUrl", changelogUrl)
      .append("description", description)
      .toString();
  }

  @Override
  public int compareTo(Release o) {
    return getVersion().compareTo(o.getVersion());
  }
}
