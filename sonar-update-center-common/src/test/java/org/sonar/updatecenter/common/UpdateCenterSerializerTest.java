/*
 * SonarSource :: Update Center :: Common
 * Copyright (C) 2010-2025 SonarSource Sàrl
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateCenterSerializerTest {

  @Test
  public void toProperties_when108and20251released_includeThemInPublicVersions() {
    Sonar sonar = new Sonar();
    addReleaseToSonarObject("10.8", sonar, Product.SONARQUBE_SERVER);
    addReleaseToSonarObject("2025.1", sonar, Product.SONARQUBE_SERVER);

    PluginReferential pluginReferential = PluginReferential.create(new ArrayList<>());

    UpdateCenter center = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, null);
    Properties properties = UpdateCenterSerializer.toProperties(center);

    assertProperty(properties, "sonar.versions", "10.8,2025.1");
    assertProperty(properties, "publicVersions", "10.8,2025.1");
    assertProperty(properties, "sqs", "10.8,2025.1");
  }

  @Test
  public void test_to_properties() throws IOException {
    Sonar sonar = new Sonar();
    Release release = new Release(sonar, Version.create("2.0"));
    Release release21 = new Release(sonar, Version.create("2.1"));
    release21.setProduct(Product.OLD_SONARQUBE);
    release.setProduct(Product.OLD_SONARQUBE);
    sonar.addRelease(release).setDisplayVersion("2.0");
    sonar
      .addRelease(release21)
      .setDisplayVersion("2.1 (build 42)")
      .setDownloadUrl("http://dist.sonar.codehaus.org/sonar-2.1.zip")
      .setDownloadUrl("http://dist.sonar.codehaus.org/sonar-developer-2.1.zip", Release.Edition.DEVELOPER)
      .setDownloadUrl("http://dist.sonar.codehaus.org/sonar-enterprise-2.1.zip", Release.Edition.ENTERPRISE)
      .setDownloadUrl("http://dist.sonar.codehaus.org/sonar-datacenter-2.1.zip", Release.Edition.DATACENTER);

    Release release20391 = new Release(sonar, Version.create("2039.1"));
    sonar.addRelease(release20391)
        .setDisplayVersion("2039.1")
        .setDownloadUrl("http://dist.sonar.codehaus.org/sonar-developer-2039.1.zip", Release.Edition.DEVELOPER)
        .setDownloadUrl("http://dist.sonar.codehaus.org/sonar-enterprise-2039.1.zip", Release.Edition.ENTERPRISE)
        .setDownloadUrl("http://dist.sonar.codehaus.org/sonar-datacenter-2039.1.zip", Release.Edition.DATACENTER)
        .setProduct(Product.SONARQUBE_SERVER);

    Release release391 = new Release(sonar, Version.create("39.1"));
    sonar.addRelease(release391)
      .setDisplayVersion("39.1")
      .setDownloadUrl("http://dist.sonar.codehaus.org/sonar-developer-39.1.zip")
      .setProduct(Product.SONARQUBE_COMMUNITY_BUILD);

    sonar.setLtaVersion("2.0");
    sonar.setLtaVersion("2.0");
    sonar.setPastLtaVersion("1.0");

    Plugin foo = Plugin.factory("foo");

    foo.setName("Foo")
      .setOrganizationUrl("http://www.sonarsource.org");
    foo.addRelease(
      new Release(foo, Version.create("1.2"))
        .addRequiredSonarVersions(Product.OLD_SONARQUBE, Version.create("2.0"))
        .addRequiredSonarVersions(Product.OLD_SONARQUBE, Version.create("2.1"))
        .setDisplayVersion("1.2 (build 42)")
      );

    Plugin bar = Plugin.factory("bar");
    bar.setSourcesUrl("scm:svn:https://svn.codehaus.org/sonar-plugins/bar-plugin-1.2")
      .setDevelopers(Arrays.asList("dev1", "dev2"));
    bar.addRelease(
      new Release(bar, Version.create("1.2"))
        .addRequiredSonarVersions(Product.OLD_SONARQUBE, Version.create("2.0"))
        .addRequiredSonarVersions(Product.OLD_SONARQUBE, Version.create("2.1"))
        .addRequiredSonarVersions(Product.SONARQUBE_SERVER, Version.create("2039.1"))
        .addRequiredSonarVersions(Product.SONARQUBE_COMMUNITY_BUILD, Version.create("39.1"))
        .setDisplayVersion("1.2")
      );
    PluginReferential pluginReferential = PluginReferential.create(Arrays.asList(foo, bar));

    UpdateCenter center = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, null);
    Properties properties = UpdateCenterSerializer.toProperties(center);
    properties.store(System.out, null);

    assertProperty(properties, "sonar.versions", "2.0,2.1,39.1,2039.1");
    assertProperty(properties, "publicVersions", "2.0,2.1");
    assertProperty(properties, "sqcb", "39.1");
    assertProperty(properties, "sqs", "2039.1");
    assertProperty(properties, "2.0.displayVersion", "2.0");
    assertProperty(properties, "2.1.displayVersion", "2.1 (build 42)");
    assertProperty(properties, "2.1.downloadUrl", "http://dist.sonar.codehaus.org/sonar-2.1.zip");
    assertProperty(properties, "2.1.downloadDeveloperUrl", "http://dist.sonar.codehaus.org/sonar-developer-2.1.zip");
    assertProperty(properties, "2.1.downloadEnterpriseUrl", "http://dist.sonar.codehaus.org/sonar-enterprise-2.1.zip");
    assertProperty(properties, "2.1.downloadDatacenterUrl", "http://dist.sonar.codehaus.org/sonar-datacenter-2.1.zip");
    assertProperty(properties, "2039.1.displayVersion", "2039.1");
    assertProperty(properties, "2039.1.downloadDeveloperUrl", "http://dist.sonar.codehaus.org/sonar-developer-2039.1.zip");
    assertProperty(properties, "2039.1.downloadEnterpriseUrl", "http://dist.sonar.codehaus.org/sonar-enterprise-2039.1.zip");
    assertProperty(properties, "2039.1.downloadDatacenterUrl", "http://dist.sonar.codehaus.org/sonar-datacenter-2039.1.zip");
    assertProperty(properties, "39.1.displayVersion", "39.1");
    assertProperty(properties, "39.1.downloadUrl", "http://dist.sonar.codehaus.org/sonar-developer-39.1.zip");
    assertProperty(properties, "ltsVersion", "2.0");
    assertProperty(properties, "ltaVersion", "2.0");
    assertProperty(properties, "pastLtaVersion", "1.0");
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
    assertProperty(properties, "bar.1.2.sqcb", "39.1");
    assertProperty(properties, "bar.1.2.sqs", "2039.1");
    assertProperty(properties, "bar.1.2.displayVersion", "1.2");
  }

  @Test
  public void should_return_required_releases() throws IOException {
    Sonar sonar = new Sonar();
    addReleaseToSonarObject("2.0", sonar);
    addReleaseToSonarObject("2.1", sonar);

    Plugin foo = Plugin.factory("foo");
    Release foo12 = new Release(foo, "1.2").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.0")
      .addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1")
      .addRequiredSonarVersions(Product.SONARQUBE_SERVER, "2039.1")
      .addRequiredSonarVersions(Product.SONARQUBE_COMMUNITY_BUILD, "22.5");
    foo.addRelease(foo12);

    Plugin test = Plugin.factory("test");
    Release test10 = new Release(test, "1.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1");
    test.addRelease(test10);

    Plugin bar = Plugin.factory("bar");
    Release bar12 = new Release(bar, "1.2").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.0").addRequiredSonarVersions(Product.OLD_SONARQUBE, "2.1");
    bar.addRelease(bar12);

    PluginReferential pluginReferential = PluginReferential.create(Arrays.asList(foo, bar, test));
    pluginReferential.addOutgoingDependency(bar12, "foo", "1.2");
    pluginReferential.addOutgoingDependency(bar12, "test", "1.0");

    UpdateCenter center = UpdateCenter.create(pluginReferential, new ArrayList<>(), sonar, null);
    Properties properties = UpdateCenterSerializer.toProperties(center);
    properties.store(System.out, null);

    assertProperty(properties, "plugins", "bar,foo,test");
    assertProperty(properties, "foo.1.2.requiredSonarVersions", "2.0,2.1");
    assertProperty(properties, "foo.1.2.sqs", "2039.1");
    assertProperty(properties, "foo.1.2.sqcb", "22.5");
    assertProperty(properties, "test.1.0.requiredSonarVersions", "2.1");
    assertProperty(properties, "bar.1.2.requiredSonarVersions", "2.0,2.1");
    String[] requirePlugins = properties.getProperty("bar.1.2.requirePlugins").split(",");
    assertThat(requirePlugins).containsOnly("foo:1.2", "test:1.0");
  }

  private void assertProperty(Properties props, String key, String value) {
    assertThat(props.getProperty(key)).isEqualTo(value);
  }

  private void addReleaseToSonarObject(String version, Sonar sonar) {
    addReleaseToSonarObject(version, sonar, Product.OLD_SONARQUBE);
  }

  private void addReleaseToSonarObject(String version, Sonar sonar, Product product) {
    Release release = new Release(sonar, Version.create(version));
    release.setProduct(product);
    sonar.addRelease(release);
  }
}
