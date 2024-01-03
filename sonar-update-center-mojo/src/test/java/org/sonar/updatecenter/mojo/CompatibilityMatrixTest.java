/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2024 SonarSource SA
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
import java.util.ArrayList;
import java.util.Date;
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

public class CompatibilityMatrixTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File outputFolder;

  private PluginReferential pluginReferential;

  private UpdateCenter center;

  private CompatibilityMatrix matrix;

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
    sonar.addRelease("10.0");

    sonar.setLtsRelease("3.7.4");
  }

  private void prepareMocks(Plugin... plugins) {
    pluginReferential = plugins.length > 0 ? PluginReferential.create(asList(plugins)) : PluginReferential.createEmpty();
    center = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar);
    matrix = new CompatibilityMatrix(center, outputFolder, mock(Log.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionIfNoOutputDir() throws IOException {
    File folder = new File("/doesnt/exist/");
    CompatibilityMatrix m = new CompatibilityMatrix(center, folder, mock(Log.class));
    m.generateHtml();
  }

  @Test
  public void shouldReturnOnlyCssFileIfNoPlugin() throws Exception {
    prepareMocks();
    matrix.generateHtml();
    assertThat(outputFolder.list()).hasSize(2);
    assertThat(outputFolder.list()).containsOnly("styles.css", "error.png");
  }

  @Test
  public void shouldGenerateHtml() throws Exception {
    Plugin pluginFoo = Plugin.factory("foo");
    Version versionFoo = Version.create("1.0");
    Release releaseFoo = new Release(pluginFoo, versionFoo);
    releaseFoo.setDate(getDate());
    releaseFoo.setDownloadUrl("http://valid.download.url");
    releaseFoo.addRequiredSonarVersions("3.0");
    pluginFoo.addRelease(releaseFoo);
    pluginFoo.setName("foo");

    Plugin pluginBar = Plugin.factory("bar");
    Version versionBar = Version.create("2.0");
    Release releaseBar = new Release(pluginBar, versionBar);
    releaseBar.setDate(getDate());
    releaseBar.setDownloadUrl("http://other.download.url");
    releaseBar.addRequiredSonarVersions("4.0");
    pluginBar.addRelease(releaseBar);
    pluginBar.setName("bar");

    Plugin pluginAbap = Plugin.factory("abap");
    Version versionAbap = Version.create("5.2");
    Release releaseAbap = new Release(pluginAbap, versionAbap);
    releaseAbap.setDate(getDate());
    releaseAbap.setDownloadUrl("http://abap.download.url");
    releaseAbap.addRequiredSonarVersions("3.0");
    releaseAbap.setArchived(true);
    pluginAbap.addRelease(releaseAbap);
    pluginAbap.setName("abap");

    prepareMocks(pluginFoo, pluginBar, pluginAbap);
    matrix.generateHtml();

    // 3 files:
    // - styles.css
    // - error.png
    // - compatibility-matrix.html
    assertThat(outputFolder.list()).hasSize(3);

    File file = outputFolder.listFiles(new FilenameFilterForCompatibilityMatrixGeneratedHtml())[0];
    String flattenFile = flatHtmlFile(file);
    String flattenExpectedFile = flatHtmlFile(getExpectedFile("compatibility-matrix.html"));
    assertThat(flattenFile).isEqualTo(flattenExpectedFile);
  }

  private File getExpectedFile(String fileName) {
    return FileUtils.toFile(getClass().getResource("/org/sonar/updatecenter/mojo/CompatibilityMatrixTest/" + fileName));
  }

  private Date getDate() throws ParseException {
    return new SimpleDateFormat("dd-MM-yyyy").parse("12-12-2012");
  }

  private String flatHtmlFile(File file) throws IOException {
    return FileUtils.readFileToString(file, StandardCharsets.UTF_8).replaceAll("\\s", "");
  }

  class FilenameFilterForCompatibilityMatrixGeneratedHtml implements FilenameFilter {
    public boolean accept(File file, String s) {
      return "compatibility-matrix.html".equals(s);
    }
  }

}
