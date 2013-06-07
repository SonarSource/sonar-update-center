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

import com.github.kevinsawicki.http.HttpRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

class HttpDownloader {

  private final File outputDir;
  private final Log log;

  public HttpDownloader(File outputDir, Log log) {
    this.outputDir = outputDir;
    this.log = log;
  }

  public File download(String url, boolean force) throws IOException, URISyntaxException {
    FileUtils.forceMkdir(outputDir);

    String filename = StringUtils.substringAfterLast(url, "/");
    File output = new File(outputDir, filename);
    if (force || !output.exists() || output.length() <= 0) {
      downloadFile(new URL(url), output);
    } else {
      log.info("File found in local cache: " + url);
    }
    return output;
  }

  File downloadFile(URL fileURL, File toFile) {
    log.info("Download " + fileURL + " in " + toFile);
    try {
      HttpRequest request = HttpRequest.get(fileURL).followRedirects(true);
      if (fileURL.getUserInfo() != null) {
        request.header("Authorization", "Basic " + com.github.kevinsawicki.http.HttpRequest.Base64.encode(fileURL.getUserInfo()));
      }

      if (!request.receive(toFile).ok()) {
        throw new IllegalStateException(request.message());
      }
    } catch (Exception e) {
      FileUtils.deleteQuietly(toFile);
      throw new IllegalStateException("Fail to download " + fileURL + " to " + toFile, e);
    }
    return toFile;
  }

}
