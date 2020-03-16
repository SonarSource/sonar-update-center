/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2019 SonarSource SA
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
import java.util.ArrayList;
import java.util.NoSuchElementException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginReferential;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.mojo.editions.generators.EditionGenerator;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class EditionsGeneratorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Captor
  private ArgumentCaptor<ArrayList<Edition>> editionListCapture;

  private File outputDir;
  private EditionTemplatesLoader templateLoader = mock(EditionTemplatesLoader.class);
  private Plugin cobolPlugin = newPlugin("cobol");
  private Plugin governancePlugin = newPlugin("governance");
  private PluginReferential pluginReferential;
  private EditionGenerator generator = mock(EditionGenerator.class);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    outputDir = temp.newFolder();
    makePluginVersionCompatibleWith(cobolPlugin, "1.0", "5.6", "6.7");
    makePluginVersionCompatibleWith(cobolPlugin, "1.1", "6.7", "7.0");
    makePluginVersionCompatibleWith(governancePlugin, "1.0", "6.7");
    makePluginVersionCompatibleWith(governancePlugin, "2.0", "7.0", "7.1");
    pluginReferential = PluginReferential.create(asList(cobolPlugin, governancePlugin));
  }

  @Test
  public void generateZipsJsonHtml_calls_generators() throws Exception {
    Sonar sonarqube = new Sonar().setReleases(new String[] {"5.6", "6.7", "7.0"}).setLtsRelease("6.7");
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonarqube);

    EditionTemplate template = newEnterpriseTemplate()
      .setPluginKeys(asList("cobol", "governance"))
      .build();
    when(templateLoader.load()).thenReturn(asList(template));

    EditionsGenerator underTest = new EditionsGenerator(updateCenter, templateLoader, "1234");
    underTest.generateZipsJsonHtml(outputDir, generator);

    verify(generator).generate(eq(outputDir), editionListCapture.capture());
    assertThat(editionListCapture.getValue()).hasSize(2);

    Edition edition67 = editionListCapture.getValue().get(0);
    Edition edition70 = editionListCapture.getValue().get(1);

    assertThat(edition67.getSonarQubeVersion()).isEqualTo("6.7");
    assertThat(edition67.jars()).containsOnly("cobol-1.1.jar", "governance-1.0.jar");
    assertThatEditionMatchesTemplate(edition67, template);

    assertThat(edition70.getSonarQubeVersion()).isEqualTo("7.0");
    assertThat(edition70.jars()).containsOnly("cobol-1.1.jar", "governance-2.0.jar");
    assertThatEditionMatchesTemplate(edition70, template);
  }

  @Test
  public void fail_if_template_declares_a_plugin_that_does_not_exist() throws Exception {
    Sonar sonarqube = new Sonar().setReleases(new String[] {"5.6", "6.7", "7.0"});
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonarqube);

    EditionTemplate template = newEnterpriseTemplate()
      .setPluginKeys(asList("cobol", "wat"))
      .build();
    when(templateLoader.load()).thenReturn(singletonList(template));

    expectedException.expect(NoSuchElementException.class);
    expectedException.expectMessage("Unable to find plugin with key wat");

    EditionsGenerator underTest = new EditionsGenerator(updateCenter, templateLoader, "1234");
    underTest.generateZipsJsonHtml(outputDir, generator);
  }

  @Test
  public void edition_is_not_generated_if_a_plugin_has_no_compatible_release() throws Exception {
    Sonar sonarqube = new Sonar().setReleases(new String[] {"99.2"});
    UpdateCenter updateCenter = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonarqube);

    EditionTemplate template = newEnterpriseTemplate()
      .setPluginKeys(asList("cobol"))
      .build();
    when(templateLoader.load()).thenReturn(asList(template));

    EditionsGenerator underTest = new EditionsGenerator(updateCenter, templateLoader, "1234");
    underTest.generateZipsJsonHtml(outputDir, generator);

    verifyZeroInteractions(generator);
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

  private void makePluginVersionCompatibleWith(Plugin plugin, String pluginVersion, String... sqVersions) {
    Release release = new Release(plugin, pluginVersion);
    release.addRequiredSonarVersions(sqVersions);
    String filename = plugin.getKey() + "-" + pluginVersion + ".jar";
    release.setDownloadUrl("http://server/" + filename);
    plugin.addRelease(release);
  }

  private static Plugin newPlugin(String key) {
    return Plugin.factory(key);
  }

}
