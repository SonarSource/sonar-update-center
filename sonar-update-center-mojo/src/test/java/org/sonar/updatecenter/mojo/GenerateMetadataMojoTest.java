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

public class GenerateMetadataMojoTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void generate_dev_properties_and_no_html() throws Exception {
    File outputDir = temp.newFolder();

    // plugin is already cached
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.2.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.3-20110822.091313-2.jar"), outputDir);

    File inputFile = resource("update-center-template/update-center.properties");
    new GenerateMetadataMojo().setInputFile(inputFile).setOutputDir(outputDir).setDevMode(true).execute();

    // verify that properties file is generated
    File outputFile = new File(outputDir, "sonar-updates.properties");
    assertThat(outputFile).exists().isFile();
    String output = FileUtils.readFileToString(outputFile);

    // metadata loaded from properties template
    assertThat(output).contains("artifactsize.versions=0.2");
    assertThat(output).contains("artifactsize.publicVersions=0.2");
    assertThat(output).contains("artifactsize.devVersion=0.3-SNAPSHOT");

    // metadata loaded from jar manifest
    assertThat(output).contains("artifactsize.organization=SonarSource");
  }

  @Test
  public void generate_prod_properties() throws Exception {
    File outputDir = temp.newFolder();

    // plugin is already cached
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.2.jar"), outputDir);

    File inputFile = resource("update-center-template/update-center.properties");
    new GenerateMetadataMojo().setInputFile(inputFile).setOutputDir(outputDir).execute();

    // verify that properties file is generated
    File outputFile = new File(outputDir, "sonar-updates.properties");
    assertThat(outputFile).exists().isFile();
    String output = FileUtils.readFileToString(outputFile);

    // metadata loaded from properties template
    assertThat(output).contains("artifactsize.versions=0.2");
    assertThat(output).excludes("0.3-SNAPSHOT");

    // metadata loaded from jar manifest
    assertThat(output).contains("artifactsize.organization=SonarSource");
  }

  @Test
  public void generate_properties_with_requires_plugins_and_parent_properties() throws Exception {
    File outputDir = temp.newFolder();

    // plugin is already cached
    FileUtils.copyFileToDirectory(resource("csharp-plugin-1.0.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("dotnet-plugin-1.0.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("fxcop-plugin-1.0.jar"), outputDir);

    File inputFile = resource("update-center-template-for-requires-and-parent/update-center.properties");
    new GenerateMetadataMojo().setInputFile(inputFile).setOutputDir(outputDir).execute();

    // verify that properties file is generated
    File outputFile = new File(outputDir, "sonar-updates.properties");
    assertThat(outputFile).exists().isFile();
    String output = FileUtils.readFileToString(outputFile);

    assertThat(output).contains("csharp.1.0.requirePlugins=dotnet\\:1.0");
    assertThat(output).contains("fxcop.1.0.parent=dotnet");
    assertThat(output).contains("parent");
  }

  private File resource(String filename) {
    return FileUtils.toFile(getClass().getResource("/org/sonar/updatecenter/mojo/GenerateMojoTest/" + filename));
  }
}
