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
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.UpdateCenterDeserializer;
import org.sonar.updatecenter.common.UpdateCenterDeserializer.Mode;

class Configuration {

  private File outputDir;
  private File inputFile;
  private UpdateCenter updateCenter;
  private boolean checkDownloadUrls;

  Configuration(File outputDir, File inputFile, boolean devMode, boolean ignoreErrors, boolean includeArchives, boolean checkDownloadUrls, Log log) {
    if (!inputFile.exists() || !inputFile.isFile()) {
      throw new IllegalArgumentException("inputFile must exist");
    }
    try {
      FileUtils.forceMkdir(outputDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create the output directory: " + outputDir.getAbsolutePath(), e);
    }
    this.outputDir = outputDir;
    this.inputFile = inputFile;
    this.checkDownloadUrls = checkDownloadUrls;
    log(log);
    try {
      this.updateCenter = new UpdateCenterDeserializer(devMode ? Mode.DEV : Mode.PROD, ignoreErrors, includeArchives).fromManyFiles(inputFile);
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

  boolean mustCheckDownloadUrls() {
    return checkDownloadUrls;
  }

  UpdateCenter getUpdateCenter() {
    return this.updateCenter;
  }
}
