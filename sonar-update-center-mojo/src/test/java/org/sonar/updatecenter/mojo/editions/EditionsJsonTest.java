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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONAssert;

public class EditionsJsonTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void write_editions_as_json_with_baseUrl_ending_with_slash() throws Exception {
    test("http://bintray/");
  }

  @Test
  public void write_editions_as_json_with_baseUrl_not_ending_with_slash() throws Exception {
    test("http://bintray");
  }

  private void test(String downloadBaseUrl) throws IOException, JSONException {
    List<Edition> editions = Arrays.asList(
      newEdition("community", "6.7"),
      newEdition("community", "7.0"),
      newEdition("enterprise", "6.7"));

    String json = toJson(editions, downloadBaseUrl);

    String expectedJson = IOUtils.toString(getClass().getResource("EditionsJsonTest/expected.json"));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  private Edition newEdition(String key, String sqVersion) throws IOException {
    return new Edition.Builder()
      .setKey(key)
      .setName(key + "_name")
      .setTextDescription(key + " Edition")
      .setSonarQubeVersion(sqVersion)
      .setHomeUrl(key + "/home")
      .setRequestUrl(key + "/request")
      .setTargetZip(temp.newFile(key + "-edition-" + sqVersion + ".zip"))
      .build();
  }

  private String toJson(List<Edition> editions, String downloadBaseUrl) throws IOException {
    EditionsJson underTest = new EditionsJson();
    StringWriter writer = new StringWriter();
    underTest.write(editions, downloadBaseUrl, writer);
    return writer.toString();
  }

}
