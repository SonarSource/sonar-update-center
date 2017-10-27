/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2017 SonarSource SA
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
package org.sonar.updatecenter.mojo.editions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.sonar.updatecenter.mojo.SQVersionInMatrix;

public class EditionsMatrix {

  private Map<String, SQVersionInMatrix> sqVersionsByVersion = new HashMap<>();
  private Map<String, EditionInMatrix> editionsByKey = new HashMap<>();

  public Map<String, SQVersionInMatrix> getSqVersionsByVersion() {
    return sqVersionsByVersion;
  }

  public Collection<SQVersionInMatrix> getSqVersions() {
    return sqVersionsByVersion.values();
  }

  public Map<String, EditionInMatrix> getEditionsByKey() {
    return editionsByKey;
  }

  public Collection<EditionInMatrix> getEditions() {
    return editionsByKey.values();
  }

  public static class EditionInMatrix {

    private final String name;
    private final Map<String, Edition> compatibleEditionBySqVersion = new HashMap<>();

    public EditionInMatrix(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public Map<String, Edition> getCompatibleEditionBySqVersion() {
      return compatibleEditionBySqVersion;
    }

    public boolean supports(String sqVersion) {
      return compatibleEditionBySqVersion.containsKey(sqVersion);
    }

    public Edition supportedEdition(String sqVersion) {
      return compatibleEditionBySqVersion.get(sqVersion);
    }

  }

}
