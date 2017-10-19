/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2017 SonarSource SA
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
package org.sonar.updatecenter.mojo.editions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class EditionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void build_edition() throws Exception {
    File zip = temp.newFile();
    Edition.Builder builder = new Edition.Builder();
    builder.setKey("enterprise");
    builder.setName("Enterprise");
    builder.setTextDescription("Enterprise Edition");
    builder.setSonarQubeVersion("6.7.1");
    builder.setHomeUrl("/home");
    builder.setRequestUrl("/request");
    builder.setTargetZip(zip);
    builder.addJar(newJar("foo.jar"));
    builder.addJar(newJar("bar.jar"));

    Edition edition = builder.build();

    assertThat(edition.getKey()).isEqualTo("enterprise");
    assertThat(edition.getName()).isEqualTo("Enterprise");
    assertThat(edition.getTextDescription()).isEqualTo("Enterprise Edition");
    assertThat(edition.getHomeUrl()).isEqualTo("/home");
    assertThat(edition.getRequestUrl()).isEqualTo("/request");
    assertThat(edition.getSonarQubeVersion()).isEqualTo("6.7.1");
    assertThat(edition.getZip()).exists().isFile();
    assertThat(FileUtils.sizeOf(edition.getZip())).isGreaterThan(0);
    assertThat(edition.getZip().getAbsolutePath()).isEqualTo(zip.getAbsolutePath());

    try (ZipInputStream zipInput = new ZipInputStream(new FileInputStream(edition.getZip()))) {
      assertThat(zipInput.getNextEntry().getName()).isEqualTo("foo.jar");
      assertThat(zipInput.getNextEntry().getName()).isEqualTo("bar.jar");
      assertThat(zipInput.getNextEntry()).isNull();
    }
  }

  @Test
  public void fail_if_adding_jar_that_does_not_exist() throws Exception {
    File jar = temp.newFile();
    jar.delete();
    Edition.Builder underTest = new Edition.Builder();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("File does not exist: " + jar.getAbsolutePath());

    underTest.addJar(jar);
  }

  private File newJar(String filename) throws IOException {
    File file = temp.newFile(filename);
    FileUtils.write(file, RandomStringUtils.randomAlphabetic(50));
    return file;
  }

}
