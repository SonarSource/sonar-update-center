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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * @goal generate
 * @requiresProject false
 */
public class GenerateMojo extends AbstractMojo {

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

  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      Configuration configuration = new Configuration(outputDir, inputFile, getLog());
      new Generator(configuration, getLog()).generate();
    } catch (Exception e) {
      throw new MojoExecutionException("Fail to execute mojo", e);
    }
  }

}
