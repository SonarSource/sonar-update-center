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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class GenerateHtmlMojoTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void generate_prod_html_without_private() throws Exception {
    File outputDir = temp.newFolder();

    // plugin is already cached
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.2.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.3.jar"), outputDir);

    File inputFile = resource("update-center-template/update-center.properties");
    new GenerateHtmlMojo().setInputFile(inputFile).setOutputDir(outputDir).execute();

    File compatibilityMatrix = new File(outputDir, "html/compatibility-matrix.html");
    assertThat(compatibilityMatrix).exists().isFile();
    String html = FileUtils.readFileToString(compatibilityMatrix, StandardCharsets.UTF_8);
    assertThat(html).contains("Artifact Size");

    assertThat(new File(outputDir, "html/styles.css")).exists().isFile();
    assertThat(new File(outputDir, "html/error.png")).exists().isFile();
    assertThat(new File(outputDir, "html/onde-sonar-16.png")).exists().isFile();
  }

  private File resource(String filename) {
    return FileUtils.toFile(getClass().getResource("/org/sonar/updatecenter/mojo/GenerateMojoTest/" + filename));
  }
}
