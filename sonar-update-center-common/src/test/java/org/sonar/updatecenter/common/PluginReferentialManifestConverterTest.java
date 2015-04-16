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

import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class PluginReferentialManifestConverterTest {

  @Test
  public void should_return_plugins(){
    PluginManifest foo = new PluginManifest().setKey("foo").setVersion("1.0");
    PluginManifest bar = new PluginManifest().setKey("bar").setVersion("1.1");

    PluginReferential pluginReferential = PluginReferentialManifestConverter.fromPluginManifests(newArrayList(foo, bar));

    assertThat(pluginReferential.getLastMasterReleasePlugins()).hasSize(2);
  }
}
