/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.updatecenter.mojo;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.fest.assertions.Assertions.assertThat;

public class GenerateMojoTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void generate_properties_and_html() throws IOException, URISyntaxException, MojoFailureException, MojoExecutionException {
    File outputDir = temp.newFolder();

    // plugin is already cached
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.3.jar"), outputDir);

    File inputFile = resource("update-center-template.properties");
    new GenerateMojo().setInputFile(inputFile).setOutputDir(outputDir).execute();

    // verify that properties file is generated
    File outputFile = new File(outputDir, "sonar-updates.properties");
    assertThat(outputFile).exists().isFile();
    String output = FileUtils.readFileToString(outputFile);

    // metadata loaded from properties template
    assertThat(output).contains("artifactsize.versions=0.3");

    // metadata loaded from jar manifest
    assertThat(output).contains("artifactsize.organization=SonarSource");

    // html headers
    File htmlHeader = new File(outputDir, "html/artifactsize.html");
    assertThat(htmlHeader).exists().isFile();
    assertThat(new File(outputDir, "html/style.css")).exists().isFile();
    String html = FileUtils.readFileToString(htmlHeader);
    assertThat(html).contains("<title>Artifact Size</title>");
  }

  private File resource(String filename) {
    return FileUtils.toFile(getClass().getResource("/org/sonar/updatecenter/mojo/GenerateMojoTest/" + filename));
  }
}
