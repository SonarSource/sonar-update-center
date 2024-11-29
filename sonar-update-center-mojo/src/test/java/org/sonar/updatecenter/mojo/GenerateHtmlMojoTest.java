/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.updatecenter.mojo;

import java.io.IOException;
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

    assertCompatibilityMatrixExist(outputDir, "compatibility-matrix.html");
    assertCompatibilityMatrixExist(outputDir, "compatibility-matrix-sqs.html");
    assertCompatibilityMatrixExist(outputDir, "compatibility-matrix-sqcb.html");

    assertThat(new File(outputDir, "html/styles.css")).exists().isFile();
    assertThat(new File(outputDir, "html/error.png")).exists().isFile();
  }

  private void assertCompatibilityMatrixExist(File outputDir, String fileName) throws IOException {
    File compatibilityMatrix = new File(outputDir, "html/" + fileName);
    assertThat(compatibilityMatrix).exists().isFile();
    String html = FileUtils.readFileToString(compatibilityMatrix, StandardCharsets.UTF_8);
    assertThat(html).contains("Artifact Size");
  }

  private File resource(String filename) {
    return FileUtils.toFile(getClass().getResource("/org/sonar/updatecenter/mojo/GenerateMojoTest/" + filename));
  }
}
