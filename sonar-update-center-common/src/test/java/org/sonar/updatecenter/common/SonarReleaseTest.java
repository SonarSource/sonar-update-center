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

public class SonarReleaseTest {

  @Test
  public void constructor_productIsSet() {
    SonarRelease sonarRelease = new SonarRelease(new Sonar(), Version.create("2025.1"), Product.SONARQUBE_SERVER);

    assertThat(sonarRelease.getProduct()).isEqualTo(Product.SONARQUBE_SERVER);
  }
}
