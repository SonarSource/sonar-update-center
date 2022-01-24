/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2022 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginManifestTest {

  @Test(expected = RuntimeException.class)
  public void test() throws Exception {
    new PluginManifest(new File("fake.jar"));
  }

  @Test
  public void should_create_manifest() throws URISyntaxException, IOException {
    URL jar = getClass().getResource("/org/sonar/updatecenter/common/PluginManifestTest/checkstyle-plugin.jar");

    PluginManifest manifest = new PluginManifest(new File(jar.toURI()));

    assertThat(manifest.getKey()).isEqualTo("checkstyle");
    assertThat(manifest.getName()).isEqualTo("Checkstyle");
    assertThat(manifest.getRequirePlugins()).isEmpty();
    assertThat(manifest.getMainClass()).isEqualTo("org.sonar.plugins.checkstyle.CheckstylePlugin");
    assertThat(manifest.getVersion().length()).isGreaterThan(1);
    assertThat(manifest.isUseChildFirstClassLoader()).isFalse();
    assertThat(manifest.isSonarLintSupported()).isFalse();
    assertThat(manifest.getDependencies()).hasSize(2);
    assertThat(manifest.getDependencies()).containsOnly("META-INF/lib/antlr-2.7.7.jar", "META-INF/lib/checkstyle-5.5.jar");
    assertThat(manifest.getImplementationBuild()).isEqualTo("b9283404030db9ce1529b1fadfb98331686b116d");
  }

  @Test
  public void accessors() throws URISyntaxException, IOException, ParseException {
    URL jar = getClass().getResource("/org/sonar/updatecenter/common/PluginManifestTest/checkstyle-plugin.jar");

    PluginManifest manifest = new PluginManifest(new File(jar.toURI()));

    manifest.setName("newName");
    String[] requirePlugins = new String[2];
    requirePlugins[0] = "requiredPlugin1";
    requirePlugins[1] = "requiredPlugin2";
    manifest.setRequirePlugins(requirePlugins);
    manifest.setSonarVersion("newSonarVersion");
    manifest.setMainClass("newMainClass");
    DateFormat formatter = new SimpleDateFormat("yyyy-mm-dd");
    Date macBirthDate = formatter.parse("1984-01-24");
    manifest.setBuildDate(macBirthDate);
    manifest.setUseChildFirstClassLoader(false);
    manifest.setSonarLintSupported(true);
    manifest.setBasePlugin("newBasePlugin");
    manifest.setImplementationBuild("newImplementationBuild");

    assertThat(manifest.getName()).isEqualTo("newName");
    assertThat(manifest.getRequirePlugins()).hasSize(2);
    assertThat(manifest.getSonarVersion()).isEqualTo("newSonarVersion");
    assertThat(manifest.getMainClass()).isEqualTo("newMainClass");
    assertThat(manifest.getBuildDate().equals(macBirthDate)).isTrue();
    assertThat(manifest.isUseChildFirstClassLoader()).isFalse();
    assertThat(manifest.isSonarLintSupported()).isTrue();
    assertThat(manifest.getBasePlugin()).isEqualTo("newBasePlugin");
    assertThat(manifest.getImplementationBuild()).isEqualTo("newImplementationBuild");
  }

  @Test
  public void do_not_fail_when_no_old_plugin_manifest() throws URISyntaxException, IOException {
    URL jar = getClass().getResource("/org/sonar/updatecenter/common/PluginManifestTest/old-plugin.jar");

    PluginManifest manifest = new PluginManifest(new File(jar.toURI()));

    assertThat(manifest.getKey()).isNull();
    assertThat(manifest.getName()).isNull();
    assertThat(manifest.getRequirePlugins()).isEmpty();
    assertThat(manifest.getMainClass()).isEqualTo("org.sonar.plugins.checkstyle.CheckstylePlugin");
    assertThat(manifest.isUseChildFirstClassLoader()).isFalse();
    assertThat(manifest.getDependencies()).isEmpty();
    assertThat(manifest.getImplementationBuild()).isNull();
    assertThat(manifest.getDevelopers()).isEmpty();
    assertThat(manifest.getSourcesUrl()).isNull();
  }

  @Test
  public void should_add_develpers() throws URISyntaxException, IOException {
    URL jar = getClass().getResource("/org/sonar/updatecenter/common/PluginManifestTest/plugin-with-devs.jar");

    PluginManifest manifest = new PluginManifest(new File(jar.toURI()));

    assertThat(manifest.getDevelopers()).contains("Firstname1 Name1", "Firstname2 Name2");
  }

  @Test
  public void should_add_sources_url() throws URISyntaxException, IOException {
    URL jar = getClass().getResource("/org/sonar/updatecenter/common/PluginManifestTest/plugin-with-sources.jar");

    PluginManifest manifest = new PluginManifest(new File(jar.toURI()));

    assertThat(manifest.getSourcesUrl()).isEqualTo("https://github.com/SonarSource/project");
  }

  @Test
  public void should_add_requires_plugins() throws URISyntaxException, IOException {
    URL jar = getClass().getResource("/org/sonar/updatecenter/common/PluginManifestTest/plugin-with-require-plugins.jar");

    PluginManifest manifest = new PluginManifest(new File(jar.toURI()));

    assertThat(manifest.getRequirePlugins()).hasSize(2);
    assertThat(manifest.getRequirePlugins()[0]).isEqualTo("scm:1.0");
    assertThat(manifest.getRequirePlugins()[1]).isEqualTo("fake:1.1");
  }
}
