/*
 * SonarSource :: Update Center :: Maven Plugin
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
package org.sonar.updatecenter.mojo;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.io.FileUtils;
import org.everit.json.schema.ValidationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.updatecenter.common.PluginReferential;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.UpdateCenter;
import org.apache.maven.plugin.logging.Log;
import org.sonar.updatecenter.common.Plugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginsJsonGeneratorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldGenerateTwoJsonFiles() throws Exception {

    UpdateCenter mockUpc = mock(UpdateCenter.class);
    PluginReferential mockReferential = mock(PluginReferential.class);
    File outputDir = temp.newFolder();
    Log mockLog = mock(Log.class);

    // Sonar.getLastRelease is final, can't mock
    Sonar stubbedSonar = new Sonar();
    stubbedSonar.setLtsRelease("4.2");
    String[] sonarVersions = {"4.2", "6.6.6"};
    stubbedSonar.setReleases(sonarVersions);

    when(mockUpc.getSonar()).thenReturn(stubbedSonar);
    when(mockUpc.getUpdateCenterPluginReferential()).thenReturn(mockReferential);

    Plugin maximumPlugin = Plugin.factory("foo");
    maximumPlugin.setDescription("Here is a application of black magic");
    maximumPlugin.setOrganization("Black Magicians Corp");
    maximumPlugin.setOrganizationUrl("https://blackmagicians.com");
    maximumPlugin.setCategory("black magic");
    maximumPlugin.setLicense("Magic v6");
    maximumPlugin.setIssueTrackerUrl("https://jira.blackmagicians.com/browse/magic");
    maximumPlugin.setSourcesUrl("http://foo.bar");
    maximumPlugin.setName("Foo");

    Release oneZero = new Release(maximumPlugin, "1.0");

    Release twoZero = new Release(maximumPlugin, "2.0");
    twoZero.setChangelogUrl("https://jira.blackmagicians.com/releaseNotes/2.0");
    twoZero.setDate(new Date(Instant.parse("1986-04-14T12:00:00Z").toEpochMilli()));
    twoZero.setDescription("Version with more RAM");
    twoZero.setDownloadUrl("http://foo.bar/2.0");
    twoZero.addRequiredSonarVersions("4.2");

    maximumPlugin.addRelease(oneZero);
    maximumPlugin.addRelease(twoZero);

    Plugin minimumPlugin = Plugin.factory("bar");
    minimumPlugin.setName("Bar");

    when(mockReferential.getPlugins()).thenReturn(Arrays.asList(maximumPlugin, minimumPlugin));

    PluginsJsonGenerator underTest = PluginsJsonGenerator.create(mockUpc, outputDir, mockLog);
    underTest.generateJsonFiles();

    File expectedFooFile = new File(outputDir, "foo.json");
    assertThat(expectedFooFile).exists().isFile();
    String fooFileContent = FileUtils.readFileToString(expectedFooFile, StandardCharsets.UTF_8);
    JSONAssert.assertEquals("{\n" +
      "  \"key\": \"foo\",\n" +
      "  \"name\": \"Foo\",\n" +
      "  \"organization\": {\n" +
      "    \"name\": \"Black Magicians Corp\",\n" +
      "    \"url\": \"https://blackmagicians.com\"\n" +
      "  },\n" +
      "  \"category\": \"black magic\",\n" +
      "  \"license\": \"Magic v6\",\n" +
      "  \"sourcesURL\": \"http://foo.bar\",\n" +
      "  \"issueTrackerURL\": \"https://jira.blackmagicians.com/browse/magic\",\n" +
      "  \"versions\": [\n" +
      "    {\n" +
      "      \"version\": \"2.0\",\n" +
      "      \"description\": \"Version with more RAM\",\n" +
      "      \"archived\": false,\n" +
      "      \"date\": \"1986-04-14\",\n" +
      "      \"changeLogUrl\": \"https://jira.blackmagicians.com/releaseNotes/2.0\"," +
      "      \"downloadURL\": \"http://foo.bar/2.0\","+
      "      \"compatibility\": \"4.2 (Compatible with LTS)\" "+
      "    },\n" +
      "    {\n" +
      "      \"version\": \"1.0\",\n" +
      "      \"archived\": false\n" +
      "    }\n" +
      "  ]\n" +
      "}", fooFileContent, true);

    File expectedBarFile = new File(outputDir, "bar.json");
    assertThat(expectedBarFile).exists().isFile();
    String barFileContent = FileUtils.readFileToString(expectedBarFile, StandardCharsets.UTF_8);
    JSONAssert.assertEquals("{\n" +
      "  \"key\": \"bar\",\n" +
      "  \"name\": \"Bar\",\n" +
      "  \"versions\": [] \n" +
      "}", barFileContent, true);

    assertThat(new File(outputDir, "plugin-schema.json")).exists().isFile();
  }


  @Test
  public void shouldNotOutputFilesNotCompliantWithSchema() throws Exception {

    UpdateCenter mockUpc = mock(UpdateCenter.class);
    PluginReferential mockReferential = mock(PluginReferential.class);
    File outputDir = temp.newFolder();
    Log mockLog = mock(Log.class);

    // Sonar.getLastRelease is final, can't mock
    Sonar stubbedSonar = new Sonar();
    stubbedSonar.setLtsRelease("1.0");
    String[] sonarVersions = {"1.0"};
    stubbedSonar.setReleases(sonarVersions);

    when(mockUpc.getSonar()).thenReturn(stubbedSonar);
    when(mockUpc.getUpdateCenterPluginReferential()).thenReturn(mockReferential);

    Plugin bad = Plugin.factory("bad");
    // missing name
    when(mockReferential.getPlugins()).thenReturn(Arrays.asList(bad));

    PluginsJsonGenerator underTest = PluginsJsonGenerator.create(mockUpc, outputDir, mockLog);

    assertThatThrownBy( () ->    underTest.generateJsonFiles()).isInstanceOf(ValidationException.class);
    File nonExpectedFile = new File(outputDir, "bad.json");
    assertThat(nonExpectedFile).doesNotExist();

    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockLog).error(messageCaptor.capture());
    assertThat(messageCaptor.getValue()).startsWith("bad json not compliant");

    assertThat(new File(outputDir, "plugin-schema.json")).doesNotExist();
  }

}
