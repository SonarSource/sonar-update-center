/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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
import org.sonar.updatecenter.common.Product;
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
    addReleaseToSonarObject("3.0", sonar);
    addReleaseToSonarObject("3.0", sonar);
    addReleaseToSonarObject("3.7", sonar);
    addReleaseToSonarObject("3.7.1", sonar);
    addReleaseToSonarObject("3.7.2", sonar);
    addReleaseToSonarObject("3.7.4", sonar);
    addReleaseToSonarObject("4.0", sonar);
    addReleaseToSonarObject("10.0", sonar);

    addReleaseToSonarObject("24.12", sonar, Product.SONARQUBE_COMMUNITY_BUILD);
    addReleaseToSonarObject("25.1", sonar, Product.SONARQUBE_COMMUNITY_BUILD);

    addReleaseToSonarObject("2025.1", sonar, Product.SONARQUBE_SERVER);
    addReleaseToSonarObject("2025.2", sonar, Product.SONARQUBE_SERVER);

    sonar.setLtsRelease("3.7.4");
    sonar.setLtaVersion("3.7.4");
    sonar.setPastLtaVersion("2.9.10");
  }

  private void addReleaseToSonarObject(String version, Sonar sonar) {
    addReleaseToSonarObject(version, sonar, Product.OLD_SONARQUBE);
  }

  private void addReleaseToSonarObject(String version, Sonar sonar, Product product) {
    Release release = new Release(sonar, Version.create(version));
    release.setProduct(product);
    sonar.addRelease(release);
  }

  private void prepareMocks(Plugin... plugins) {
    pluginReferential = plugins.length > 0 ? PluginReferential.create(asList(plugins)) : PluginReferential.createEmpty();
    center = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, Product.OLD_SONARQUBE);
    matrix = new CompatibilityMatrix(center, outputFolder, mock(Log.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionIfNoOutputDir() throws IOException {
    File folder = new File("/doesnt/exist/");
    CompatibilityMatrix m = new CompatibilityMatrix(center, folder, mock(Log.class));
    m.generateHtmls();
  }

  @Test
  public void shouldReturnOnlyCssFileIfNoPlugin() throws Exception {
    prepareMocks();
    matrix.generateHtmls();
    assertThat(outputFolder.list()).hasSize(2);
    assertThat(outputFolder.list()).containsOnly("styles.css", "error.png");
  }

  @Test
  public void shouldGenerate3Htmls() throws Exception {
    Plugin pluginFoo = Plugin.factory("foo");
    Version versionFoo = Version.create("1.0");
    Release releaseFoo = new Release(pluginFoo, versionFoo);
    releaseFoo.setDate(getDate());
    releaseFoo.setDownloadUrl("http://valid.download.url");
    releaseFoo.addRequiredSonarVersions(Product.OLD_SONARQUBE, "3.0");
    releaseFoo.addRequiredSonarVersions(Product.SONARQUBE_SERVER, "2025.1");
    releaseFoo.addRequiredSonarVersions(Product.SONARQUBE_COMMUNITY_BUILD, "24.12");
    pluginFoo.addRelease(releaseFoo);
    pluginFoo.setName("foo");

    Plugin pluginBar = Plugin.factory("bar");
    Version versionBar = Version.create("2.0");
    Release releaseBar = new Release(pluginBar, versionBar);
    releaseBar.setDate(getDate());
    releaseBar.setDownloadUrl("http://other.download.url");
    releaseBar.addRequiredSonarVersions(Product.OLD_SONARQUBE, "4.0");
    releaseBar.addRequiredSonarVersions(Product.SONARQUBE_COMMUNITY_BUILD, "25.1");
    releaseBar.addRequiredSonarVersions(Product.SONARQUBE_SERVER, "2025.2");
    pluginBar.addRelease(releaseBar);
    pluginBar.setName("bar");

    Plugin pluginAbap = Plugin.factory("abap");
    Version versionAbap = Version.create("5.2");
    Release releaseAbap = new Release(pluginAbap, versionAbap);
    releaseAbap.setDate(getDate());
    releaseAbap.setDownloadUrl("http://abap.download.url");
    releaseAbap.addRequiredSonarVersions(Product.OLD_SONARQUBE, "3.0");
    releaseAbap.setArchived(true);
    pluginAbap.addRelease(releaseAbap);
    pluginAbap.setName("abap");

    prepareMocks(pluginFoo, pluginBar, pluginAbap);
    matrix.generateHtmls();

    assertThat(outputFolder.list()).hasSize(5);
    assertHtmlForProduct(Product.OLD_SONARQUBE, "compatibility-matrix.html");
    assertHtmlForProduct(Product.SONARQUBE_COMMUNITY_BUILD, "compatibility-matrix-sqcb.html");
    assertHtmlForProduct(Product.SONARQUBE_SERVER, "compatibility-matrix-sqs.html");
  }

  private void assertHtmlForProduct(Product product, String fileName) throws IOException {
    File file = outputFolder.listFiles(new FilenameFilterForCompatibilityMatrixGeneratedHtml(product))[0];
    String flattenFile = flatHtmlFile(file);
    String flattenExpectedFile = flatHtmlFile(getExpectedFile(fileName));
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

    private final String filename;

    public FilenameFilterForCompatibilityMatrixGeneratedHtml(Product product) {
      switch (product) {
        case OLD_SONARQUBE: this.filename = "compatibility-matrix.html"; break;
        case SONARQUBE_SERVER: this.filename = "compatibility-matrix-sqs.html"; break;
        case SONARQUBE_COMMUNITY_BUILD: this.filename = "compatibility-matrix-sqcb.html"; break;
        default: throw new IllegalArgumentException("Unsupported product: " + product);
      }

    }
    public boolean accept(File file, String s) {
      return filename.equals(s);
    }
  }

}
