/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2018 SonarSource SA
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

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class GenerateMetadataMojoTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void generate_properties_with_requires_plugins() throws Exception {
    File outputDir = temp.newFolder();

    // plugin is already cached
    FileUtils.copyFileToDirectory(resource("csharp-plugin-1.0.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("dotnet-plugin-1.0.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("fxcop-plugin-1.0.jar"), outputDir);

    File inputFile = resource("update-center-template-for-requires-and-parent/update-center.properties");
    GenerateMetadataMojo underTest = new GenerateMetadataMojo();
    underTest.inputFile = inputFile;
    underTest.outputDir = outputDir;
    underTest.editionsDownloadBaseUrl = "http://bintray/";
    underTest.editionsOutputDir = temp.newFolder();
    underTest.editionTemplateProperties = new File("src/test/resources/org/sonar/updatecenter/mojo/GenerateMojoTest/edition-templates.properties");
    underTest.execute();

    // verify that properties file is generated
    File outputFile = new File(outputDir, "sonar-updates.properties");
    assertThat(outputFile).exists().isFile();
    String output = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8);

    assertThat(output).contains("csharp.1.0.requirePlugins=dotnet\\:1.0");
    assertThat(output).contains("csharp.1.0.displayVersion=1.0 (build 42)");
  }

  @Test
  public void generate_properties_with_requires_plugins_dev_mode() throws Exception {
    File outputDir = temp.newFolder();

    // plugin is already cached
    FileUtils.copyFileToDirectory(resource("csharp-plugin-1.0.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("dotnet-plugin-1.0.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("fxcop-plugin-1.0.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("dotnet-plugin-1.1-SNAPSHOT.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("fxcop-plugin-1.1-SNAPSHOT.jar"), outputDir);

    File inputFile = resource("update-center-template-for-requires-and-parent/update-center.properties");
    Configuration configuration = new Configuration(outputDir, inputFile, true, false, false, new SystemStreamLog());
    configuration.getUpdateCenter().getUpdateCenterPluginReferential().findPlugin("csharp").getRelease("1.1-SNAPSHOT")
      .setDownloadUrl(url("csharp-plugin-1.1-SNAPSHOT.jar").toString());
    configuration.getUpdateCenter().getUpdateCenterPluginReferential().findPlugin("fxcop").getRelease("1.1-SNAPSHOT")
      .setDownloadUrl(url("fxcop-plugin-1.1-SNAPSHOT.jar").toString());
    configuration.getUpdateCenter().getUpdateCenterPluginReferential().findPlugin("dotnet").getRelease("1.1-SNAPSHOT")
      .setDownloadUrl(url("dotnet-plugin-1.1-SNAPSHOT.jar").toString());
    new Generator(configuration, new SystemStreamLog()).generateMetadata();

    // verify that properties file is generated
    File outputFile = new File(outputDir, "sonar-updates.properties");
    assertThat(outputFile).exists().isFile();
    String output = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8);

    assertThat(output).contains("csharp.1.1-SNAPSHOT.requirePlugins=dotnet\\:1.1");
  }

  @Test
  public void generate_properties_including_archived_versions() throws Exception {
    File outputDir = temp.newFolder();

    // plugin is already cached
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.2.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.3.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.4.jar"), outputDir);

    File inputFile = resource("update-center-template/update-center.properties");
    Configuration configuration = new Configuration(outputDir, inputFile, false, false, true, new SystemStreamLog());
    new Generator(configuration, new SystemStreamLog()).generateMetadata();

    // verify that properties file is generated
    File outputFile = new File(outputDir, "sonar-updates.properties");
    assertThat(outputFile).exists().isFile();
    String output = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8);

    assertThat(output).contains("artifactsize.versions=0.2,0.3");
  }

  // UPC-97
  @Test
  public void fail_if_key_mismatch() throws Exception {
    File outputDir = temp.newFolder();

    // plugin is already cached
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.2.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.3.jar"), outputDir);
    FileUtils.copyFileToDirectory(resource("sonar-artifact-size-plugin-0.4.jar"), outputDir);

    File inputFile = resource("key-mismatch/update-center.properties");
    Configuration configuration = new Configuration(outputDir, inputFile, false, false, true, new SystemStreamLog());
    try {
      new Generator(configuration, new SystemStreamLog()).generateMetadata();
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class)
        .hasMessage("Plugin sonar-artifact-size-plugin-0.3.jar is declared with key 'artifactsize' in its MANIFEST, but with key 'artifactsize2' in the update center");
    }
  }

  private File resource(String filename) {
    return FileUtils.toFile(url(filename));
  }

  private URL url(String filename) {
    return getClass().getResource("/org/sonar/updatecenter/mojo/GenerateMojoTest/" + filename);
  }
}
