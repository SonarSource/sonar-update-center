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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginReferential;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.UpdateCenter;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EditionsGeneratorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File jarsDir;
  private File outputDir;
  private EditionTemplatesLoader templateLoader = mock(EditionTemplatesLoader.class);
  private Plugin cobolPlugin = newPlugin("cobol");
  private Plugin governancePlugin = newPlugin("governance");
  private PluginReferential pluginReferential;

  @Before
  public void setUp() throws Exception {
    jarsDir = temp.newFolder();
    outputDir = temp.newFolder();
    makePluginVersionCompatibleWith(cobolPlugin, "1.0", "5.6", "6.7");
    makePluginVersionCompatibleWith(cobolPlugin, "1.1", "6.7", "7.0");
    makePluginVersionCompatibleWith(governancePlugin, "1.0", "6.7");
    makePluginVersionCompatibleWith(governancePlugin, "2.0", "7.0", "7.1");
    pluginReferential = PluginReferential.create(asList(cobolPlugin, governancePlugin));
  }

  @Test
  public void generateZipsAndJson_generates_files_in_output_dir() throws IOException {
    Sonar sonarqube = new Sonar().setReleases(new String[] {"5.6", "6.7", "7.0"});

    EditionTemplate template = newEnterpriseTemplate()
      .setPluginKeys(asList("cobol", "governance"))
      .build();
    when(templateLoader.load()).thenReturn(asList(template));

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonarqube);

    EditionsGenerator underTest = new EditionsGenerator(updateCenter, templateLoader, jarsDir);
    underTest.generateZipsAndJson(outputDir, "http://bintray");

    assertThat(new File(outputDir, "editions.json")).isFile().exists();
    assertThat(new File(outputDir, "enterprise-6.7.zip")).isFile().exists();
    assertThat(new File(outputDir, "enterprise-7.0.zip")).isFile().exists();
  }

  @Test
  public void generate_zip_files_of_editions() throws Exception {
    Sonar sonarqube = new Sonar().setReleases(new String[] {"5.6", "6.7", "7.0"});

    EditionTemplate template = newEnterpriseTemplate()
      .setPluginKeys(asList("cobol", "governance"))
      .build();
    when(templateLoader.load()).thenReturn(asList(template));

    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonarqube);

    EditionsGenerator underTest = new EditionsGenerator(updateCenter, templateLoader, jarsDir);

    // one enterprise edition per SQ version >= 6.5
    // The latest compatible release of each plugin is packaged in the zip.
    List<Edition> editions = underTest.generateZips(outputDir);
    assertThat(editions).hasSize(2);

    Edition edition67 = editions.get(0);
    assertThat(edition67.getSonarQubeVersion()).isEqualTo("6.7");
    assertThatEditionMatchesTemplate(edition67, template);
    assertThat(edition67.getZip())
      .exists()
      .isFile()
      .hasName("enterprise-6.7.zip")
      .hasParent(outputDir);
    assertThatZipContainsExactly(edition67.getZip(), "cobol-1.1.jar", "governance-1.0.jar");

    Edition edition70 = editions.get(1);
    assertThatEditionMatchesTemplate(edition70, template);
    assertThat(edition70.getSonarQubeVersion()).isEqualTo("7.0");
    assertThat(edition70.getZip())
      .exists()
      .isFile()
      .hasName("enterprise-7.0.zip")
      .hasParent(outputDir);

    assertThatZipContainsExactly(edition70.getZip(), "cobol-1.1.jar", "governance-2.0.jar");
  }

  @Test
  public void fail_if_template_declares_a_plugin_that_does_not_exist() throws Exception {
    Sonar sonarqube = new Sonar().setReleases(new String[] {"5.6", "6.7", "7.0"});
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonarqube);

    EditionTemplate template = newEnterpriseTemplate()
      .setPluginKeys(asList("cobol", "wat"))
      .build();
    when(templateLoader.load()).thenReturn(singletonList(template));

    expectedException.expect(NoSuchElementException.class);
    expectedException.expectMessage("Unable to find plugin with key wat");

    EditionsGenerator underTest = new EditionsGenerator(updateCenter, templateLoader, jarsDir);
    underTest.generateZips(outputDir);
  }

  @Test
  public void plugin_is_not_packaged_if_no_compatible_versions() throws Exception {
    Sonar sonarqube = new Sonar().setReleases(new String[] {"99.2"});
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonarqube);

    EditionTemplate template = newEnterpriseTemplate()
      .setPluginKeys(asList("cobol"))
      .build();
    when(templateLoader.load()).thenReturn(asList(template));

    EditionsGenerator underTest = new EditionsGenerator(updateCenter, templateLoader, jarsDir);
    Edition edition = underTest.generateZips(outputDir).get(0);
    assertThatZipIsEmpty(edition.getZip());
  }

  @Test
  public void fail_if_jars_dir_does_not_exist() throws Exception {
    FileUtils.deleteDirectory(jarsDir);
    Sonar sonarqube = new Sonar().setReleases(new String[] {"99.2"});
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, sonarqube);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Directory does not exist: " + jarsDir);

    new EditionsGenerator(updateCenter, templateLoader, jarsDir);
  }

  private EditionTemplate.Builder newEnterpriseTemplate() {
    return new EditionTemplate.Builder()
      .setKey("enterprise")
      .setName("Enterprise")
      .setTextDescription("Enterprise Edition")
      .setHomeUrl("/home")
      .setRequestLicenseUrl("/request");
  }

  private void assertThatEditionMatchesTemplate(Edition edition, EditionTemplate template) {
    assertThat(edition.getKey()).isEqualTo(template.getKey());
    assertThat(edition.getName()).isEqualTo(template.getName());
    assertThat(edition.getTextDescription()).isEqualTo(template.getTextDescription());
    assertThat(edition.getHomeUrl()).isEqualTo(template.getHomeUrl());
    assertThat(edition.getRequestUrl()).isEqualTo(template.getRequestUrl());
  }

  private void assertThatZipIsEmpty(File zip) throws Exception {
    assertThatZipContainsExactly(zip);
  }

  private void assertThatZipContainsExactly(File zip, String... filenames) throws Exception {
    try (ZipInputStream zipInput = new ZipInputStream(new FileInputStream(zip))) {
      for (String filename : filenames) {
        assertThat(zipInput.getNextEntry().getName()).isEqualTo(filename);
      }
      assertThat(zipInput.getNextEntry()).isNull();
    }
  }

  private void makePluginVersionCompatibleWith(Plugin plugin, String pluginVersion, String... sqVersions) throws IOException {
    Release release = new Release(plugin, pluginVersion);
    release.addRequiredSonarVersions(sqVersions);
    String filename = plugin.getKey() + "-" + pluginVersion + ".jar";
    release.setDownloadUrl("http://server/" + filename);
    plugin.addRelease(release);
    FileUtils.write(new File(jarsDir, filename), "content of " + filename);
  }

  private static Plugin newPlugin(String key) {
    return Plugin.factory(key);
  }

}
