/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * @goal generate-html
 * @requiresProject false
 */
public class GenerateHtmlHeadersMojo extends AbstractMojo {

  /**
   * The directory that contains generated files and cache of plugins.
   *
   * @parameter expression="${outputDir}"
   * @required
   */
  private File outputDir;

  /**
   * The path to the metadata file
   *
   * @parameter expression="${inputFile}"
   * @required
   */
  private File inputFile;

  /**
   * Should we fail fast on errors
   *
   * @parameter expression="${ignoreErrors}"
   */
  private boolean ignoreErrors = false;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      Configuration configuration = new Configuration(outputDir, inputFile, false, ignoreErrors, getLog());
      new Generator(configuration, getLog()).generateHtml();
    } catch (Exception e) {
      throw new MojoExecutionException("Fail to execute mojo", e);
    }
  }

  @VisibleForTesting
  GenerateHtmlHeadersMojo setOutputDir(File d) {
    this.outputDir = d;
    return this;
  }

  @VisibleForTesting
  GenerateHtmlHeadersMojo setInputFile(File f) {
    this.inputFile = f;
    return this;
  }
}
