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
import java.util.Collections;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.mojo.editions.Edition;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HtmlEditionGeneratorTest {
  private static final String DOWNLOAD_URL = "http://sonarsource.com/";
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private Sonar sonar = new Sonar();
  private HtmlEditionGenerator generator;
  private File outputDir;
  private UpdateCenter uc;

  @Before
  public void setUp() throws IOException {
    outputDir = temp.newFolder("output");
    uc = mockUpdateCenter();
    generator = new HtmlEditionGenerator(uc, DOWNLOAD_URL);
  }

  @Test
  public void test() throws IOException {
    sonar.setLtsRelease("6.7.1");
    sonar.setReleases(new String[] {"6.7.1", "6.7", "7.0"});

    generator.generate(outputDir, Collections.singletonList(newEdition("dev", "6.7.1")));
    File htmlFile = new File(outputDir, "edition-dev.html");
    assertThat(htmlFile).exists();
    String htmlFileContent = FileUtils.readFileToString(htmlFile, StandardCharsets.UTF_8);
    assertThat(htmlFileContent).contains(loadExpectedTable());
  }

  private UpdateCenter mockUpdateCenter() {
    UpdateCenter uc = mock(UpdateCenter.class);
    when(uc.getSonar()).thenReturn(sonar);
    return uc;
  }

  private String loadExpectedTable() throws IOException {
    return IOUtils.toString(getClass().getResource("HtmlEditionGeneratorTest/table.html"), UTF_8);
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

}
