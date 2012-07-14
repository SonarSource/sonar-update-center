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

import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.hamcrest.number.OrderingComparisons.lessThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class VersionTest {

  @Test
  public void test_fields_of_snapshot_versions() {
    Version version = Version.create("1.2.3-SNAPSHOT");
    assertThat(version.getMajor(), is("1"));
    assertThat(version.getMinor(), is("2"));
    assertThat(version.getPatch(), is("3"));
    assertThat(version.getPatch2(), is("0"));
    assertThat(version.getQualifier(), is("SNAPSHOT"));
  }

  @Test
  public void test_fields_of_releases() {
    Version version = Version.create("1.2");
    assertThat(version.getMajor(), is("1"));
    assertThat(version.getMinor(), is("2"));
    assertThat(version.getPatch(), is("0"));
    assertThat(version.getPatch2(), is("0"));
    assertThat(version.getQualifier(), is(""));
  }

  @Test
  public void compare_releases() {
    Version version12 = Version.create("1.2");
    Version version121 = Version.create("1.2.1");

    assertThat(version12.toString(), is("1.2"));
    assertThat(version12.compareTo(version12), is(0));
    assertThat(version121.compareTo(version121), is(0));

    assertTrue(version121.compareTo(version12) > 0);
    assertTrue(version12.compareTo(version121) < 0);
  }

  @Test
  public void compare_snapshots() {
    Version version12 = Version.create("1.2");
    Version version12Snapshot = Version.create("1.2-SNAPSHOT");
    Version version121Snapshot = Version.create("1.2.1-SNAPSHOT");
    Version version12RC = Version.create("1.2-RC1");

    assertThat(version12.compareTo(version12Snapshot), greaterThan(0));
    assertThat(version12Snapshot.compareTo(version12Snapshot), is(0));
    assertThat(version121Snapshot.compareTo(version12Snapshot), greaterThan(0));
    assertThat(version12Snapshot.compareTo(version12RC), greaterThan(0));
  }

  @Test
  public void compare_release_candidates() {
    Version version12 = Version.create("1.2");
    Version version12Snapshot = Version.create("1.2-SNAPSHOT");
    Version version12RC1 = Version.create("1.2-RC1");
    Version version12RC2 = Version.create("1.2-RC2");

    assertThat(version12RC1.compareTo(version12Snapshot), lessThan(0));
    assertThat(version12RC1.compareTo(version12RC1), is(0));
    assertThat(version12RC1.compareTo(version12RC2), lessThan(0));
    assertThat(version12RC1.compareTo(version12), lessThan(0));

  }

  @Test
  public void testTrim() {
    Version version12 = Version.create("   1.2  ");

    assertThat(version12.getName(), is("1.2"));
    assertTrue(version12.equals(Version.create("1.2")));
  }

  @Test
  public void testDefaultNumberIsZero() {
    Version version12 = Version.create("1.2");
    Version version120 = Version.create("1.2.0");

    assertTrue(version12.equals(version120));
    assertTrue(version120.equals(version12));
  }

  @Test
  public void testCompareOnTwoDigits() {
    Version version1dot10 = Version.create("1.10");
    Version version1dot1 = Version.create("1.1");
    Version version1dot9 = Version.create("1.9");

    assertTrue(version1dot10.compareTo(version1dot1) > 0);
    assertTrue(version1dot10.compareTo(version1dot9) > 0);
  }

  @Test
  public void testFields() {
    Version version = Version.create("1.10.2");

    assertThat(version.getName(), is("1.10.2"));
    assertThat(version.toString(), is("1.10.2"));
    assertThat(version.getMajor(), is("1"));
    assertThat(version.getMinor(), is("10"));
    assertThat(version.getPatch(), is("2"));
    assertThat(version.getPatch2(), is("0"));
  }

  @Test
  public void testPatchFields() {
    Version version = Version.create("1.2.3.4");

    assertThat(version.getPatch(), is("3"));
    assertThat(version.getPatch2(), is("4"));

    assertTrue(version.equals(version));
    assertTrue(version.equals(Version.create("1.2.3.4")));
    assertFalse(version.equals(Version.create("1.2.3.5")));
  }

  @Test
  public void createRelease() {
    Version version = Version.createRelease("1.2.3-SNAPSHOT");

    assertThat(version.getMajor(), is("1"));
    assertThat(version.getMinor(), is("2"));
    assertThat(version.getPatch(), is("3"));
    assertThat(version.getQualifier(), is(""));
  }
}
