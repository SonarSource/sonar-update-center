/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2020 SonarSource SA
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

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.updatecenter.common.PluginReferential;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Scanner;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.UpdateCenter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScannerJsonGeneratorTest {

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

    Scanner scanner = Scanner.factory("foo");
    scanner.setName("foo");
    scanner.setSourcesUrl("http://foo.bar");
    scanner.setDescription("Here is a application of black magic");
    scanner.setOrganization("Black Magicians Corp");
    scanner.setOrganizationUrl("https://blackmagicians.com");
    scanner.setCategory("black magic");
    scanner.setLicense("Magic v6");
    scanner.setIssueTrackerUrl("https://jira.blackmagicians.com/browse/magic");

    Release twoZero = new Release(scanner, "2.0");
    twoZero.setChangelogUrl("https://jira.blackmagicians.com/releaseNotes/2.0");
    twoZero.setDate(new Date(Instant.parse("1986-04-14T12:00:00Z").toEpochMilli()));
    twoZero.setDescription("Version with more RAM");
    twoZero.setDownloadUrl("http://foo.bar/2.0");

    scanner.addRelease(twoZero);


    when(mockUpc.getScanners()).thenReturn(Arrays.asList(scanner));

    ScannerJsonGenerator underTest = ScannerJsonGenerator.create(mockUpc, outputDir, mockLog);
    underTest.generateJsonFiles();

    File expectedFooFile = new File(outputDir, "foo.json");
    assertThat(expectedFooFile).exists().isFile();
    String fooFileContent = FileUtils.readFileToString(expectedFooFile, StandardCharsets.UTF_8);
    JSONAssert.assertEquals("{\n" +
      "  \"key\": \"foo\",\n" +
      "  \"name\": \"foo\",\n" +
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
      "      \"downloadURL\": [{\"url\":\"http://foo.bar/2.0\"}]" +
      "    }\n" +
      "  ]\n" +
      "}", fooFileContent, true);


    assertThat(new File(outputDir, "scanner-schema.json")).exists().isFile();
  }
}
