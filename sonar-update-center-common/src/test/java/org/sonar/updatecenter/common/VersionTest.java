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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionTest {

  @Test
  public void test_fields_of_snapshot_versions() {
    Version version = Version.create("1.2.3-SNAPSHOT");
    assertThat(version.getMajor()).isEqualTo("1");
    assertThat(version.getMinor()).isEqualTo("2");
    assertThat(version.getPatch()).isEqualTo("3");
    assertThat(version.getPatch2()).isEqualTo("0");
    assertThat(version.getQualifier()).isEqualTo("SNAPSHOT");
  }

  @Test
  public void test_fields_of_releases() {
    Version version = Version.create("1.2");
    assertThat(version.getMajor()).isEqualTo("1");
    assertThat(version.getMinor()).isEqualTo("2");
    assertThat(version.getPatch()).isEqualTo("0");
    assertThat(version.getPatch2()).isEqualTo("0");
    assertThat(version.getQualifier()).isEqualTo("");
  }

  @Test
  public void test_fields_fromString() {

    Version version = Version.create("1.2");
    assertThat(version.getMajor()).isEqualTo("1");
    assertThat(version.getMinor()).isEqualTo("2");
    assertThat(version.getPatch()).isEqualTo("0");
    assertThat(version.getPatch2()).isEqualTo("0");
    assertThat(version.getQualifier()).isEqualTo("");
    assertThat(version.getFromString()).isEqualTo("1.2");

    Version versionWFromString = Version.create("1.2", "LATEST");
    assertThat(versionWFromString.getMajor()).isEqualTo("1");
    assertThat(versionWFromString.getMinor()).isEqualTo("2");
    assertThat(versionWFromString.getPatch()).isEqualTo("0");
    assertThat(versionWFromString.getPatch2()).isEqualTo("0");
    assertThat(versionWFromString.getQualifier()).isEqualTo("");
    assertThat(versionWFromString.getFromString()).isEqualTo("LATEST");

    Version copiedVersion = Version.create(versionWFromString, "COPY");
    copiedVersion.equals(versionWFromString);
    assertThat(copiedVersion.getFromString()).isEqualTo("COPY");
  }

  @Test
  public void compare_releases() {
    Version version12 = Version.create("1.2");
    Version version121 = Version.create("1.2.1");

    assertThat(version12.toString()).isEqualTo("1.2");
    assertThat(version12.compareTo(version12)).isEqualTo(0);
    assertThat(version121.compareTo(version121)).isEqualTo(0);

    assertThat(version121.compareTo(version12) > 0).isTrue();
    assertThat(version12.compareTo(version121) < 0).isTrue();
  }

  @Test
  public void compare_snapshots() {
    Version version12 = Version.create("1.2");
    Version version12Snapshot = Version.create("1.2-SNAPSHOT");
    Version version121Snapshot = Version.create("1.2.1-SNAPSHOT");
    Version version12RC = Version.create("1.2-RC1");

    assertThat(version12.compareTo(version12Snapshot)).isGreaterThan(0);
    assertThat(version12Snapshot.compareTo(version12Snapshot)).isEqualTo(0);
    assertThat(version121Snapshot.compareTo(version12Snapshot)).isGreaterThan(0);
    assertThat(version12Snapshot.compareTo(version12RC)).isGreaterThan(0);
  }

  @Test
  public void compare_release_candidates() {
    Version version12 = Version.create("1.2");
    Version version12Snapshot = Version.create("1.2-SNAPSHOT");
    Version version12RC1 = Version.create("1.2-RC1");
    Version version12RC2 = Version.create("1.2-RC2");

    assertThat(version12RC1.compareTo(version12Snapshot)).isLessThan(0);
    assertThat(version12RC1.compareTo(version12RC1)).isEqualTo(0);
    assertThat(version12RC1.compareTo(version12RC2)).isLessThan(0);
    assertThat(version12RC1.compareTo(version12)).isLessThan(0);

  }

  @Test
  public void testTrim() {
    Version version12 = Version.create("   1.2  ");

    assertThat(version12.getName()).isEqualTo("1.2");
    assertThat(version12.equals(Version.create("1.2"))).isTrue();
  }

  @Test
  public void testDefaultNumberIsZero() {
    Version version12 = Version.create("1.2");
    Version version120 = Version.create("1.2.0");

    assertThat(version12.equals(version120)).isTrue();
    assertThat(version120.equals(version12)).isTrue();
  }

  @Test
  public void testCompareOnTwoDigits() {
    Version version1dot10 = Version.create("1.10");
    Version version1dot1 = Version.create("1.1");
    Version version1dot9 = Version.create("1.9");

    assertThat(version1dot10.compareTo(version1dot1) > 0).isTrue();
    assertThat(version1dot10.compareTo(version1dot9) > 0).isTrue();
  }

  @Test
  public void testFields() {
    Version version = Version.create("1.10.2");

    assertThat(version.getName()).isEqualTo("1.10.2");
    assertThat(version.toString()).isEqualTo("1.10.2");
    assertThat(version.getMajor()).isEqualTo("1");
    assertThat(version.getMinor()).isEqualTo("10");
    assertThat(version.getPatch()).isEqualTo("2");
    assertThat(version.getPatch2()).isEqualTo("0");
  }

  @Test
  public void testPatchFields() {
    Version version = Version.create("1.2.3.4");

    assertThat(version.getPatch()).isEqualTo("3");
    assertThat(version.getPatch2()).isEqualTo("4");

    assertThat(version.equals(version)).isTrue();
    assertThat(version.equals(Version.create("1.2.3.4"))).isTrue();
    assertThat(version.equals(Version.create("1.2.3.5"))).isFalse();
  }

  @Test
  public void isPatchVersion_whenIsPatchVersion_returnTrue() {
    Version version = Version.create("1.2.3.4");

    boolean patchVersion = version.isPatchVersion();

    assertThat(patchVersion).isTrue();
  }

  @Test
  public void isPatchVersion_whenIsNotPatchVersion_returnFalse() {
    Version version = Version.create("1.2.0.0");

    boolean patchVersion = version.isPatchVersion();

    assertThat(patchVersion).isFalse();
  }

  @Test
  public void removeQualifier() {
    Version version = Version.create("1.2.3-SNAPSHOT").removeQualifier();

    assertThat(version.getMajor()).isEqualTo("1");
    assertThat(version.getMinor()).isEqualTo("2");
    assertThat(version.getPatch()).isEqualTo("3");
    assertThat(version.getQualifier()).isEqualTo("");
  }
}
