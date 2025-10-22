/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2025 SonarSource SA
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

class HttpDownloader {

  private final File outputDir;
  private final boolean verifyUrlIfCached;
  private final Log log;

  public HttpDownloader(File outputDir, boolean verifyUrlIfCached, Log log) {
    this.outputDir = outputDir;
    this.verifyUrlIfCached = verifyUrlIfCached;
    this.log = log;
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

  protected void downloadFile(URL fileURL, File toFile) {
    log.info(String.format("Download %s in %s", fileURL, toFile));
    try {
      if ("file".equals(fileURL.getProtocol())) {
        File src = new File(fileURL.toURI());
        FileUtils.copyFile(src, toFile);
      } else {
        try (CloseableHttpClient httpClient = createHttpClient()) {
          HttpGet httpGet = new HttpGet(fileURL.toString());
          addAuthorizationHeader(httpGet, fileURL);

          try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
              throw new IllegalStateException("HTTP error: " + response.getStatusLine().getReasonPhrase());
            }

            try (InputStream inputStream = response.getEntity().getContent();
                 FileOutputStream outputStream = new FileOutputStream(toFile)) {
              byte[] buffer = new byte[8192];
              int bytesRead;
              while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      FileUtils.deleteQuietly(toFile);
      throw new IllegalStateException(String.format("Fail to download %s to %s", fileURL, toFile), e);
    }
  }

  boolean verifyDownloadUrl(URL fileURL) {
    log.debug(String.format("Verify download URL (%s) is still valid", fileURL));
    try {
      if ("file".equals(fileURL.getProtocol())) {
        File src = new File(fileURL.toURI());
        return src.exists();
      } else {
        try (CloseableHttpClient httpClient = createHttpClient()) {
          HttpHead httpHead = new HttpHead(fileURL.toString());
          addAuthorizationHeader(httpHead, fileURL);

          try (CloseableHttpResponse response = httpClient.execute(httpHead)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
              return true;
            } else {
              // Some services refuse HEAD requests. Try a GET instead.
              log.debug(String.format("Download URL (%s) failed with a HEAD request. Double check using GET...", fileURL));

              HttpGet httpGet = new HttpGet(fileURL.toString());
              addAuthorizationHeader(httpGet, fileURL);

              try (CloseableHttpResponse getResponse = httpClient.execute(httpGet)) {
                statusCode = getResponse.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                  log.debug(String.format("Download URL (%s) is still valid", fileURL));
                  return true;
                } else {
                  log.error(String.format("Download URL (%s) is no longer valid (HTTP status: %d)", fileURL, statusCode));
                  return false;
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      return false;
    }
  }

  private static CloseableHttpClient createHttpClient() {
    RequestConfig config = RequestConfig.custom()
      .setRedirectsEnabled(true)
      .setMaxRedirects(10)
      .setConnectTimeout(30000)
      .setSocketTimeout(30000)
      .build();

    return HttpClients.custom()
      .setDefaultRequestConfig(config)
      .build();
  }

  private static void addAuthorizationHeader(HttpGet request, URL fileURL) {
    if (fileURL.getUserInfo() != null) {
      String encodedAuth = Base64.getEncoder().encodeToString(fileURL.getUserInfo().getBytes(StandardCharsets.UTF_8));
      request.setHeader("Authorization", "Basic " + encodedAuth);
    }
  }

  private static void addAuthorizationHeader(HttpHead request, URL fileURL) {
    if (fileURL.getUserInfo() != null) {
      String encodedAuth = Base64.getEncoder().encodeToString(fileURL.getUserInfo().getBytes(StandardCharsets.UTF_8));
      request.setHeader("Authorization", "Basic " + encodedAuth);
    }
  }

}
