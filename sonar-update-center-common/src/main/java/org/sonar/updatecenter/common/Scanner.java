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

import org.apache.commons.lang.StringUtils;

public class Scanner extends Component {

  private Scanner(String key) {
    super(key);
  }

  public static Scanner factory(String key) {
    // in accordance with https://github.com/SonarSource/sonar-packaging-maven-plugin/blob/master/src/main/java/org/sonarsource/pluginpackaging/PluginKeyUtils.java#L44
    if (StringUtils.isAlphanumeric(key)) {
      return new Scanner(key);
    } else {
      throw new IllegalArgumentException("scanner key must be alphanumeric, strictly");
    }
  }

  @Override
  boolean needArtifact() {
    return false;
  }

  @Override
  boolean needSqVersion() {
    return false;
  }

}
