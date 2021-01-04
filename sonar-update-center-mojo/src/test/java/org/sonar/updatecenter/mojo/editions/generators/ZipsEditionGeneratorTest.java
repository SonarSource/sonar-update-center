/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2021 SonarSource SA
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
package org.sonar.updatecenter.mojo.editions.generators;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.updatecenter.mojo.editions.Edition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ZipsEditionGeneratorTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File jarDir;
  private File outputDir;
  private ZipsEditionGenerator generator;

  @Before
  public void setUp() throws IOException {
    jarDir = temp.newFolder("jars");
    outputDir = temp.newFolder("output");
    generator = new ZipsEditionGenerator(jarDir);
  }

  @Test
  public void generate_zip_files_of_editions() throws Exception {
    List<Edition> editions = Collections.singletonList(createEdition("dev", "file1.jar", "file2.jar"));

    generator.generate(outputDir, editions);
    File expectedZip = new File(outputDir, "dev.zip");
    assertThat(expectedZip).exists();

    assertThatZipContainsExactly(expectedZip, "file1.jar", "file2.jar");
  }

  @Test
  public void dont_generate_zip_files_of_empty_editions() throws Exception {
    List<Edition> editions = Collections.singletonList(createEdition("dev"));

    generator.generate(outputDir, editions);
    File expectedZip = new File(outputDir, "dev.zip");
    assertThat(expectedZip).doesNotExist();
  }

  @Test
  public void fail_if_adding_jar_that_does_not_exist() throws Exception {
    List<Edition> editions = Collections.singletonList(createEdition("dev", "file1.jar", "file2.jar"));
    File file = new File(jarDir, "file1.jar");
    file.delete();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("File does not exist: " + file.getAbsolutePath());

    generator.generate(outputDir, editions);
  }

  @Test
  public void fail_if_adding_jar_that_is_a_directory() throws Exception {
    List<Edition> editions = Collections.singletonList(createEdition("dev", "file1.jar", "file2.jar"));
    File file = new File(jarDir, "file1.jar");
    file.delete();
    file.mkdir();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("File does not exist: ");

    generator.generate(outputDir, editions);
  }

  @Test
  public void fail_if_jars_dir_does_not_exist() throws Exception {
    jarDir = new File(temp.getRoot(), "doesntExist");
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Directory does not exist: " + jarDir);

    generator = new ZipsEditionGenerator(jarDir);
  }

  private Edition createEdition(String name, String... jars) throws IOException {
    Edition edition = mock(Edition.class);
    when(edition.getKey()).thenReturn(name);
    when(edition.jars()).thenReturn(Sets.newLinkedHashSet(jars));
    when(edition.getZipFileName()).thenReturn(jars.length > 0 ? (name + ".zip") : null);

    for (String jar : jars) {
      createJarFile(jar);
    }
    return edition;
  }

  private File createJarFile(String name) throws IOException {
    File jarFile = new File(jarDir, name);
    FileUtils.write(jarFile, "content of file", StandardCharsets.UTF_8);
    return jarFile;
  }

  private void assertThatZipContainsExactly(File zip, String... filenames) throws Exception {
    try (ZipInputStream zipInput = new ZipInputStream(new FileInputStream(zip))) {
      for (String filename : filenames) {
        assertThat(zipInput.getNextEntry().getName()).isEqualTo(filename);
      }
      assertThat(zipInput.getNextEntry()).isNull();
    }
  }

}
