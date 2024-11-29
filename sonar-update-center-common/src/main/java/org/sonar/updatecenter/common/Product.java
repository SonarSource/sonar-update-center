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

public enum Product {

  SONARQUBE_COMMUNITY_BUILD("sqcb"),
  SONARQUBE_SERVER("sqs"),
  //10.7 and before
  OLD_SONARQUBE("sqVersions");

  private final String suffix;

  Product(String suffix) {
    this.suffix = suffix;
  }

  public String getSuffix() {
    return suffix;
  }
}
