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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ArtifactTest {

  @Test
  public void compare() {
    Artifact a = new FakeArtifact("a");
    Artifact b = new FakeArtifact("b");
    Artifact c = Plugin.factory("c");

    List<Artifact> list = Arrays.asList(b, a, c);
    Collections.sort(list);
    assertThat(list).containsExactly(a, b, c);
  }

  @Test
  public void getReleases_orders_releases_by_version() {
    FakeArtifact artifact = new FakeArtifact("fake");
    artifact.addRelease(Version.create("2.0"));
    artifact.addRelease(Version.create("1.1"));
    artifact.addRelease(Version.create("1.5"));

    assertThat(artifact.getReleases())
      .extracting(Release::getVersion)
      .extracting(Version::getName)
      .containsExactly("1.1", "1.5", "2.0");
  }

  @Test
  public void test_equals() {
    FakeArtifact foo = new FakeArtifact("foo");
    assertThat(foo.equals(new FakeArtifact("foo"))).isTrue();
    assertThat(foo.equals(foo)).isTrue();
    assertThat(foo.equals(new FakeArtifact("bar"))).isFalse();
  }

  @Test
  public void test_getReleasesGreaterThan() {
    FakeArtifact artifact = new FakeArtifact("fake");
    artifact.addRelease(Version.create("2.0"));
    artifact.addRelease(Version.create("1.1"));
    artifact.addRelease(Version.create("1.5"));

    assertThat(artifact.getReleasesGreaterThan("1.2", null))
      .extracting(Release::getVersion)
      .extracting(Version::getName)
      .containsExactly("1.5", "2.0");
  }

  @Test
  public void getMinimalRelease_returns_the_first_version_greater_than_specified_version() {
    FakeArtifact artifact = new FakeArtifact("fake");
    artifact.addRelease(Version.create("2.0"));
    artifact.addRelease(Version.create("1.1"));
    artifact.addRelease(Version.create("1.5"));

    assertThat(artifact.getMinimalRelease(Version.create("1.2")).getVersion().getName()).isEqualTo("1.5");
  }

  @Test
  public void getLastRelease_returns_the_highest_public_release() {
    FakeArtifact artifact = new FakeArtifact("fake");
    artifact.addRelease(Version.create("1.1"));
    artifact.addRelease(Version.create("2.0"));
    artifact.addRelease(Version.create("1.5"));
    artifact.setDevRelease(new Release(artifact, "2.1-SNAPSHOT"));

    assertThat(artifact.getLastRelease().getVersion()).isEqualTo(Version.create("2.0"));
  }

  @Test
  public void getAllReleases_includes_dev_version() {
    FakeArtifact artifact = new FakeArtifact("fake");
    artifact.addRelease(Version.create("1.1"));
    artifact.addRelease(Version.create("2.0"));
    artifact.addRelease(Version.create("1.5"));
    artifact.setDevRelease(new Release(artifact, "2.1-SNAPSHOT"));

    assertThat(artifact.getAllReleases())
      .extracting(Release::getVersion)
      .extracting(Version::getName)
      .containsExactly("1.1", "1.5", "2.0", "2.1-SNAPSHOT");
  }

  @Test
  public void getMajorReleases() {
    FakeArtifact artifact = new FakeArtifact("fake");
    artifact.addRelease(Version.create("1.1"));
    artifact.addRelease(Version.create("1.1.2"));
    artifact.addRelease(Version.create("2.0"));
    artifact.addRelease(Version.create("2.0.1"));
    artifact.addRelease(Version.create("2.0.2"));

    assertThat(artifact.getMajorReleases())
      .extracting(Release::getVersion)
      .extracting(Version::getName)
      .containsExactly("1.1.2", "2.0.2");
  }

  private static class FakeArtifact extends Artifact {

    private FakeArtifact(String key) {
      super(key);
    }

    Release addRelease(Version version) {
      Release release = new Release(this, version);
      releases.add(release);
      return release;
    }
  }

}
