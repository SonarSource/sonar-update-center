/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2024 SonarSource SA
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
