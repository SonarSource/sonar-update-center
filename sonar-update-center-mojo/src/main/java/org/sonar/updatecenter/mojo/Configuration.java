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

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.sonar.updatecenter.common.PluginReferential;

import java.io.File;
import java.io.IOException;

class Configuration {

  private File outputDir, inputFile;

  Configuration(File outputDir, File inputFile, Log log) {
    Preconditions.checkArgument(inputFile.exists(), "inputFile must exist");
    Preconditions.checkArgument(inputFile.isFile(), "inputFile must be a file");
    try {
      FileUtils.forceMkdir(outputDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create the output directory: " + outputDir.getAbsolutePath(), e);
    }
    this.outputDir = outputDir;
    this.inputFile = inputFile;
    log(log);
  }

  private void log(Log log) {
    log.info("-------------------------------");
    log.info("outputDir: " + outputDir.getAbsolutePath());
    log.info("inputFile: " + inputFile.getAbsolutePath());
    log.info("-------------------------------");
  }


  File getOutputDir() {
    return outputDir;
  }

  File getOutputFile() {
    return new File(getOutputDir(), "sonar-updates.properties");
  }

  File getInputFile() {
    return inputFile;
  }

  PluginReferential getUpdateCenter() {
    try {
      return org.sonar.updatecenter.common.PluginReferentialDeserializer.fromProperties(getInputFile());

    } catch (IOException e) {
      throw new IllegalStateException("Can not read properties from: " + getInputFile(), e);
    }
  }
}
