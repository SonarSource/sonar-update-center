/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
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
    pluginReferential = plugins.length > 0 ? PluginReferential.create(Arrays.asList(plugins)) : PluginReferential.createEmpty();
    center = UpdateCenter.create(pluginReferential, sonar);
    pluginHeaders = new PluginHeaders(center, outputFolder, mock(Log.class));
  }

  @Test
  public void shouldReturnOnlyCssFileIfNoPlugin() throws Exception {
    prepareMocks();

    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(3);
    assertThat(outputFolder.list()).containsOnly("style-confluence.css", "error.png", "onde-sonar-16.png");
  }

  @Test
  public void shouldGenerateHtml() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("download_url");
    release.addRequiredSonarVersions("3.0");
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");
    plugin.setSourcesUrl("sources_url");
    plugin.setDevelopers(newArrayList("dev"));
    plugin.setSupportedBySonarSource(true);

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    // 7 files:
    // - style-confluence.css
    // - error.png
    // - onde-sonar-16.png
    // - compatibility-matrix.html
    // - PLUGIN_KEY-confluence-include.html
    // - PLUGIN_KEY-sonarsource.html
    // - PLUGIN_KEY-sonarsource-include.html
    assertThat(outputFolder.list()).hasSize(7);

    // since Freemarker transformation, confluence include data file are not easy to read
    // flatten the file to keep a easy to read reference file.
    File file = outputFolder.listFiles(new FilenameFilterForConfluenceIncludeGeneratedHtml())[0];
    String flattenFile = flatHtmlFile(file);
    String flattenExpectedFile = flatHtmlFile(getExpectedFile("normal-include.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);

    file = outputFolder.listFiles(new FilenameFilterForSonarSourceGeneratedHtml())[0];
    flattenFile = flatHtmlFile(file);
    flattenExpectedFile = flatHtmlFile(getExpectedFile("normal-sonarsource.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);

    file = outputFolder.listFiles(new FilenameFilterForSonarSourceIncludeGeneratedHtml())[0];
    flattenFile = flatHtmlFile(file);
    flattenExpectedFile = flatHtmlFile(getExpectedFile("normal-sonarsource-include.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);
  }

  // UPC-20
  @Test
  public void shouldGenerateHtml_latest_plugin_version_compatible_with_lts() throws Exception {
    Map<String, File> files = generateWithLts(true, true);

    String flattenFile = flatHtmlFile(files.get("confluenceIncludeHtml"));
    String flattenExpectedFile = flatHtmlFile(getExpectedFile("latest_plugin_version_compatible_with_lts-include.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);
  }

  @Test
  public void shouldGenerateHtml_old_plugin_version_compatible_with_lts() throws Exception {
    Map<String, File> files = generateWithLts(false, true);

    String flattenFile = flatHtmlFile(files.get("confluenceIncludeHtml"));
    String flattenExpectedFile = flatHtmlFile(getExpectedFile("old_plugin_version_compatible_with_lts-include.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);
  }

  // UPC-20
  @Test
  public void shouldGenerateHtml_no_plugin_version_compatible_with_lts() throws Exception {

    Map<String, File> files = generateWithLts(false, false);

    String flattenFile = flatHtmlFile(files.get("confluenceIncludeHtml"));
    String flattenExpectedFile = flatHtmlFile(getExpectedFile("no_plugin_version_compatible_with_lts-include.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);
  }

  private Map<String, File> generateWithLts(boolean latestCompatibleWithLts, boolean atLeastOneCompatibleWithLts) throws ParseException, IOException {
    sonar.setLtsRelease("3.0");
    Plugin plugin = Plugin.factory(PLUGIN_KEY);
    if (atLeastOneCompatibleWithLts) {
      Version version1 = Version.create("1.0");
      Release release1 = new Release(plugin, version1);
      release1.setDate(getDate());
      release1.setDownloadUrl("download_url1");
      release1.addRequiredSonarVersions("3.0");
      plugin.addRelease(release1);
    }
    Version version2 = Version.create("2.0");
    Release release2 = new Release(plugin, version2);
    release2.setDate(new SimpleDateFormat("dd-MM-yyyy").parse("12-12-2013"));
    release2.setDownloadUrl("download_url2");
    if (latestCompatibleWithLts) {
      release2.addRequiredSonarVersions("3.0", "4.0");
    } else {
      release2.addRequiredSonarVersions("4.0");
    }
    plugin.addRelease(release2);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");
    plugin.setSourcesUrl("sources_url");
    plugin.setDevelopers(newArrayList("dev"));

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(7);
    Map<String, File> returned = new HashMap<>(2);
    returned.put("confluenceIncludeHtml", outputFolder.listFiles(new FilenameFilterForConfluenceIncludeGeneratedHtml())[0]);

    return returned;
  }

  @Test
  public void shouldHaveNoDownloadLinkIfArchivedVersion() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);

    Version version10 = Version.create("1.0");
    Release release10 = new Release(plugin, version10);
    release10.setDate(new SimpleDateFormat("dd-MM-yyyy").parse("12-12-2013"));
    release10.setDownloadUrl("download_url");
    release10.addRequiredSonarVersions("3.0");
    release10.setDescription("Archived version");
    release10.setArchived(true);

    plugin.addRelease(release10);

    Version version11 = Version.create("1.1");
    Release release11 = new Release(plugin, version11);
    release11.setDate(new SimpleDateFormat("dd-MM-yyyy").parse("15-12-2015"));
    release11.setDownloadUrl("download_url");
    release11.addRequiredSonarVersions("3.0");
    release11.setDescription("Non-archived version");
    release11.setArchived(false);

    plugin.addRelease(release11);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");
    plugin.setSourcesUrl("sources_url");
    plugin.setDevelopers(newArrayList("dev"));
    plugin.setOrganization("SonarSource");
    plugin.setOrganizationUrl("http://sonarsource.com");

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).containsOnly("key-sonarsource.html", "key-sonarsource-include.html", "key-confluence-include.html", "style-confluence.css", "compatibility-matrix.html", "error.png", "onde-sonar-16.png");

    File file = outputFolder.listFiles(new FilenameFilterForConfluenceIncludeGeneratedHtml())[0];
    String flattenFile = flatHtmlFile(file);
    String flattenExpectedFile = flatHtmlFile(getExpectedFile("archived-version-have-no-download-link-include.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);
  }

  class FilenameFilterForConfluenceIncludeGeneratedHtml implements FilenameFilter {
    public boolean accept(File file, String s) {
      return (PLUGIN_KEY + "-confluence-include.html").equals(s);
    }
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
    release.setDownloadUrl("download_url");
    release.addRequiredSonarVersions("3.0");
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");
    plugin.setSourcesUrl("sources_url");
    plugin.setDevelopers(newArrayList("dev1", "dev2"));

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(7);

    File file = outputFolder.listFiles(new FilenameFilterForConfluenceIncludeGeneratedHtml())[0];
    String flattenFile = flatHtmlFile(file);
    String flattenExpectedFile = flatHtmlFile(getExpectedFile("normal-with-2-dev-include.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);
  }

  @Test
  public void shouldWriteUnknownIfNoLicence() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("download_url");
    release.addRequiredSonarVersions("3.0");
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense(null);
    plugin.setSourcesUrl("sources_url");
    plugin.setDevelopers(newArrayList("dev"));

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(7);

    File file = outputFolder.listFiles(new FilenameFilterForConfluenceIncludeGeneratedHtml())[0];
    String flattenFile = flatHtmlFile(file);
    String flattenExpectedFile = flatHtmlFile(getExpectedFile("without-licence-include.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);
  }

  @Test
  public void shouldWriteUnknownIfNoIssueUrl() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("download_url");
    release.addRequiredSonarVersions("3.0");
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl(null);
    plugin.setLicense("licence");
    plugin.setSourcesUrl("sources_url");
    plugin.setDevelopers(newArrayList("dev"));

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(7);

    File file = outputFolder.listFiles(new FilenameFilterForConfluenceIncludeGeneratedHtml())[0];
    String flattenFile = flatHtmlFile(file);
    String flattenExpectedFile = flatHtmlFile(getExpectedFile("without-issues-url-include.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);
  }

  @Test
  public void shouldWriteAuthor() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("download_url");
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

    assertThat(outputFolder.list()).hasSize(7);

    File file = outputFolder.listFiles(new FilenameFilterForConfluenceIncludeGeneratedHtml())[0];
    String flattenFile = flatHtmlFile(file);
    String flattenExpectedFile = flatHtmlFile(getExpectedFile("with-author-include.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);
  }

  @Test
  public void shouldWriteUnknownWhenNoSources() throws Exception {
    Plugin plugin = Plugin.factory(PLUGIN_KEY);
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("download_url");
    release.addRequiredSonarVersions("3.0");
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");
    plugin.setSourcesUrl(null);
    plugin.setDevelopers(newArrayList("dev"));

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).containsOnly("key-sonarsource.html", "key-sonarsource-include.html", "key-confluence-include.html", "style-confluence.css",
      "compatibility-matrix.html", "error.png", "onde-sonar-16.png");

    File file = outputFolder.listFiles(new FilenameFilterForConfluenceIncludeGeneratedHtml())[0];
    String flattenFile = flatHtmlFile(file);
    String flattenExpectedFile = flatHtmlFile(getExpectedFile("without-sources-url-include.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);
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
    return FileUtils.readFileToString(file).replaceAll("\\s", "");
  }

}
