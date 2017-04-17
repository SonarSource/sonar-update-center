/*
 * SonarSource :: Update Center :: Common
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
package org.sonar.updatecenter.common;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Properties;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class UpdateCenterSerializerTest {

  @Test
  public void test_to_properties() throws IOException, URISyntaxException {
    Sonar sonar = new Sonar();
    sonar.addRelease("2.0").setDisplayVersion("2.0");
    sonar.addRelease("2.1").setDisplayVersion("2.1 (build 42)");
    sonar.setLtsRelease("2.0");

    Plugin foo = Plugin.factory("foo")
      .setName("Foo")
      .setOrganizationUrl("http://www.sonarsource.org");
    foo.addRelease(
      new Release(foo, Version.create("1.2"))
        .addRequiredSonarVersions(Version.create("2.0"))
        .addRequiredSonarVersions(Version.create("2.1"))
        .setDisplayVersion("1.2 (build 42)")
      );

    Plugin bar = Plugin.factory("bar")
      .setSourcesUrl("scm:svn:https://svn.codehaus.org/sonar-plugins/bar-plugin-1.2")
      .setDevelopers(Arrays.asList("dev1", "dev2"));
    bar.addRelease(
      new Release(bar, Version.create("1.2"))
        .addRequiredSonarVersions(Version.create("2.0"))
        .addRequiredSonarVersions(Version.create("2.1"))
        .setDisplayVersion("1.2")
      );
    PluginReferential pluginReferential = PluginReferential.create(Arrays.asList(foo, bar));

    UpdateCenter center = UpdateCenter.create(pluginReferential, sonar);
    Properties properties = UpdateCenterSerializer.toProperties(center);
    properties.store(System.out, null);

    assertProperty(properties, "sonar.versions", "2.0,2.1");
    assertProperty(properties, "publicVersions", "2.0,2.1");
    assertProperty(properties, "2.0.displayVersion", "2.0");
    assertProperty(properties, "2.1.displayVersion", "2.1 (build 42)");
    assertProperty(properties, "ltsVersion", "2.0");
    assertProperty(properties, "plugins", "bar,foo");
    assertProperty(properties, "foo.name", "Foo");
    assertProperty(properties, "foo.organizationUrl", "http://www.sonarsource.org");
    assertProperty(properties, "foo.1.2.requiredSonarVersions", "2.0,2.1");
    assertProperty(properties, "foo.1.2.displayVersion", "1.2 (build 42)");
    assertProperty(properties, "bar.versions", "1.2");
    assertProperty(properties, "bar.publicVersions", "1.2");
    assertProperty(properties, "bar.scm", "scm:svn:https://svn.codehaus.org/sonar-plugins/bar-plugin-1.2");
    assertProperty(properties, "bar.developers", "dev1,dev2");
    assertProperty(properties, "bar.1.2.requiredSonarVersions", "2.0,2.1");
    assertProperty(properties, "bar.1.2.sqVersions", "2.0,2.1");
    assertProperty(properties, "bar.1.2.displayVersion", "1.2");
  }

  @Test
  public void should_return_required_releases() throws IOException {
    Sonar sonar = new Sonar();
    sonar.addRelease(Version.create("2.0"));
    sonar.addRelease(Version.create("2.1"));

    Plugin foo = Plugin.factory("foo");
    Release foo12 = new Release(foo, "1.2").addRequiredSonarVersions("2.0").addRequiredSonarVersions("2.1");
    foo.addRelease(foo12);

    Plugin test = Plugin.factory("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions("2.1");
    test.addRelease(test10);

    Plugin bar = Plugin.factory("bar");
    Release bar12 = new Release(bar, "1.2").addRequiredSonarVersions("2.0").addRequiredSonarVersions("2.1");
    bar.addRelease(bar12);

    PluginReferential pluginReferential = PluginReferential.create(Arrays.asList(foo, bar, test));
    pluginReferential.addOutgoingDependency(bar12, "foo", "1.2");
    pluginReferential.addOutgoingDependency(bar12, "test", "1.0");

    UpdateCenter center = UpdateCenter.create(pluginReferential, sonar);
    Properties properties = UpdateCenterSerializer.toProperties(center);
    properties.store(System.out, null);

    assertProperty(properties, "plugins", "bar,foo,test");
    assertProperty(properties, "foo.1.2.requiredSonarVersions", "2.0,2.1");
    assertProperty(properties, "test.1.0.requiredSonarVersions", "2.1");
    assertProperty(properties, "bar.1.2.requiredSonarVersions", "2.0,2.1");
    String[] requirePlugins = properties.getProperty("bar.1.2.requirePlugins").split(",");
    assertThat(requirePlugins).containsOnly("foo:1.2", "test:1.0");
  }

  private void assertProperty(Properties props, String key, String value) {
    assertThat(props.getProperty(key)).isEqualTo(value);
  }
}
