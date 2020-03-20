/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2020 SonarSource SA
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
package org.sonar.updatecenter.mojo.editions.generators;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.sonar.updatecenter.mojo.editions.Edition;

public class ZipsEditionGenerator implements EditionGenerator {
  private final File jarsDir;

  public ZipsEditionGenerator(File jarsDir) {
    if (!jarsDir.exists()) {
      throw new IllegalArgumentException("Directory does not exist: " + jarsDir.getAbsolutePath());
    }
    this.jarsDir = jarsDir;
  }

  /**
   * Example of files generated in outputDir:
   * - edition1-6.7.zip
   * - edition1-7.0.zip
   * - edition2-6.7.zip
   * - edition2-7.0.zip
   */
  @Override
  public void generate(File outputDir, List<Edition> editions) throws Exception {
    for (Edition e : editions) {
      if (e.getZipFileName() != null) {
        zip(new File(outputDir, e.getZipFileName()), jarFiles(e.jars()));
      }
    }
  }

  private List<File> jarFiles(Set<String> jarNames) {
    return jarNames.stream()
      .map(this::jarFile)
      .collect(Collectors.toList());
  }

  private File jarFile(String jarName) {
    File jarFile = new File(jarsDir, jarName);
    if (!jarFile.exists() || !jarFile.isFile()) {
      throw new IllegalArgumentException("File does not exist: " + jarFile);
    }
    return jarFile;
  }

  private static File zip(File zipFile, List<File> files) throws IOException {

    try (ZipOutputStream zipOutput = new ZipOutputStream(new FileOutputStream(zipFile, false))) {
      for (File file : files) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
          ZipEntry entry = new ZipEntry(file.getName());
          zipOutput.putNextEntry(entry);
          IOUtils.copy(in, zipOutput);
          zipOutput.closeEntry();
        }
      }
    }
    return zipFile;
  }
}
