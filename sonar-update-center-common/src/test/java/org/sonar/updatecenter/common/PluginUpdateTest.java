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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PluginUpdateTest {

  @Test
  public void createForCompatibleRelease() {
    Version sonarVersion = Version.create("2.2");
    Release pluginRelease = new Release(Plugin.factory("fake"), "1.0");
    pluginRelease.addRequiredSonarVersions(Product.OLD_SONARQUBE, Version.create("2.1"), Version.create("2.2"), Version.create("2.3"));

    PluginUpdate update = PluginUpdate.createForPluginRelease(pluginRelease, sonarVersion, Product.OLD_SONARQUBE);

    assertThat(update.getRelease(), is(pluginRelease));
    assertThat(update.isCompatible(), is(true));
    assertThat(update.isIncompatible(), is(false));
    assertThat(update.requiresSonarUpgrade(), is(false));
  }

  @Test
  public void createForSonarUpgrade() {
    Version sonarVersion = Version.create("2.0");
    Release pluginRelease = new Release(Plugin.factory("fake"), "1.0");
    pluginRelease.addRequiredSonarVersions(Product.OLD_SONARQUBE, Version.create("2.1"), Version.create("2.2"), Version.create("2.3"));

    PluginUpdate update = PluginUpdate.createForPluginRelease(pluginRelease, sonarVersion, Product.OLD_SONARQUBE);

    assertThat(update.getRelease(), is(pluginRelease));
    assertThat(update.isCompatible(), is(false));
    assertThat(update.isIncompatible(), is(false));
    assertThat(update.requiresSonarUpgrade(), is(true));
  }

  @Test
  public void createForIncompatibleReleae() {
    Version sonarVersion = Version.create("2.4");
    Release pluginRelease = new Release(Plugin.factory("fake"), "1.0");
    pluginRelease.addRequiredSonarVersions(Product.OLD_SONARQUBE, Version.create("2.1"), Version.create("2.2"), Version.create("2.3"));

    // the plugin is only compatible with older versions of sonar
    PluginUpdate update = PluginUpdate.createForPluginRelease(pluginRelease, sonarVersion, Product.OLD_SONARQUBE);

    assertThat(update.getRelease(), is(pluginRelease));
    assertThat(update.isCompatible(), is(false));
    assertThat(update.isIncompatible(), is(true));
    assertThat(update.requiresSonarUpgrade(), is(false));
  }
}
