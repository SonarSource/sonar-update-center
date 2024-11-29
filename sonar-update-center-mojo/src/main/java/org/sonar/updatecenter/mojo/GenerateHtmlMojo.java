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

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "generate-html", requiresProject = false, threadSafe = true)
public class GenerateHtmlMojo extends AbstractMojo {

  /**
   * The directory that contains generated files and cache of plugins.
   */
  @Parameter(property = "outputDir", required = true)
  private File outputDir;

  /**
   * The path to the metadata file
   */
  @Parameter(property = "inputFile", required = true)
  private File inputFile;

  /**
   * Should we fail fast on errors
   */
  @Parameter(property = "ignoreErrors")
  private boolean ignoreErrors = false;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      Configuration configuration = new Configuration(outputDir, inputFile, false, ignoreErrors, false, false, getLog());
      new Generator(configuration, getLog()).generateHtml();
    } catch (Exception e) {
      throw new MojoExecutionException("Fail to execute mojo", e);
    }
  }

  GenerateHtmlMojo setOutputDir(File d) {
    this.outputDir = d;
    return this;
  }

  GenerateHtmlMojo setInputFile(File f) {
    this.inputFile = f;
    return this;
  }
}
