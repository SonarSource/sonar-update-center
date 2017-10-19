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
package org.sonar.updatecenter.mojo;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.mojo.editions.EditionTemplatesLoaderImpl;
import org.sonar.updatecenter.mojo.editions.EditionsGenerator;

@Mojo(name = "generate-metadata", requiresProject = false, threadSafe = true)
public class GenerateMetadataMojo extends AbstractMojo {

  /**
   * The directory that contains generated files and cache of plugins.
   */
  @Parameter(property="outputDir", required = true)
  File outputDir;

  /**
   * The path to the metadata file
   */
  @Parameter(property="inputFile", required = true)
  File inputFile;

  /**
   * Should we consider private and dev versions
   */
  @Parameter(property="devMode")
  boolean devMode = false;

  /**
   * Should we fail fast on errors
   */
  @Parameter(property="ignoreErrors")
  boolean ignoreErrors = false;

  /**
   * Should we include archived versions in public versions
   */
  @Parameter(property="includeArchives")
  boolean includeArchives = false;

  /**
   * Base URL for hosting of editions
   */
  @Parameter(property = "editionsDownloadBaseUrl", required = true)
  String editionsDownloadBaseUrl;

  /**
   * The directory that contains generated json and zip files of editions
   */
  @Parameter(property = "editionsOutputDir", required = true)
  File editionsOutputDir;

  /**
   * The directory that contains generated json and zip files of editions
   */
  @Parameter(property = "editionTemplateProperties", defaultValue = "edition-templates.properties")
  File editionTemplateProperties;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      Configuration configuration = new Configuration(outputDir, inputFile, devMode, ignoreErrors, includeArchives, getLog());

      // generate properties
      new Generator(configuration, getLog()).generateMetadata();

      // generate editions (json + zip files)
      generateEditions(configuration.getUpdateCenter());

    } catch (Exception e) {
      throw new MojoExecutionException("Fail to execute mojo", e);
    }
  }

  private void generateEditions(UpdateCenter updateCenter) throws IOException {
    File jarsDir = outputDir;
    EditionTemplatesLoaderImpl templatesLoader = new EditionTemplatesLoaderImpl(editionTemplateProperties);
    EditionsGenerator editionsGenerator = new EditionsGenerator(updateCenter, templatesLoader, jarsDir);
    editionsGenerator.generateZipsAndJson(editionsOutputDir, editionsDownloadBaseUrl);
  }
}
