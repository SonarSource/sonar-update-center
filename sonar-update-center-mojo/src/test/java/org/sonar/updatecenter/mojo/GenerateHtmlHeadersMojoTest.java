/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.updatecenter.mojo;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class GenerateHtmlHeadersMojoTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void generate_prod_html_without_private() throws Exception {
    File outputDir = temp.newFolder();

    // plugin is already cached
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.2.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.3.jar"), outputDir);

    File inputFile = resource("update-center-template/update-center.properties");
    new GenerateHtmlHeadersMojo().setInputFile(inputFile).setOutputDir(outputDir).execute();

    // html confluence include
    File htmlConfluenceInclude = new File(outputDir, "html/artifactsize-confluence-include.html");
    assertThat(htmlConfluenceInclude).exists().isFile();
    String html = FileUtils.readFileToString(htmlConfluenceInclude);
    assertThat(html).contains("<strong>Artifact Size");

    assertThat(new File(outputDir, "html/style-confluence.css")).exists().isFile();
  }

  private File resource(String filename) {
    return FileUtils.toFile(getClass().getResource("/org/sonar/updatecenter/mojo/GenerateMojoTest/" + filename));
  }
}
