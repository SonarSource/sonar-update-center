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

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(2);
    File file = outputFolder.listFiles(new FilenameFilterForGeneratedHtml())[0];
    assertThat(file).hasSameContentAs(getExpectedFile("normal.html"));
  }

  // UPC-20
  @Test
  public void shouldGenerateHtmlWithoutCompatibleWithLts() throws Exception {
    File file = generateWithLts(true);
    assertThat(file).hasSameContentAs(getExpectedFile("normal-without-lts.html"));
  }

  // UPC-20
  @Test
  public void shouldGenerateHtmlWithCompatibleWithLts() throws Exception {
    File file = generateWithLts(false);
    assertThat(file).hasSameContentAs(getExpectedFile("normal-with-lts.html"));
  }

  private File generateWithLts(boolean latestCompatibleWithLts) throws ParseException, IOException {
    sonar.setLtsRelease("3.0");
    Plugin plugin = new Plugin(PLUGIN_KEY);
    Version version1 = Version.create("1.0");
    Release release1 = new Release(plugin, version1);
    release1.setDate(getDate());
    release1.setDownloadUrl("download_url1");
    release1.addRequiredSonarVersions("3.0");
    plugin.addRelease(release1);
    Version version2 = Version.create("2.0");
    Release release2 = new Release(plugin, version2);
    release2.setDate(new SimpleDateFormat("dd-MM-yyyy").parse("12-12-2013"));
    release2.setDownloadUrl("download_url2");
    if (latestCompatibleWithLts) {
      release2.addRequiredSonarVersions("3.0", "4.0");
    }
    else {
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

    assertThat(outputFolder.list()).hasSize(2);
    File file = outputFolder.listFiles(new FilenameFilterForGeneratedHtml())[0];
    return file;
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

    prepareMocks(plugin);
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

    prepareMocks(plugin);
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

    prepareMocks(plugin);
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

    prepareMocks(plugin);
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

    prepareMocks(plugin);
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

    prepareMocks(plugin);
    pluginHeaders.generateHtml();

    assertThat(outputFolder.list()).hasSize(2);
  }

  private File getExpectedFile(String fileName) {
    return FileUtils.toFile(getClass().getResource("/org/sonar/updatecenter/mojo/PluginHeadersTest/" + fileName));
  }

  private Date getDate() throws ParseException {
    return new SimpleDateFormat("dd-MM-yyyy").parse("12-12-2012");
  }

}
