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
package org.sonar.updatecenter.server;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal generate
 * @requiresProject false
 */
public class GenerateMojo extends AbstractMojo {

  /**
   * The working directory
   *
   * @parameter expression="${workingDir}" default-value="${basedir}/sonar-update-center"
   */
  private String workingDir;

  /**
   * The generated file containing the sonar update center properties
   *
   * @parameter expression="${outputFile}" default-value="${basedir}/sonar-update-center/generated-sonar-updates.properties"
   */
  private String outputFile;

  /**
   * The path to the metadata file
   *
   * @parameter expression="${inputFile}"
   * @required
   */
  private String inputFile;

  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      Configuration configuration = new Configuration(workingDir, outputFile, inputFile);
      Server server = new Server(configuration);
      server.start();
    } catch (Exception e) {
      throw new MojoExecutionException("Fail to execute server mojo", e);
    }
  }

}
