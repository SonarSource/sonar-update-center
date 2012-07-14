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

import java.util.*;

import static org.fest.assertions.Assertions.assertThat;


public class ArtifactTest {

  @Test
  public void compare() {
    Artifact a = new FakeArtifact("a");
    Artifact b = new FakeArtifact("b");
    Artifact c = new Plugin("c");

    List<Artifact> list = Arrays.asList(b, a, c);
    Collections.sort(list);
    assertThat(list.get(0)).isEqualTo(a);
    assertThat(list.get(1)).isEqualTo(b);
    assertThat(list.get(2)).isEqualTo(c);
  }

  @Test
  public void sortReleases() {
    FakeArtifact artifact = new FakeArtifact("fake");
    artifact.addRelease(Version.create("2.0"));
    artifact.addRelease(Version.create("1.1"));
    artifact.addRelease(Version.create("1.5"));

    Iterator<Release> it = artifact.getReleases().iterator();
    assertThat(it.next().getVersion().getName()).isEqualTo("1.1");
    assertThat(it.next().getVersion().getName()).isEqualTo("1.5");
    assertThat(it.next().getVersion().getName()).isEqualTo("2.0");
  }

  @Test
  public void equals() {
    FakeArtifact foo = new FakeArtifact("foo");
    assertThat(foo.equals(new FakeArtifact("foo"))).isTrue();
    assertThat(foo.equals(foo)).isTrue();
    assertThat(foo.equals(new FakeArtifact("bar"))).isFalse();
  }

  @Test
  public void getReleasesGreaterThan() {
    FakeArtifact artifact = new FakeArtifact("fake");
    artifact.addRelease(Version.create("2.0"));
    artifact.addRelease(Version.create("1.1"));
    artifact.addRelease(Version.create("1.5"));

    SortedSet<Release> greaterReleases = artifact.getReleasesGreaterThan("1.2");
    assertThat(greaterReleases).onProperty("version").containsOnly(Version.create("1.5"), Version.create("2.0"));
  }
}

class FakeArtifact extends Artifact {

  protected FakeArtifact(String key) {
    super(key);
  }
}