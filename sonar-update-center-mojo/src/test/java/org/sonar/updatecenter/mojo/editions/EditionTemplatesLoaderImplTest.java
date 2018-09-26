/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2018 SonarSource SA
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
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class EditionTemplatesLoaderImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test_loading_of_editions() throws Exception {
    Properties props = new Properties();
    props.setProperty("editions", "community,enterprise");
    props.setProperty("community.name", "Community");
    props.setProperty("community.textDescription", "Community Edition");
    props.setProperty("community.homeUrl", "/home1");
    props.setProperty("community.requestLicenseUrl", "/request1");
    props.setProperty("community.plugins", "support");
    props.setProperty("enterprise.name", "Enterprise");
    props.setProperty("enterprise.textDescription", "Enterprise Edition");
    props.setProperty("enterprise.homeUrl", "/home2");
    props.setProperty("enterprise.requestLicenseUrl", "/request2");
    props.setProperty("enterprise.plugins", "cobol,governance");

    List<EditionTemplate> templates = test(props);

    assertThat(templates).hasSize(2);
    // order is defined by the property "editions"
    EditionTemplate community = templates.get(0);
    assertThat(community.getKey()).isEqualTo("community");
    assertThat(community.getName()).isEqualTo("Community");
    assertThat(community.getTextDescription()).isEqualTo("Community Edition");
    assertThat(community.getHomeUrl()).isEqualTo("/home1");
    assertThat(community.getRequestUrl()).isEqualTo("/request1");
    assertThat(community.getPluginKeys()).containsExactly("support");
    EditionTemplate enterprise = templates.get(1);
    assertThat(enterprise.getKey()).isEqualTo("enterprise");
    assertThat(enterprise.getName()).isEqualTo("Enterprise");
    assertThat(enterprise.getTextDescription()).isEqualTo("Enterprise Edition");
    assertThat(enterprise.getHomeUrl()).isEqualTo("/home2");
    assertThat(enterprise.getRequestUrl()).isEqualTo("/request2");
    assertThat(enterprise.getPluginKeys()).containsExactly("cobol", "governance");
  }

  @Test
  public void fail_if_missing_property_editions() throws Exception {
    Properties props = new Properties();
    props.setProperty("editions", "community");
    props.setProperty("community.name", "Community");

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Property [community.textDescription] is missing");

    test(props);
  }

  @Test
  public void fail_if_missing_property_in_an_edition() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Property [editions] is missing");

    test(new Properties());
  }

  private List<EditionTemplate> test(Properties properties) throws Exception {
    File propFile = temp.newFile();
    try (Writer writer = new OutputStreamWriter(new FileOutputStream(propFile), UTF_8)) {
      properties.store(writer, "");
    }
    EditionTemplatesLoaderImpl underTest = new EditionTemplatesLoaderImpl(propFile);
    return underTest.load();
  }
}
