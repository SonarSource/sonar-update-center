/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2025 SonarSource Sàrl
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;

class HttpDownloader {

  private static final int DEFAULT_TIMEOUT_SECONDS = 30;

  private final File outputDir;
  private final boolean verifyUrlIfCached;
  private final Log log;
  private final HttpClient httpClient;

  public HttpDownloader(File outputDir, boolean verifyUrlIfCached, Log log) {
    this(outputDir, verifyUrlIfCached, log, createDefaultHttpClient());
  }

  HttpDownloader(File outputDir, boolean verifyUrlIfCached, Log log, HttpClient httpClient) {
    this.outputDir = outputDir;
    this.verifyUrlIfCached = verifyUrlIfCached;
    this.log = log;
    this.httpClient = httpClient;
  }

  private static HttpClient createDefaultHttpClient() {
    return HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
      .build();
  }

  public File download(String url, boolean force) throws IOException {
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
        HttpRequest request = buildHttpRequest(fileURL, "GET");
        HttpResponse<Path> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofFile(toFile.toPath()));

        if (!isSuccessStatusCode(response.statusCode())) {
          throw new IllegalStateException("HTTP " + response.statusCode());
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      FileUtils.deleteQuietly(toFile);
      throw new IllegalStateException(String.format("Fail to download %s to %s", fileURL, toFile), e);
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
        return verifyHttpUrl(fileURL);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean verifyHttpUrl(URL fileURL) throws URISyntaxException, IOException, InterruptedException {
    // Try HEAD request first
    HttpRequest headRequest = buildHttpRequest(fileURL, "HEAD");
    HttpResponse<Void> headResponse = httpClient.send(headRequest, HttpResponse.BodyHandlers.discarding());

    if (isSuccessStatusCode(headResponse.statusCode())) {
      return true;
    }

    // Some services refuse HEAD requests. Try a GET instead.
    log.debug(String.format("Download URL (%s) failed with a HEAD request. Double check using GET...", fileURL));
    HttpRequest getRequest = buildHttpRequest(fileURL, "GET");
    HttpResponse<Void> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.discarding());

    if (isSuccessStatusCode(getResponse.statusCode())) {
      log.debug(String.format("Download URL (%s) is still valid", fileURL));
      return true;
    }

    log.error(String.format("Download URL (%s) is no longer valid (HTTP status: %d)", fileURL, getResponse.statusCode()));
    return false;
  }

  private static HttpRequest buildHttpRequest(URL fileURL, String method) throws URISyntaxException {
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
      .uri(fileURL.toURI())
      .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));

    if ("GET".equals(method)) {
      requestBuilder.GET();
    } else if ("HEAD".equals(method)) {
      requestBuilder.method("HEAD", HttpRequest.BodyPublishers.noBody());
    }

    if (fileURL.getUserInfo() != null) {
      String encoded = Base64.getEncoder().encodeToString(fileURL.getUserInfo().getBytes(StandardCharsets.UTF_8));
      requestBuilder.header("Authorization", "Basic " + encoded);
    }

    return requestBuilder.build();
  }

  private static boolean isSuccessStatusCode(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

}
