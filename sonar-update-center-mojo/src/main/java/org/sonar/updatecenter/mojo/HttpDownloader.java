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
  private final boolean verifyUrlIfCached;
  private final Log log;

  public HttpDownloader(File outputDir, boolean verifyUrlIfCached, Log log) {
    this.outputDir = outputDir;
    this.verifyUrlIfCached = verifyUrlIfCached;
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
      if (verifyUrlIfCached && !verifyDownloadUrl(new URL(url))) {
        throw new IllegalStateException(String.format("Failed to download %s, URL is no longer valid!", url));
      }
    }
    return output;
  }

  File downloadFile(URL fileURL, File toFile) {
    log.info(String.format("Download %s in %s", fileURL, toFile));
    try {
      if ("file".equals(fileURL.getProtocol())) {
        File src = new File(fileURL.toURI());
        FileUtils.copyFile(src, toFile);
      } else {
        HttpRequest request = HttpRequest.get(fileURL).followRedirects(true);
        addAuthorizationHeader(request, fileURL);

        if (!request.receive(toFile).ok()) {
          throw new IllegalStateException(request.message());
        }
      }
    } catch (Exception e) {
      FileUtils.deleteQuietly(toFile);
      throw new IllegalStateException(String.format("Fail to download %s to %s", fileURL, toFile), e);
    }
    return toFile;
  }


  boolean verifyDownloadUrl(URL fileURL) {
    log.debug(String.format("Verify download URL (%s) is still valid", fileURL));
    try {
      if ("file".equals(fileURL.getProtocol())) {
        File src = new File(fileURL.toURI());
        return src.exists();
      } else {
        HttpRequest request = HttpRequest.head(fileURL).followRedirects(true);
        addAuthorizationHeader(request, fileURL);

        if (request.ok()) {
          return true;
        } else {
          // Some services refuse HEAD requests. Try a GET instead.
          log.debug(String.format("Download URL (%s) failed with a HEAD request. Double check using GET...", fileURL));

          request = HttpRequest.get(fileURL).followRedirects(true);
          addAuthorizationHeader(request, fileURL);

          if (request.ok()) {
            log.debug(String.format("Download URL (%s) is still valid", fileURL));
            return true;
          } else {
            log.error(String.format("Download URL (%s) is no longer valid (HTTP status: %d)", fileURL, request.code()));
            return false;
          }
        }
      }
    } catch (Exception e) {
      return false;
    }
  }

  private static void addAuthorizationHeader(HttpRequest request, URL fileURL) {
    if (fileURL.getUserInfo() != null) {
      request.header("Authorization", "Basic " + HttpRequest.Base64.encode(fileURL.getUserInfo()));
    }
  }

}
