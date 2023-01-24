/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2023 SonarSource SA
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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "generate-metadata", requiresProject = false, threadSafe = true)
public class GenerateMetadataMojo extends AbstractMojo {

  /**
   * The directory that contains generated files and cache of plugins.
   */
  @Parameter(property = "outputDir", required = true)
  File outputDir;

  /**
   * The path to the metadata file
   */
  @Parameter(property = "inputFile", required = true)
  File inputFile;

  /**
   * Should we consider private and dev versions
   */
  @Parameter(property = "devMode")
  boolean devMode = false;

  /**
   * Should we fail fast on errors
   */
  @Parameter(property = "ignoreErrors")
  boolean ignoreErrors = false;

  /**
   * Should we include archived versions in public versions
   */
  @Parameter(property = "includeArchives")
  boolean includeArchives = false;

  /**
   * Should we check if the download URLs are still valid for cached releases
   */
  @Parameter(property = "checkDownloadUrls")
  boolean checkDownloadUrls = true;

  /**
   * Should we only validate the marketplace properties files
   */
  @Parameter(property = "validateOnly")
  boolean validateOnly = false;

  @Override
  public void execute() throws MojoExecutionException {
    try {
      Configuration configuration = new Configuration(outputDir, inputFile, devMode, ignoreErrors, includeArchives, checkDownloadUrls, getLog());

      // Are we in validation mode? If so, stop here.
      if (validateOnly) {
        return;
      }

      // generate properties
      new Generator(configuration, getLog()).generateMetadata();

    } catch (Exception e) {
      throw new MojoExecutionException("Fail to execute mojo", e);
    }
  }
}
