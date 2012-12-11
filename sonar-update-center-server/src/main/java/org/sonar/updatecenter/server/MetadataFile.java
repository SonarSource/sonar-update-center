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

import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.UpdateCenterDeserializer;

import java.io.File;
import java.io.IOException;

public final class MetadataFile {

  private Configuration conf;

  public MetadataFile(Configuration conf) {
    this.conf = conf;
  }

  public File getFile() {
    try {
      File file = new File(conf.getSourcePath());
      if (!file.exists()) {
        throw new IllegalStateException("The metadata file does not exist: " + file.getPath());
      }
      return file;
    } catch (Exception e) {
      throw new IllegalStateException("Can not open the metadata file: " + conf.getSourcePath(), e);
    }
  }

  public UpdateCenter getUpdateCenter() {
    File file = getFile();
    try {
      return UpdateCenterDeserializer.fromProperties(file);

    } catch (IOException e) {
      throw new IllegalStateException("Can not read properties from: " + file.getPath(), e);
    }
  }

}
