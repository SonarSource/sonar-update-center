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

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.UpdateCenterDeserializer;
import org.sonar.updatecenter.common.UpdateCenterDeserializer.Mode;

import java.io.File;
import java.io.IOException;

class Configuration {

  private File outputDir, inputFile;
  private UpdateCenter updateCenter;

  Configuration(File outputDir, File inputFile, boolean devMode, boolean ignoreErrors, Log log) {
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
    try {
      this.updateCenter = new UpdateCenterDeserializer(devMode ? Mode.DEV : Mode.PROD, ignoreErrors).fromManyFiles(inputFile);
    } catch (IOException e) {
      throw new IllegalStateException("Can not read properties from: " + inputFile, e);
    }
  }

  private void log(Log log) {
    String commentLine = "-------------------------------";
    log.info(commentLine);
    log.info("outputDir: " + outputDir.getAbsolutePath());
    log.info("inputFile: " + inputFile.getAbsolutePath());
    log.info(commentLine);
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

  UpdateCenter getUpdateCenter() {
    return this.updateCenter;
  }
}
