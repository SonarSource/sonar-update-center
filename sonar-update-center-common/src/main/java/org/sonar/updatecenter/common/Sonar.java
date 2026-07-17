/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) SonarSource Sàrl
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Sonar extends Artifact {

  /**
   * @deprecated since 1.32 in favor of {@link Sonar#ltaVersion}
   */
  @Deprecated(since = "1.32", forRemoval = true)
  private Release ltsVersion;

  private Release ltaVersion;
  private Release pastLtaVersion;

  /**
   * Ordered from oldest to newest. Supersedes {@link #ltaVersion}/{@link #pastLtaVersion} to support more than
   * one concurrently supported LTA line; those two fields are still kept in sync (latest / second-latest of this
   * list) for consumers that haven't migrated yet.
   */
  private final List<Release> ltaVersions = new ArrayList<>();

  public Sonar() {
    super("sonar");
  }

  /**
   * @deprecated since 1.32 in favor of {@link Sonar#setLtaVersion(String)}
   */
  @Deprecated(since = "1.32", forRemoval = true)
  public Sonar setLtsRelease(String ltsVersion) {
    this.ltsVersion = new Release(this, Version.create(ltsVersion));
    return this;
  }

  /**
   * @deprecated since 1.32 in favor of {@link Sonar#getLtaVersion()}
   */
  @Deprecated(since = "1.32", forRemoval = true)
  public Release getLtsRelease() {
    return ltsVersion;
  }

  /**
   * shortcut only for tests, no need to have other fields than version
   */
  public Sonar setReleases(String[] versions) {
    for (String version : versions) {
      addRelease(new Release(this, Version.create(version)));
    }
    return this;
  }

  public Release getLtaVersion() {
    return ltaVersion;
  }

  public void setLtaVersion(String version) {
    this.ltaVersion = new Release(this, Version.create(version));
  }

  public Release getPastLtaVersion() {
    return pastLtaVersion;
  }

  public void setPastLtaVersion(String version) {
    this.pastLtaVersion = new Release(this, Version.create(version));
  }

  public List<Release> getLtaVersions() {
    return List.copyOf(ltaVersions);
  }

  public Sonar setLtaVersions(List<Release> ltaVersions) {
    this.ltaVersions.clear();
    this.ltaVersions.addAll(ltaVersions);
    this.ltaVersions.sort(Comparator.naturalOrder());
    return this;
  }
}
