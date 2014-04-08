/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class SonarUpdateTest {

  @Test
  public void incompatibleUpdateIfSomePluginsAreIncompatible() {
    SonarUpdate update = new SonarUpdate(new Release(new Sonar(), "2.3"));
    update.addIncompatiblePlugin(new Plugin("old"));

    assertThat(update.isIncompatible()).isTrue();
    assertThat(update.hasWarnings()).isTrue();
    assertThat(update.requiresPluginUpgrades()).isFalse();
  }

  @Test
  public void incompatibleUpdateIfRequiredPluginUpgrades() {
    SonarUpdate update = new SonarUpdate(new Release(new Sonar(), "2.3"));
    update.addPluginToUpgrade(new Release(new Plugin("old"), Version.create("0.2")));

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

  @Test
  public void testUpdateJava() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/SonarUpdateTest/update-center.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = UpdateCenterDeserializer.fromProperties(props);

      center.setInstalledSonarVersion(Version.create("3.7.4"));
      List<Plugin> plugins = new ArrayList<Plugin>();
      Plugin java = new Plugin("java");
      java.addRelease("2.0");
      plugins.add(java);
      center.registerInstalledPlugins(PluginReferential.create(plugins));

      List<SonarUpdate> sonarUpdates = center.findSonarUpdates();
      SonarUpdate sonar4_2 = sonarUpdates.get(4);
      assertThat(sonar4_2.getRelease().getVersion().toString()).isEqualTo("4.2");
      assertThat(sonar4_2.getPluginsToUpgrade().size()).isEqualTo(1);
      assertThat(sonar4_2.getPluginsToUpgrade().get(0).getVersion().toString()).isEqualTo("2.1");
    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
