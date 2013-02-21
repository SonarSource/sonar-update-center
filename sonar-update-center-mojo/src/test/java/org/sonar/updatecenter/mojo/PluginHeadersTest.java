package org.sonar.updatecenter.mojo;

import org.apache.commons.io.FileUtils;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginReferential;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Version;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.Date;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PluginHeadersTest {

  private final static String PLUGIN_KEY = "key";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File outputFolder;

  @Before
  public void before() throws Exception {
    outputFolder = temporaryFolder.newFolder();
  }

  @Test
  public void shouldReturnOnlyCssFileIfNoPlugin() throws Exception {
    PluginReferential pluginReferential = PluginReferential.create(Collections.<Plugin>emptyList());
    PluginHeaders pluginHeaders = new PluginHeaders(pluginReferential, outputFolder, mock(Log.class));
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(1);
    assertThat(outputFolder.list()[0]).contains("style.css");
  }

  @Test
  public void shouldGenerateHtml() throws Exception {
    Plugin plugin = new Plugin(PLUGIN_KEY);
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

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(plugin));
    PluginHeaders pluginHeaders = new PluginHeaders(pluginReferential, outputFolder, mock(Log.class));
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(2);
    File file = outputFolder.listFiles(new FilenameFilterForGeneratedHtml())[0];
    assertThat(file).hasSameContentAs(getExpectedFile("normal.html"));
  }

  class FilenameFilterForGeneratedHtml implements FilenameFilter {
    public boolean accept(File file, String s) {
      return (PLUGIN_KEY + ".html").equals(s);
    }
  }

  @Test
  public void shouldGenerateHtmlWithTwoDevelopers() throws Exception {
    Plugin plugin = new Plugin(PLUGIN_KEY);
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

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(plugin));
    PluginHeaders pluginHeaders = new PluginHeaders(pluginReferential, outputFolder, mock(Log.class));
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(2);
    File file = outputFolder.listFiles(new FilenameFilterForGeneratedHtml())[0];
    assertThat(file).hasSameContentAs(getExpectedFile("normal-with-2-dev.html"));
  }

  @Test
  public void shouldWriteUnknownIfNoLicence() throws Exception {
    Plugin plugin = new Plugin(PLUGIN_KEY);
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

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(plugin));
    PluginHeaders pluginHeaders = new PluginHeaders(pluginReferential, outputFolder, mock(Log.class));
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(2);
    File file = outputFolder.listFiles(new FilenameFilterForGeneratedHtml())[0];
    assertThat(file).hasSameContentAs(getExpectedFile("without-licence.html"));
  }

  @Test
  public void shouldWriteUnknownIfNoIssueUrl() throws Exception {
    Plugin plugin = new Plugin(PLUGIN_KEY);
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

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(plugin));
    PluginHeaders pluginHeaders = new PluginHeaders(pluginReferential, outputFolder, mock(Log.class));
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(2);
    File file = outputFolder.listFiles(new FilenameFilterForGeneratedHtml())[0];
    assertThat(file).hasSameContentAs(getExpectedFile("without-issues-url.html"));
  }

  @Test
  public void shouldWriteUnknownWhenNoDeveloper() throws Exception {
    Plugin plugin = new Plugin(PLUGIN_KEY);
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
    plugin.setDevelopers(null);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(plugin));
    PluginHeaders pluginHeaders = new PluginHeaders(pluginReferential, outputFolder, mock(Log.class));
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(2);
    File file = outputFolder.listFiles(new FilenameFilterForGeneratedHtml())[0];
    assertThat(file).hasSameContentAs(getExpectedFile("without-developper.html"));
  }

  @Test
  public void shouldWriteUnknownWhenNoSources() throws Exception {
    Plugin plugin = new Plugin(PLUGIN_KEY);
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

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(plugin));
    PluginHeaders pluginHeaders = new PluginHeaders(pluginReferential, outputFolder, mock(Log.class));
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(2);
    File file = outputFolder.listFiles(new FilenameFilterForGeneratedHtml())[0];
    assertThat(file).hasSameContentAs(getExpectedFile("without-sources-url.html"));
  }

  @Test
  public void should_generate_html_for_child_plugin() throws Exception {
    Plugin plugin = new Plugin(PLUGIN_KEY);
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("download_url");
    release.addRequiredSonarVersions("3.0");
    release.setParent(new Release(new Plugin("parent"), "1.0"));
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");
    plugin.setSourcesUrl("sources_url");
    plugin.setDevelopers(newArrayList("dev"));

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(plugin));
    PluginHeaders pluginHeaders = new PluginHeaders(pluginReferential, outputFolder, mock(Log.class));
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(2);
  }

  private File getExpectedFile(String fileName) {
    return FileUtils.toFile(getClass().getResource("/org/sonar/updatecenter/mojo/PluginHeadersTest/" + fileName));
  }

  private Date getDate() throws DateParseException {
    return DateUtils.parseDate("12-12-2012", new String[]{"dd-MM-yyyy"});
  }

}
