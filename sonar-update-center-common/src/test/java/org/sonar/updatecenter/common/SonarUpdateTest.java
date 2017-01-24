/*
 * SonarSource :: Update Center :: Common
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
package org.sonar.updatecenter.common;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class SonarUpdateTest {

  @Test
  public void incompatibleUpdateIfSomePluginsAreIncompatible() {
    SonarUpdate update = new SonarUpdate(new Release(new Sonar(), "2.3"));
    update.addIncompatiblePlugin(Plugin.factory("old"));

    assertThat(update.isIncompatible()).isTrue();
    assertThat(update.hasWarnings()).isTrue();
    assertThat(update.requiresPluginUpgrades()).isFalse();
  }

  @Test
  public void incompatibleUpdateIfRequiredPluginUpgrades() {
    SonarUpdate update = new SonarUpdate(new Release(new Sonar(), "2.3"));
    update.addPluginToUpgrade(new Release(Plugin.factory("old"), Version.create("0.2")));

    assertThat(update.isIncompatible()).isFalse();
    assertThat(update.hasWarnings()).isTrue();
    assertThat(update.requiresPluginUpgrades()).isTrue();
  }

  @Test
  public void equals() {
    SonarUpdate update1 = new SonarUpdate(new Release(new Sonar(), "2.2"));
    SonarUpdate update2 = new SonarUpdate(new Release(new Sonar(), "2.3"));

    assertThat(update1).isEqualTo(update1);
    assertThat(update1).isEqualTo(new SonarUpdate(new Release(new Sonar(), "2.2")));
    assertThat(update1).isNotEqualTo(update2);
  }

}
