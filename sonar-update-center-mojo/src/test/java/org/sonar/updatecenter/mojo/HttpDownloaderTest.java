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
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Base64;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.maven.plugin.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpDownloaderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File outputDir;
  private Log log;
  private MockWebServer mockWebServer;

  @Before
  public void setUp() throws IOException {
    outputDir = temp.newFolder();
    log = mock(Log.class);
    mockWebServer = new MockWebServer();
    mockWebServer.start();
  }

  @After
  public void tearDown() throws IOException {
    if (mockWebServer != null) {
      mockWebServer.close();
    }
  }

  @Test
  public void download_whenUsingFileProtocol_shouldDownloadFile() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    File sourceFile = temp.newFile("source.txt");
    String content = "test content";
    Files.writeString(sourceFile.toPath(), content);

    URL fileUrl = sourceFile.toURI().toURL();
    File result = underTest.download(fileUrl.toString(), false);

    assertThat(result)
      .exists()
      .hasName("source.txt");
    assertThat(Files.readString(result.toPath())).isEqualTo(content);
  }

  @Test
  public void download_whenCachedFileExistsAndForceIsFalse_shouldUseCachedFile() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    File cachedFile = new File(outputDir, "cached.txt");
    String cachedContent = "cached content";
    Files.writeString(cachedFile.toPath(), cachedContent);

    File sourceFile = temp.newFile("cached.txt");
    String newContent = "new content";
    Files.writeString(sourceFile.toPath(), newContent);

    URL fileUrl = sourceFile.toURI().toURL();
    File result = underTest.download(fileUrl.toString(), false);

    assertThat(result).exists();
    assertThat(Files.readString(result.toPath())).isEqualTo(cachedContent);
  }

  @Test
  public void download_whenForceIsTrue_shouldRedownloadFile() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    File cachedFile = new File(outputDir, "forced.txt");
    String cachedContent = "cached content";
    Files.writeString(cachedFile.toPath(), cachedContent);

    File sourceFile = temp.newFile("forced.txt");
    String newContent = "new content";
    Files.writeString(sourceFile.toPath(), newContent);

    URL fileUrl = sourceFile.toURI().toURL();
    File result = underTest.download(fileUrl.toString(), true);

    assertThat(result).exists();
    assertThat(Files.readString(result.toPath())).isEqualTo(newContent);
  }

  @Test
  public void download_whenCachedFileIsEmpty_shouldDownloadFile() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    File cachedFile = new File(outputDir, "empty.txt");
    cachedFile.createNewFile();
    assertThat(cachedFile).isEmpty();

    File sourceFile = temp.newFile("empty.txt");
    String content = "content";
    Files.writeString(sourceFile.toPath(), content);

    URL fileUrl = sourceFile.toURI().toURL();
    File result = underTest.download(fileUrl.toString(), false);

    assertThat(result).exists();
    assertThat(Files.readString(result.toPath())).isEqualTo(content);
  }

  @Test
  public void download_whenOutputDirectoryDoesNotExist_shouldCreateDirectory() throws Exception {
    File nonExistentDir = new File(temp.getRoot(), "new-dir");
    assertThat(nonExistentDir).doesNotExist();

    HttpDownloader downloaderWithNewDir = new HttpDownloader(nonExistentDir, false, log);

    File sourceFile = temp.newFile("test.txt");
    Files.writeString(sourceFile.toPath(), "test");

    URL fileUrl = sourceFile.toURI().toURL();
    downloaderWithNewDir.download(fileUrl.toString(), false);

    assertThat(nonExistentDir).exists().isDirectory();
  }

  @Test
  public void download_whenValidUrl_shouldExtractFilename() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    File sourceFile = temp.newFile("myfile.jar");
    Files.writeString(sourceFile.toPath(), "content");

    URL fileUrl = sourceFile.toURI().toURL();
    File result = underTest.download(fileUrl.toString(), false);

    assertThat(result).hasName("myfile.jar");
  }

  @Test
  public void verifyDownloadUrl_whenFileExists_shouldReturnTrue() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    File existingFile = temp.newFile("existing.txt");
    Files.writeString(existingFile.toPath(), "content");

    URL fileUrl = existingFile.toURI().toURL();
    boolean result = underTest.verifyDownloadUrl(fileUrl);

    assertThat(result).isTrue();
  }

  @Test
  public void verifyDownloadUrl_whenFileDoesNotExist_shouldReturnFalse() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    File nonExistentFile = new File(temp.getRoot(), "nonexistent.txt");
    assertThat(nonExistentFile).doesNotExist();

    URL fileUrl = nonExistentFile.toURI().toURL();
    boolean result = underTest.verifyDownloadUrl(fileUrl);

    assertThat(result).isFalse();
  }

  @Test
  public void download_whenVerifyUrlIfCachedEnabledAndUrlInvalid_shouldThrowException() throws Exception {
    HttpDownloader strictDownloader = new HttpDownloader(outputDir, true, log);

    File cachedFile = new File(outputDir, "verified.txt");
    Files.writeString(cachedFile.toPath(), "content");

    File sourceFile = temp.newFile("verified.txt");
    URL fileUrl = sourceFile.toURI().toURL();
    String urlString = fileUrl.toString();
    sourceFile.delete();

    assertThatThrownBy(() -> strictDownloader.download(urlString, false))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("URL is no longer valid");
  }

  @Test
  public void downloadFile_whenDownloadFails_shouldCleanup() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    URL invalidUrl = new URL("file:///nonexistent/path/to/file.txt");
    File targetFile = new File(outputDir, "test.txt");

    assertThatThrownBy(() -> underTest.downloadFile(invalidUrl, targetFile))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Fail to download");
  }

  @Test
  public void download_whenUrlHasComplexFilename_shouldExtractCorrectFilename() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    File sourceFile = temp.newFile("plugin-1.2.3-SNAPSHOT.jar");
    Files.writeString(sourceFile.toPath(), "content");

    URL fileUrl = sourceFile.toURI().toURL();
    File result = underTest.download(fileUrl.toString(), false);

    assertThat(result).hasName("plugin-1.2.3-SNAPSHOT.jar");
  }

  @Test
  public void download_whenMultipleDownloadsToSameDirectory_shouldHandleCorrectly() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    File sourceFile1 = temp.newFile("file1.txt");
    File sourceFile2 = temp.newFile("file2.txt");
    Files.writeString(sourceFile1.toPath(), "content1");
    Files.writeString(sourceFile2.toPath(), "content2");

    File result1 = underTest.download(sourceFile1.toURI().toURL().toString(), false);
    File result2 = underTest.download(sourceFile2.toURI().toURL().toString(), false);

    assertThat(result1).exists();
    assertThat(result2).exists();
    assertThat(Files.readString(result1.toPath())).isEqualTo("content1");
    assertThat(Files.readString(result2.toPath())).isEqualTo("content2");
  }

  @Test
  public void download_whenForceDownloading_shouldOverwriteExistingFile() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    File sourceFile = temp.newFile("overwrite.txt");
    Files.writeString(sourceFile.toPath(), "original");

    URL fileUrl = sourceFile.toURI().toURL();
    underTest.download(fileUrl.toString(), false);

    Files.writeString(sourceFile.toPath(), "updated");

    File result = underTest.download(fileUrl.toString(), true);

    assertThat(Files.readString(result.toPath())).isEqualTo("updated");
  }

  @Test
  public void downloadFile_whenLocalFileProtocol_shouldCopyFile() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    File sourceFile = temp.newFile("local.txt");
    String content = "local file content";
    Files.writeString(sourceFile.toPath(), content);

    File targetFile = new File(outputDir, "target.txt");
    URL fileUrl = sourceFile.toURI().toURL();
    File result = underTest.downloadFile(fileUrl, targetFile);

    assertThat(result)
      .exists()
      .isEqualTo(targetFile);
    assertThat(Files.readString(result.toPath())).isEqualTo(content);
  }

  @Test
  public void downloadFile_whenDownloadFails_shouldCleanupTargetFile() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    File targetFile = new File(outputDir, "failed.txt");
    assertThat(targetFile).doesNotExist();

    URL invalidUrl = new URL("file:///nonexistent/path/file.txt");

    assertThatThrownBy(() -> underTest.downloadFile(invalidUrl, targetFile))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Fail to download");

    assertThat(targetFile).doesNotExist();
  }

  @Test
  public void verifyDownloadUrl_whenUsingFileProtocol_shouldHandleExistenceCheck() throws Exception {
    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    File existingFile = temp.newFile("verify.txt");
    Files.writeString(existingFile.toPath(), "content");

    boolean resultExists = underTest.verifyDownloadUrl(existingFile.toURI().toURL());

    assertThat(resultExists).isTrue();

    existingFile.delete();
    boolean resultDeleted = underTest.verifyDownloadUrl(existingFile.toURI().toURL());

    assertThat(resultDeleted).isFalse();
  }

  // ===== HTTP Protocol Tests =====

  @Test
  public void downloadFile_whenHttpDownloadSucceeds_shouldDownloadFile() throws Exception {
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(200)
      .setBody("test file content"));

    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    URL httpUrl = new URL(mockWebServer.url("/file.txt").toString());
    File targetFile = new File(outputDir, "http-test.txt");

    File result = underTest.downloadFile(httpUrl, targetFile);

    assertThat(result)
      .exists()
      .isEqualTo(targetFile);
    assertThat(Files.readString(result.toPath())).isEqualTo("test file content");

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getPath()).isEqualTo("/file.txt");
  }

  @Test
  public void downloadFile_whenHttpReturns404_shouldThrowException() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(404));

    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    URL httpUrl = new URL(mockWebServer.url("/notfound.txt").toString());
    File targetFile = new File(outputDir, "not-found.txt");

    assertThatThrownBy(() -> underTest.downloadFile(httpUrl, targetFile))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Fail to download");

    assertThat(targetFile).doesNotExist();
  }

  @Test
  public void downloadFile_whenHttpReturns500_shouldThrowException() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    URL httpUrl = new URL(mockWebServer.url("/error.txt").toString());
    File targetFile = new File(outputDir, "server-error.txt");

    assertThatThrownBy(() -> underTest.downloadFile(httpUrl, targetFile))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Fail to download");

    assertThat(targetFile).doesNotExist();
  }

  @Test
  public void downloadFile_whenHttpWithBasicAuth_shouldAddAuthorizationHeader() throws Exception {
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(200)
      .setBody("authenticated content"));

    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    URL httpUrl = new URL("http://user:pass@" + mockWebServer.getHostName() + ":" + mockWebServer.getPort() + "/file.txt");
    File targetFile = new File(outputDir, "auth-test.txt");

    underTest.downloadFile(httpUrl, targetFile);

    assertThat(targetFile).exists();
    assertThat(Files.readString(targetFile.toPath())).isEqualTo("authenticated content");

    RecordedRequest request = mockWebServer.takeRequest();
    String authHeader = request.getHeader("Authorization");
    assertThat(authHeader)
      .isNotNull()
      .startsWith("Basic ");

    String decoded = new String(Base64.getDecoder().decode(authHeader.substring(6)));
    assertThat(decoded).isEqualTo("user:pass");
  }

  @Test
  public void verifyDownloadUrl_whenHttpHeadReturns200_shouldReturnTrue() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200));

    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    URL httpUrl = new URL(mockWebServer.url("/file.txt").toString());

    boolean result = underTest.verifyDownloadUrl(httpUrl);

    assertThat(result).isTrue();

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("HEAD");
  }

  @Test
  public void verifyDownloadUrl_whenHttpHeadFailsButGetSucceeds_shouldReturnTrue() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(405));
    mockWebServer.enqueue(new MockResponse().setResponseCode(200));

    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    URL httpUrl = new URL(mockWebServer.url("/file.txt").toString());

    boolean result = underTest.verifyDownloadUrl(httpUrl);

    assertThat(result).isTrue();

    RecordedRequest headRequest = mockWebServer.takeRequest();
    assertThat(headRequest.getMethod()).isEqualTo("HEAD");

    RecordedRequest getRequest = mockWebServer.takeRequest();
    assertThat(getRequest.getMethod()).isEqualTo("GET");
  }

  @Test
  public void verifyDownloadUrl_whenHttpHeadAndGetFail_shouldReturnFalse() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(405));
    mockWebServer.enqueue(new MockResponse().setResponseCode(404));

    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    URL httpUrl = new URL(mockWebServer.url("/notfound.txt").toString());

    boolean result = underTest.verifyDownloadUrl(httpUrl);

    assertThat(result).isFalse();

    mockWebServer.takeRequest(); // HEAD
    mockWebServer.takeRequest(); // GET
  }

  @Test
  public void download_whenHttpUrlAndNotCached_shouldDownloadViaHttp() throws Exception {
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(200)
      .setBody("jar file content"));

    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);

    File result = underTest.download(mockWebServer.url("/download-test.jar").toString(), false);

    assertThat(result)
      .exists()
      .hasName("download-test.jar");
    assertThat(Files.readString(result.toPath())).isEqualTo("jar file content");

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getPath()).isEqualTo("/download-test.jar");
  }

  @Test
  public void downloadFile_whenHttpRedirect_shouldFollowRedirect() throws Exception {
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(302)
      .setHeader("Location", mockWebServer.url("/redirected.txt").toString()));
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(200)
      .setBody("redirected content"));

    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    URL httpUrl = new URL(mockWebServer.url("/original.txt").toString());
    File targetFile = new File(outputDir, "redirect-test.txt");

    File result = underTest.downloadFile(httpUrl, targetFile);

    assertThat(result).exists();
    assertThat(Files.readString(result.toPath())).isEqualTo("redirected content");
  }

  @Test
  public void verifyDownloadUrl_whenHttpWithAuth_shouldSendAuthHeader() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200));

    HttpDownloader underTest = new HttpDownloader(outputDir, false, log);
    URL httpUrl = new URL("http://testuser:testpass@" + mockWebServer.getHostName() + ":" + mockWebServer.getPort() + "/secure.txt");

    boolean result = underTest.verifyDownloadUrl(httpUrl);

    assertThat(result).isTrue();

    RecordedRequest request = mockWebServer.takeRequest();
    String authHeader = request.getHeader("Authorization");
    assertThat(authHeader)
      .isNotNull()
      .startsWith("Basic ");
  }

  @Test
  public void downloadFile_whenInterrupted_shouldRestoreInterruptFlagAndCleanupFile() throws Exception {
    HttpClient mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenThrow(new InterruptedException("Download interrupted"));

    HttpDownloader underTest = new HttpDownloader(outputDir, false, log, mockHttpClient);
    URL httpUrl = new URL("http://example.com/file.txt");
    File targetFile = new File(outputDir, "interrupted.txt");

    assertThatThrownBy(() -> underTest.downloadFile(httpUrl, targetFile))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Fail to download")
      .hasCauseInstanceOf(InterruptedException.class);

    assertThat(Thread.interrupted()).isTrue();
    assertThat(targetFile).doesNotExist();
  }

  @Test
  public void verifyDownloadUrl_whenInterrupted_shouldRestoreInterruptFlagAndReturnFalse() throws Exception {
    HttpClient mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenThrow(new InterruptedException("Verify interrupted"));

    HttpDownloader underTest = new HttpDownloader(outputDir, false, log, mockHttpClient);
    URL httpUrl = new URL("http://example.com/file.txt");

    boolean result = underTest.verifyDownloadUrl(httpUrl);

    assertThat(result).isFalse();
    assertThat(Thread.interrupted()).isTrue();
  }
}
