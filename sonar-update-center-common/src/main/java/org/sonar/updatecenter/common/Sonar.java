/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.updatecenter.common;

public class Sonar extends Artifact {

  /**
   * @deprecated since 1.32 in favor of {@link Sonar#ltaVersion}
   */
  @Deprecated
  private Release ltsVersion;

  private Release ltaVersion;
  private Release pastLtaVersion;

  public Sonar() {
    super("sonar");
  }

  /**
   * @deprecated since 1.32 in favor of {@link Sonar#setLtaVersion(String)}
   */
  @Deprecated
  public Sonar setLtsRelease(String ltsVersion) {
    this.ltsVersion = new Release(this, Version.create(ltsVersion));
    return this;
  }

  /**
   * @deprecated since 1.32 in favor of {@link Sonar#getLtaVersion()}
   */
  @Deprecated
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
}
