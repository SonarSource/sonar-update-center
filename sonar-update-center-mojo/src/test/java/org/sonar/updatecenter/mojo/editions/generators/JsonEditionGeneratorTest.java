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
package org.sonar.updatecenter.mojo.editions.generators;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.updatecenter.mojo.editions.Edition;

import static java.nio.charset.StandardCharsets.UTF_8;

public class JsonEditionGeneratorTest {
  private final String DOWNLOAD_BASE_URL = "http://bintray";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File outputDir;
  private JsonEditionGenerator generator;

  @Before
  public void setUp() throws IOException {
    outputDir = temp.newFolder("output");
  }

  @Test
  public void write_editions_as_json_with_baseUrl_ending_with_slash() throws Exception {
    generator = new JsonEditionGenerator(DOWNLOAD_BASE_URL);
    test();
  }

  @Test
  public void write_editions_as_json_with_baseUrl_not_ending_with_slash() throws Exception {
    generator = new JsonEditionGenerator(DOWNLOAD_BASE_URL + "/");
    test();
  }

  private void test() throws IOException, JSONException {
    List<Edition> editions = Arrays.asList(
      newEdition("community", "6.7"),
      newEdition("community", "7.0"),
      newEdition("enterprise", "6.7"));

    String json = toJson(editions);

    String expectedJson = IOUtils.toString(getClass().getResource("JsonEditionGeneratorTest/expected.json"), UTF_8);
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  private Edition newEdition(String key, String sqVersion) {
    return new Edition.Builder()
      .setKey(key)
      .setName(key + "_name")
      .setTextDescription(key + " Edition")
      .setSonarQubeVersion(sqVersion)
      .setHomeUrl(key + "/home")
      .setRequestUrl(key + "/request")
      .setZipFileName(key + "-edition-" + sqVersion + ".zip")
      .build();
  }

  private String toJson(List<Edition> editions) throws IOException {
    generator.generate(outputDir, editions);
    return FileUtils.readFileToString(new File(outputDir, JsonEditionGenerator.FILE_NAME), StandardCharsets.UTF_8);
  }
}
