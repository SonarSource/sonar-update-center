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

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import org.sonar.updatecenter.common.exception.VersionParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

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
  public void test_parsing_exceptions() {
    for (String version : new String[] {"1.*.1", "1.*.1.*-SNAPSHOT", "1.*.1*", "1*", "*.*"}) {
      try {
        Version.create(version);
        fail(String.format("Should throw an exception on version '%s'.", version));
      } catch(VersionParseException e) {
        // noop
      }
    }

    // Should ignore any patterns after the '-'.
    Version.create("1.0-BETA.*.1");

    // Special '*' version is accepted.
    Version.create("*");
  }

  @Test
  public void compare_releases() {
    Version version12 = Version.create("1.2");
    Version version13 = Version.create("1.3");
    Version version121 = Version.create("1.2.1");
    Version version12Star = Version.create("1.2.*");
    Version version121Star = Version.create("1.2.1.*");
    Version version12StarStar = Version.create("1.2.*.*");
    Version version2 = Version.create("2.0");
    Version version2Star = Version.create("2.*");
    Version version2StarStarStar = Version.create("2.*.*.*");

    assertThat(version12.toString()).isEqualTo("1.2");

    // Should be compatible with itself.
    assertThat(version12.compareTo(version12)).isEqualTo(0);
    assertThat(version12.isCompatibleWith(version12)).isTrue();
    assertThat(version121.compareTo(version121)).isEqualTo(0);
    assertThat(version12Star.compareTo(version12Star)).isEqualTo(0);
    assertThat(version121Star.compareTo(version121Star)).isEqualTo(0);
    assertThat(version12StarStar.compareTo(version12StarStar)).isEqualTo(0);
    assertThat(version2Star.compareTo(version2Star)).isEqualTo(0);
    assertThat(version2StarStarStar.compareTo(version2StarStarStar)).isEqualTo(0);

    // Should not be compatible with more recent versions.
    assertThat(version12.compareTo(version13)).isLessThan(0);
    assertThat(version12.isCompatibleWith(version13)).isFalse();
    assertThat(version12.compareTo(version121)).isLessThan(0);
    assertThat(version12.isCompatibleWith(version121)).isFalse();
    assertThat(version12.compareTo(version2Star)).isLessThan(0);
    assertThat(version12.isCompatibleWith(version2Star)).isFalse();
    assertThat(version12.compareTo(version2StarStarStar)).isLessThan(0);
    assertThat(version12.isCompatibleWith(version2StarStarStar)).isFalse();

    // Should not be compatible with less recent versions.
    assertThat(version121.compareTo(version12)).isGreaterThan(0);
    assertThat(version121.isCompatibleWith(version12)).isFalse();
    assertThat(version13.compareTo(version121)).isGreaterThan(0);
    assertThat(version13.isCompatibleWith(version121)).isFalse();
    assertThat(version2StarStarStar.compareTo(version12)).isGreaterThan(0);
    assertThat(version2StarStarStar.isCompatibleWith(version12)).isFalse();

    // Should be compatible with wildcard versions of the same major release.
    assertThat(version2Star.compareTo(version2)).isEqualTo(0);
    assertThat(version2Star.isCompatibleWith(version2)).isTrue();
    assertThat(version2StarStarStar.compareTo(version2)).isEqualTo(0);
    assertThat(version2StarStarStar.isCompatibleWith(version2)).isTrue();
    assertThat(version2.compareTo(version2Star)).isEqualTo(0);
    assertThat(version2.isCompatibleWith(version2Star)).isTrue();
    assertThat(version2.compareTo(version2StarStarStar)).isEqualTo(0);
    assertThat(version2.isCompatibleWith(version2StarStarStar)).isTrue();

    // Should be compatible with wildcard versions of the same minor(.patch) release.
    assertThat(version12Star.compareTo(version12)).isEqualTo(0);
    assertThat(version12Star.isCompatibleWith(version12)).isTrue();
    assertThat(version12Star.compareTo(version121)).isEqualTo(0);
    assertThat(version12Star.isCompatibleWith(version121)).isTrue();
    assertThat(version12Star.compareTo(version121Star)).isEqualTo(0);
    assertThat(version12Star.isCompatibleWith(version121Star)).isTrue();
    assertThat(version12.compareTo(version12Star)).isEqualTo(0);
    assertThat(version12.isCompatibleWith(version12Star)).isTrue();
    assertThat(version12.compareTo(version12StarStar)).isEqualTo(0);
    assertThat(version12.isCompatibleWith(version12StarStar)).isTrue();

    // Should not be compatible if wildcard version is for a more recent major release.
    assertThat(version2Star.compareTo(version12)).isGreaterThan(0);
    assertThat(version2Star.isCompatibleWith(version12)).isFalse();
    assertThat(version2Star.compareTo(version12Star)).isGreaterThan(0);
    assertThat(version2Star.isCompatibleWith(version12Star)).isFalse();
    assertThat(version12.compareTo(version2Star)).isLessThan(0);
    assertThat(version12.isCompatibleWith(version2Star)).isFalse();
    assertThat(version12Star.compareTo(version2Star)).isLessThan(0);
    assertThat(version12Star.isCompatibleWith(version2Star)).isFalse();
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
  public void removeQualifier() {
    Version version = Version.create("1.2.3-SNAPSHOT").removeQualifier();

    assertThat(version.getMajor()).isEqualTo("1");
    assertThat(version.getMinor()).isEqualTo("2");
    assertThat(version.getPatch()).isEqualTo("3");
    assertThat(version.getQualifier()).isEqualTo("");
  }
}
