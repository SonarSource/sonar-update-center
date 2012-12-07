package org.sonar.updatecenter.server;

import org.apache.commons.io.FileUtils;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import java.io.File;
import java.util.Collections;
import java.util.Date;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class PluginsHtmlHeaderTest {

  private PluginsHtmlHeader pluginsHtmlHeader;

  private UpdateCenter center;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File outputFolder;

  @Before
  public void before() throws Exception {
    center = new UpdateCenter();
    outputFolder = temporaryFolder.newFolder();
    pluginsHtmlHeader = new PluginsHtmlHeader(center, outputFolder);
  }

  @Test
  public void shouldReturnOnlyCssFileIfNoPlugin() throws Exception {
    pluginsHtmlHeader.start();

    assertThat(outputFolder.list()).hasSize(1);
    assertThat(outputFolder.list()[0]).contains("style.css");
  }

  @Test
  public void shouldGenerateHtmlForOnePlugin() throws Exception {
    Plugin plugin = new Plugin("key");
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("download_url");
    release.setSourcesUrl("sources_url");
    release.addRequiredSonarVersions("3.0");
    release.setDevelopers(newArrayList("dev"));
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");

    center.setPlugins(newArrayList(plugin));

    pluginsHtmlHeader.start();

    assertThat(outputFolder.list()).hasSize(2);
    assertThat(outputFolder.list()[0]).contains("key.html");
    File file = outputFolder.listFiles()[0];
    assertThat(file).hasSameContentAs(getExpectedFile("normal.html"));
  }

  @Test
  public void shouldWriteUnknownIfNoLicence() throws Exception {
    Plugin plugin = new Plugin("key");
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("download_url");
    release.setSourcesUrl("sources_url");
    release.addRequiredSonarVersions("3.0");
    release.setDevelopers(newArrayList("dev"));
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense(null);

    center.setPlugins(newArrayList(plugin));

    pluginsHtmlHeader.start();

    assertThat(outputFolder.list()).hasSize(2);
    assertThat(outputFolder.list()[0]).contains("key.html");
    File file = outputFolder.listFiles()[0];
    assertThat(file).hasSameContentAs(getExpectedFile("without-licence.html"));
  }

  @Test
  public void shouldWriteUnknownIfNoIssueUrl() throws Exception {
    Plugin plugin = new Plugin("key");
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("download_url");
    release.setSourcesUrl("sources_url");
    release.addRequiredSonarVersions("3.0");
    release.setDevelopers(newArrayList("dev"));
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl(null);
    plugin.setLicense("licence");

    center.setPlugins(newArrayList(plugin));

    pluginsHtmlHeader.start();

    assertThat(outputFolder.list()).hasSize(2);
    assertThat(outputFolder.list()[0]).contains("key.html");
    File file = outputFolder.listFiles()[0];
    assertThat(file).hasSameContentAs(getExpectedFile("without-issues-url.html"));
  }

  @Test
  public void shouldWriteUnknownWhenNoDeveloper() throws Exception {
    Plugin plugin = new Plugin("key");
    Version version = Version.create("1.0");
    Release release = new Release(plugin, version);
    release.setDate(getDate());
    release.setDownloadUrl("download_url");
    release.setSourcesUrl("sources_url");
    release.addRequiredSonarVersions("3.0");
    release.setDevelopers(Collections.<String>emptyList());
    plugin.addRelease(release);
    plugin.setName("name");
    plugin.setIssueTrackerUrl("issue_url");
    plugin.setLicense("licence");

    center.setPlugins(newArrayList(plugin));

    pluginsHtmlHeader.start();

    assertThat(outputFolder.list()).hasSize(2);
    assertThat(outputFolder.list()[0]).contains("key.html");
    File file = outputFolder.listFiles()[0];
    assertThat(file).hasSameContentAs(getExpectedFile("without-developper.html"));
  }

  private File getExpectedFile(String fileName){
    return FileUtils.toFile(getClass().getResource("/"+ fileName));
  }

  private Date getDate() throws DateParseException {
    return DateUtils.parseDate("12-12-2012", new String[]{"dd-MM-yyyy"});
  }

}
