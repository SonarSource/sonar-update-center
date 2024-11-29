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

public class PluginKeyUtilsTest {

  @Test
  public void shouldSanitizeMavenArtifactId() {
    assertThat(PluginKeyUtils.sanitize("sonar-test-plugin")).isEqualTo("test");
    assertThat(PluginKeyUtils.sanitize("test-sonar-plugin")).isEqualTo("test");
    assertThat(PluginKeyUtils.sanitize("test")).isEqualTo("test");

    assertThat(PluginKeyUtils.sanitize("sonar-test-foo-plugin")).isEqualTo("testfoo");
    assertThat(PluginKeyUtils.sanitize("test-foo-sonar-plugin")).isEqualTo("testfoo");
    assertThat(PluginKeyUtils.sanitize("test-foo")).isEqualTo("testfoo");
    assertThat(PluginKeyUtils.sanitize("keep.only-digits%12345&and*letters")).isEqualTo("keeponlydigits12345andletters");
    assertThat(PluginKeyUtils.sanitize("   remove whitespaces   ")).isEqualTo("removewhitespaces");
  }

  @Test
  public void shouldBeValid() {
    assertThat(PluginKeyUtils.isValid("foo")).isEqualTo(true);
    assertThat(PluginKeyUtils.isValid("sonarfooplugin")).isEqualTo(true);
    assertThat(PluginKeyUtils.isValid("foo6")).isEqualTo(true);
    assertThat(PluginKeyUtils.isValid("FOO6")).isEqualTo(true);
  }

  @Test
  public void shouldNotBeValid() {
    assertThat(PluginKeyUtils.isValid(null)).isEqualTo(false);
    assertThat(PluginKeyUtils.isValid("")).isEqualTo(false);
    assertThat(PluginKeyUtils.isValid("sonar-foo-plugin")).isEqualTo(false);
    assertThat(PluginKeyUtils.isValid("foo.bar")).isEqualTo(false);
    assertThat(PluginKeyUtils.isValid("  nowhitespaces   ")).isEqualTo(false);
    assertThat(PluginKeyUtils.isValid("no whitespaces")).isEqualTo(false);
  }
}
