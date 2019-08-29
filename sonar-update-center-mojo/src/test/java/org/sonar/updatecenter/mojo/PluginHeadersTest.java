/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2019 SonarSource SA
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
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginReferential;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PluginHeadersTest {

  private final static String PLUGIN_KEY = "key";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File outputFolder;

  private PluginReferential pluginReferential;

  private UpdateCenter center;

  private PluginHeaders pluginHeaders;

  private Sonar sonar;

  @Before
  public void before() throws Exception {
    outputFolder = temporaryFolder.newFolder();
    sonar = new Sonar();
    sonar.addRelease("3.0");
    sonar.addRelease("3.7");
    sonar.addRelease("3.7.1");
    sonar.addRelease("3.7.2");
    sonar.addRelease("3.7.4");
    sonar.addRelease("4.0");

    sonar.setLtsRelease("3.7.4");
  }

  private void prepareMocks(Plugin... plugins) throws IOException {
    pluginReferential = plugins.length > 0 ? PluginReferential.create(asList(plugins)) : PluginReferential.createEmpty();
    center = UpdateCenter.create(pluginReferential, sonar);
    pluginHeaders = new PluginHeaders(center, outputFolder, mock(Log.class));
  }

  @Test
  public void shouldReturnOnlyCssFileIfNoPlugin() throws Exception {
    prepareMocks();

    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(3);
    assertThat(outputFolder.list()).containsOnly("styles.css", "error.png", "onde-sonar-16.png");
  }

  @Test
  public void shouldGenerateHtml() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("http://valid.download.url");
    release.addRequiredSonarVersions("3.0");
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");
    plugin.setSourcesUrl("sources_url");
    plugin.setDevelopers(asList("dev"));
    plugin.setSupportedBySonarSource(true);

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    // 6 files:
    // - styles.css
    // - error.png
    // - onde-sonar-16.png
    // - compatibility-matrix.html
    // - PLUGIN_KEY-sonarsource-include.html
    assertThat(outputFolder.list()).hasSize(5);

    // since Freemarker transformation, confluence include data file are not easy to read
    // flatten the file to keep a easy to read reference file.
    File file = outputFolder.listFiles(new FilenameFilterForSonarSourceIncludeGeneratedHtml())[0];
    String flattenFile = flatHtmlFile(file);
    String flattenExpectedFile = flatHtmlFile(getExpectedFile("normal-sonarsource-include.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);
  }

  @Test
  public void shouldHaveNoDownloadLinkIfArchivedVersion() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);

    Version version10 = Version.create("1.0");
    Release release10 = new Release(plugin, version10);
    release10.setDate(new SimpleDateFormat("dd-MM-yyyy").parse("12-12-2013"));
    release10.setDownloadUrl("http://download.url");
    release10.addRequiredSonarVersions("3.0");
    release10.setDescription("Archived version");
    release10.setArchived(true);

    plugin.addRelease(release10);

    Version version11 = Version.create("1.1");
    Release release11 = new Release(plugin, version11);
    release11.setDate(new SimpleDateFormat("dd-MM-yyyy").parse("15-12-2015"));
    release11.setDownloadUrl("http://download.url");
    release11.addRequiredSonarVersions("3.0");
    release11.setDescription("Non-archived version");
    release11.setArchived(false);

    plugin.addRelease(release11);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");
    plugin.setSourcesUrl("sources_url");
    plugin.setDevelopers(asList("dev"));
    plugin.setOrganization("SonarSource");
    plugin.setOrganizationUrl("http://sonarsource.com");

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).containsOnly("key-sonarsource-include.html", "styles.css", "compatibility-matrix.html", "error.png",
      "onde-sonar-16.png");
  }

  class FilenameFilterForSonarSourceGeneratedHtml implements FilenameFilter {
    public boolean accept(File file, String s) {
      return (PLUGIN_KEY + "-sonarsource.html").equals(s);
    }
  }

  class FilenameFilterForSonarSourceIncludeGeneratedHtml implements FilenameFilter {
    public boolean accept(File file, String s) {
      return (PLUGIN_KEY + "-sonarsource-include.html").equals(s);
    }
  }

  @Test
  public void shouldGenerateHtmlWithTwoDevelopers() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("http://download.url");
    release.addRequiredSonarVersions("3.0");
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");
    plugin.setSourcesUrl("sources_url");
    plugin.setDevelopers(asList("dev1", "dev2"));

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(5);
  }

  @Test
  public void shouldWriteUnknownIfNoLicence() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("http://download.url");
    release.addRequiredSonarVersions("3.0");
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense(null);
    plugin.setSourcesUrl("sources_url");
    plugin.setDevelopers(asList("dev"));

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(5);
  }

  @Test
  public void shouldWriteUnknownIfNoIssueUrl() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("http://download.url");
    release.addRequiredSonarVersions("3.0");
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl(null);
    plugin.setLicense("licence");
    plugin.setSourcesUrl("sources_url");
    plugin.setDevelopers(asList("dev"));

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(5);
  }

  @Test
  public void shouldWriteAuthor() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("http://download.url");
    release.addRequiredSonarVersions("3.0");
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");
    plugin.setSourcesUrl("sources_url");
    plugin.setOrganization("SonarSource");
    plugin.setOrganizationUrl("http://sonarsource.com");

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(5);
  }

  @Test
  public void shouldWriteUnknownWhenNoSources() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("http://download.url");
    release.addRequiredSonarVersions("3.0");
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");
    plugin.setSourcesUrl(null);
    plugin.setDevelopers(asList("dev"));

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).containsOnly("key-sonarsource-include.html", "styles.css",
      "compatibility-matrix.html", "error.png", "onde-sonar-16.png");
  }

  @Test
  public void shouldProcessProperlyNoMoreCompatiblePlugin() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("http://download.url.org");

    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");
    plugin.setSourcesUrl("sources_url");
    plugin.setDevelopers(asList("dev"));

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).containsOnly("key-sonarsource-include.html", "styles.css",
      "compatibility-matrix.html", "error.png", "onde-sonar-16.png");
  }

  private File getExpectedFile(String fileName) {
    return FileUtils.toFile(getClass().getResource("/org/sonar/updatecenter/mojo/PluginHeadersTest/" + fileName));
  }

  private Date getDate() throws ParseException {
    return new SimpleDateFormat("dd-MM-yyyy").parse("12-12-2012");
  }

  /**
   * Suppress all spaces from an HTML file
   *
   * @return
   */
  private String flatHtmlFile(File file) throws IOException {
    return FileUtils.readFileToString(file, StandardCharsets.UTF_8).replaceAll("\\s", "");
  }

}
