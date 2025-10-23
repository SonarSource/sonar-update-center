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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.headRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class HttpDownloaderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

  private File outputDir;
  private Log log;
  private HttpDownloader underTest;
  private String testBasicAuth;

  @Before
  public void setUp() throws IOException {
    outputDir = temp.newFolder();
    log = mock(Log.class);
    underTest = new HttpDownloader(outputDir, false, log);
    testBasicAuth = "Basic " + Base64.getEncoder().encodeToString("test-user:test-pass".getBytes(StandardCharsets.UTF_8));
  }

  /*
   ========================================
   Tests for file:// protocol
   These test the file copy code path
   and do NOT exercise the HTTP client logic
   ========================================
  */

  @Test
  public void download_whenUsingFileProtocol_shouldDownloadSuccessfully() throws IOException {
    // Create a source file
    File sourceFile = temp.newFile("source.txt");
    Files.write(sourceFile.toPath(), "test content".getBytes());

    // Download it
    File downloaded = underTest.download(sourceFile.toURI().toString(), false);

    assertThat(downloaded).exists();
    assertThat(Files.readAllLines(downloaded.toPath())).containsExactly("test content");
  }

  @Test
  public void download_whenFileAlreadyCached_shouldUseCachedVersion() throws IOException {
    // Create a source file
    File sourceFile = temp.newFile("cached.txt");
    Files.write(sourceFile.toPath(), "cached content".getBytes());

    // First download
    File firstDownload = underTest.download(sourceFile.toURI().toString(), false);
    long firstSize = firstDownload.length();

    // Modify source file - if cache works, downloaded file won't change
    Files.write(sourceFile.toPath(), "modified source content".getBytes());

    // Second download should use cache (ignore modified source)
    File secondDownload = underTest.download(sourceFile.toURI().toString(), false);

    assertThat(secondDownload)
      .isEqualTo(firstDownload)
      .hasSize(firstSize);
    assertThat(Files.readAllLines(secondDownload.toPath())).containsExactly("cached content");
  }

  @Test
  public void download_whenForcedAndCached_shouldRedownloadFile() throws IOException {
    // Create a source file
    File sourceFile = temp.newFile("forced.txt");
    Files.write(sourceFile.toPath(), "original content".getBytes());

    // First download
    File firstDownload = underTest.download(sourceFile.toURI().toString(), false);
    assertThat(Files.readAllLines(firstDownload.toPath())).containsExactly("original content");

    // Update source file
    Files.write(sourceFile.toPath(), "updated content".getBytes());

    // Force download should get new content
    File secondDownload = underTest.download(sourceFile.toURI().toString(), true);

    assertThat(secondDownload).isEqualTo(firstDownload);
    assertThat(Files.readAllLines(secondDownload.toPath())).containsExactly("updated content");
  }

  @Test
  public void verifyDownloadUrl_whenFileExists_shouldReturnTrue() throws IOException {
    File sourceFile = temp.newFile("exists.txt");
    Files.write(sourceFile.toPath(), "test".getBytes());

    boolean result = underTest.verifyDownloadUrl(sourceFile.toURI().toURL());

    assertThat(result).isTrue();
  }

  @Test
  public void verifyDownloadUrl_whenFileDoesNotExist_shouldReturnFalse() throws IOException {
    File nonExistent = new File(outputDir, "does-not-exist.txt");

    boolean result = underTest.verifyDownloadUrl(nonExistent.toURI().toURL());

    assertThat(result).isFalse();
  }

  @Test
  public void download_whenFilenameHasSpecialCharacters_shouldDownloadSuccessfully() throws IOException {
    File sourceFile = temp.newFile("file with spaces.txt");
    Files.write(sourceFile.toPath(), "content".getBytes());

    File downloaded = underTest.download(sourceFile.toURI().toString(), false);

    assertThat(downloaded).exists();
    assertThat(downloaded.getName()).contains("spaces");
  }

  @Test
  public void downloadFile_whenSourceFileNotFound_shouldThrowException() throws Exception {
    File nonExistent = new File(outputDir, "missing.txt");
    URL sourceUrl = nonExistent.toURI().toURL();
    File outputFile = new File(outputDir, "output.txt");

    assertThatThrownBy(() -> underTest.downloadFile(sourceUrl, outputFile))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Fail to download");
  }

  @Test
  public void download_whenOutputDirectoryDoesNotExist_shouldCreateDirectory() throws IOException {
    File newOutputDir = new File(temp.getRoot(), "nested/output/dir");
    HttpDownloader newDownloader = new HttpDownloader(newOutputDir, false, log);

    File sourceFile = temp.newFile("source.txt");
    Files.write(sourceFile.toPath(), "test".getBytes());

    newDownloader.download(sourceFile.toURI().toString(), false);

    assertThat(newOutputDir).exists().isDirectory();
  }

  @Test
  public void download_whenCalledWithUrl_shouldExtractFilenameFromUrl() throws IOException {
    File sourceFile = temp.newFile("original-name.jar");
    Files.write(sourceFile.toPath(), "jar content".getBytes());

    File downloaded = underTest.download(sourceFile.toURI().toString(), false);

    assertThat(downloaded).hasName("original-name.jar");
  }

  @Test
  public void download_whenFileIsEmpty_shouldRedownloadOnNextCall() throws IOException {
    File emptyFile = temp.newFile("empty.txt");

    File downloaded = underTest.download(emptyFile.toURI().toString(), false);
    assertThat(downloaded).exists();

    // Second call should re-download because file length is 0
    Files.delete(downloaded.toPath());
    File redownloaded = underTest.download(emptyFile.toURI().toString(), false);
    assertThat(redownloaded).exists();
  }

  @Test
  public void download_whenVerificationEnabledAndCached_shouldVerifyUrl() throws IOException {
    File sourceFile = temp.newFile("verify.txt");
    Files.write(sourceFile.toPath(), "content".getBytes());

    // Create downloader with URL verification enabled
    HttpDownloader verifyingDownloader = new HttpDownloader(outputDir, true, log);

    // First download
    File downloaded = verifyingDownloader.download(sourceFile.toURI().toString(), false);
    assertThat(downloaded).exists();

    // Second download with verification
    File cached = verifyingDownloader.download(sourceFile.toURI().toString(), false);
    assertThat(cached).exists();
  }

  @Test
  public void download_whenCachedUrlNoLongerValid_shouldThrowException() throws IOException {
    File sourceFile = temp.newFile("will-be-deleted.txt");
    Files.write(sourceFile.toPath(), "content".getBytes());

    // Create downloader with URL verification enabled
    HttpDownloader verifyingDownloader = new HttpDownloader(outputDir, true, log);

    // First download
    String sourceUrl = sourceFile.toURI().toString();
    File downloaded = verifyingDownloader.download(sourceUrl, false);
    assertThat(downloaded).exists();

    // Delete source file
    Files.delete(sourceFile.toPath());

    // Second download should fail because URL verification fails
    assertThatThrownBy(() -> verifyingDownloader.download(sourceUrl, false))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("URL is no longer valid");
  }

  /*
   ========================================
   Tests for http:// and https:// protocols
   These test the Apache HttpClient code path
   using WireMock to simulate real HTTP server responses
   ========================================
  */

  @Test
  public void download_whenUsingHttp_shouldDownloadSuccessfully() throws Exception {
    String content = "http content";
    stubFor(get(urlEqualTo("/test.txt"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(content)));

    String url = "http://localhost:" + wireMockRule.port() + "/test.txt";
    File downloaded = underTest.download(url, false);

    assertThat(downloaded).exists();
    assertThat(new String(Files.readAllBytes(downloaded.toPath()))).isEqualTo(content);
    verify(getRequestedFor(urlEqualTo("/test.txt")));
  }

  @Test
  public void download_whenUsingHttpWithAuth_shouldSendAuthorizationHeader() throws Exception {
    String content = "protected content";
    stubFor(get(urlEqualTo("/protected.txt"))
      .withHeader("Authorization", equalTo(testBasicAuth))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(content)));

    String url = "http://test-user:test-pass@localhost:" + wireMockRule.port() + "/protected.txt";
    File downloaded = underTest.download(url, false);

    assertThat(downloaded).exists();
    assertThat(new String(Files.readAllBytes(downloaded.toPath()))).isEqualTo(content);
    verify(getRequestedFor(urlEqualTo("/protected.txt"))
      .withHeader("Authorization", equalTo(testBasicAuth)));
  }

  @Test
  public void download_whenHttpReturnsError_shouldThrowException() {
    stubFor(get(urlEqualTo("/error.txt"))
      .willReturn(aResponse()
        .withStatus(404)
        .withStatusMessage("Not Found")));

    String url = "http://localhost:" + wireMockRule.port() + "/error.txt";

    assertThatThrownBy(() -> underTest.download(url, false))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Fail to download");
  }

  @Test
  public void download_whenHttpRedirects_shouldFollowRedirect() throws Exception {
    String content = "redirected content";
    stubFor(get(urlEqualTo("/redirect"))
      .willReturn(aResponse()
        .withStatus(302)
        .withHeader("Location", "/final")));

    stubFor(get(urlEqualTo("/final"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(content)));

    String url = "http://localhost:" + wireMockRule.port() + "/redirect";
    File downloaded = underTest.download(url, false);

    assertThat(downloaded).exists();
    assertThat(new String(Files.readAllBytes(downloaded.toPath()))).isEqualTo(content);
  }

  @Test
  public void verifyDownloadUrl_whenHttpUrlExists_shouldUseHeadRequest() throws Exception {
    stubFor(head(urlEqualTo("/exists.txt"))
      .willReturn(aResponse()
        .withStatus(200)));

    String url = "http://localhost:" + wireMockRule.port() + "/exists.txt";
    boolean result = underTest.verifyDownloadUrl(new URL(url));

    assertThat(result).isTrue();
    verify(headRequestedFor(urlEqualTo("/exists.txt")));
  }

  @Test
  public void verifyDownloadUrl_whenHeadRequestFails_shouldFallbackToGet() throws Exception {
    stubFor(head(urlEqualTo("/no-head.txt"))
      .willReturn(aResponse()
        .withStatus(405)));

    stubFor(get(urlEqualTo("/no-head.txt"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody("content")));

    String url = "http://localhost:" + wireMockRule.port() + "/no-head.txt";
    boolean result = underTest.verifyDownloadUrl(new URL(url));

    assertThat(result).isTrue();
    verify(headRequestedFor(urlEqualTo("/no-head.txt")));
    verify(getRequestedFor(urlEqualTo("/no-head.txt")));
  }

  @Test
  public void verifyDownloadUrl_whenHttpUrlNotFound_shouldReturnFalse() throws Exception {
    stubFor(head(urlEqualTo("/missing.txt"))
      .willReturn(aResponse()
        .withStatus(404)));

    stubFor(get(urlEqualTo("/missing.txt"))
      .willReturn(aResponse()
        .withStatus(404)));

    String url = "http://localhost:" + wireMockRule.port() + "/missing.txt";
    boolean result = underTest.verifyDownloadUrl(new URL(url));

    assertThat(result).isFalse();
  }

  @Test
  public void verifyDownloadUrl_whenHttpUrlRequiresAuth_shouldSendAuthorizationHeader() throws Exception {
    stubFor(head(urlEqualTo("/protected.txt"))
      .withHeader("Authorization", equalTo(testBasicAuth))
      .willReturn(aResponse()
        .withStatus(200)));

    String url = "http://test-user:test-pass@localhost:" + wireMockRule.port() + "/protected.txt";
    boolean result = underTest.verifyDownloadUrl(new URL(url));

    assertThat(result).isTrue();
    verify(headRequestedFor(urlEqualTo("/protected.txt"))
      .withHeader("Authorization", equalTo(testBasicAuth)));
  }
}
