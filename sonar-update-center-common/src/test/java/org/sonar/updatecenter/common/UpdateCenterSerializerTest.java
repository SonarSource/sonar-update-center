/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.updatecenter.common;

import com.google.common.base.Splitter;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Properties;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class UpdateCenterSerializerTest {

  @Test
  public void test_to_properties() throws IOException, URISyntaxException {
    Sonar sonar = new Sonar();
    sonar.addRelease("2.0");
    sonar.addRelease("2.1");
    sonar.setLtsRelease("2.0");

    Plugin foo = new Plugin("foo")
      .setName("Foo")
      .setOrganizationUrl("http://www.sonarsource.org");
    foo.addRelease(
      new Release(foo, Version.create("1.2"))
        .addRequiredSonarVersions(Version.create("2.0"))
        .addRequiredSonarVersions(Version.create("2.1"))
      );

    Plugin bar = new Plugin("bar")
      .setSourcesUrl("scm:svn:https://svn.codehaus.org/sonar-plugins/bar-plugin-1.2")
      .setDevelopers(Arrays.asList("dev1", "dev2"));
    bar.addRelease(
      new Release(bar, Version.create("1.2"))
        .addRequiredSonarVersions(Version.create("2.0"))
        .addRequiredSonarVersions(Version.create("2.1"))
      );
    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, bar));

    UpdateCenter center = UpdateCenter.create(pluginReferential, sonar);
    Properties properties = UpdateCenterSerializer.toProperties(center);
    properties.store(System.out, null);

    assertProperty(properties, "sonar.versions", "2.0,2.1");
    assertProperty(properties, "publicVersions", "2.0,2.1");
    assertProperty(properties, "ltsVersion", "2.0");
    assertProperty(properties, "plugins", "bar,foo");
    assertProperty(properties, "foo.name", "Foo");
    assertProperty(properties, "foo.organizationUrl", "http://www.sonarsource.org");
    assertProperty(properties, "foo.1.2.requiredSonarVersions", "2.0,2.1");
    assertProperty(properties, "bar.versions", "1.2");
    assertProperty(properties, "bar.publicVersions", "1.2");
    assertProperty(properties, "bar.scm", "scm:svn:https://svn.codehaus.org/sonar-plugins/bar-plugin-1.2");
    assertProperty(properties, "bar.developers", "dev1,dev2");
    assertProperty(properties, "bar.1.2.requiredSonarVersions", "2.0,2.1");
    assertProperty(properties, "bar.1.2.sqVersions", "2.0,2.1");
  }

  @Test
  public void should_return_required_releases() throws IOException {
    Sonar sonar = new Sonar();
    sonar.addRelease(Version.create("2.0"));
    sonar.addRelease(Version.create("2.1"));

    Plugin foo = new Plugin("foo");
    Release foo12 = new Release(foo, "1.2").addRequiredSonarVersions("2.0").addRequiredSonarVersions("2.1");
    foo.addRelease(foo12);

    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1");
    test.addRelease(test10);

    Plugin bar = new Plugin("bar");
    Release bar12 = new Release(bar, "1.2").addRequiredSonarVersions("2.0").addRequiredSonarVersions("2.1");
    bar.addRelease(bar12);

    PluginReferential pluginReferential = PluginReferential.create(newArrayList(foo, bar, test));
    pluginReferential.addOutgoingDependency(bar12, "foo", "1.2");
    pluginReferential.addOutgoingDependency(bar12, "test", "1.0");

    UpdateCenter center = UpdateCenter.create(pluginReferential, sonar);
    Properties properties = UpdateCenterSerializer.toProperties(center);
    properties.store(System.out, null);

    assertProperty(properties, "plugins", "bar,foo,test");
    assertProperty(properties, "foo.1.2.requiredSonarVersions", "2.0,2.1");
    assertProperty(properties, "test.1.0.requiredSonarVersions", "2.1");
    assertProperty(properties, "bar.1.2.requiredSonarVersions", "2.0,2.1");
    Iterable requirePlugins = Splitter.on(",").split(properties.getProperty("bar.1.2.requirePlugins"));
    assertThat(requirePlugins).containsOnly("foo:1.2", "test:1.0");
  }

  private void assertProperty(Properties props, String key, String value) {
    assertThat(props.getProperty(key)).isEqualTo(value);
  }
}
