/*
 * SonarSource :: Update Center :: Maven Plugin
 * Copyright (C) 2010-2022 SonarSource SA
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
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;

public class EditionTemplatesLoaderImpl implements EditionTemplatesLoader {

  private final File propertiesFile;

  public EditionTemplatesLoaderImpl(File propertiesFile) {
    this.propertiesFile = propertiesFile;
  }

  @Override
  public List<EditionTemplate> load() throws IOException {
    Properties props = new Properties();
    try (Reader input = new InputStreamReader(new FileInputStream(propertiesFile), UTF_8)) {
      props.load(input);
    }
    return load(props);
  }

  List<EditionTemplate> load(Properties props) {
    List<EditionTemplate> templates = new ArrayList<>();
    stream(StringUtils.split(valueOf(props, "editions"), ","))
      .forEach(editionKey -> templates.add(toTemplate(props, editionKey)));
    return templates;
  }

  private static EditionTemplate toTemplate(Properties props, String editionKey) {
    return new EditionTemplate.Builder()
      .setKey(editionKey)
      .setName(valueOf(props, editionKey + ".name"))
      .setTextDescription(valueOf(props, editionKey + ".textDescription"))
      .setHomeUrl(valueOf(props, editionKey + ".homeUrl"))
      .setRequestLicenseUrl(valueOf(props, editionKey + ".requestLicenseUrl"))
      .setPluginKeys(Arrays.asList(StringUtils.split(valueOf(props, editionKey + ".plugins"), ",")))
      .build();
  }

  private static String valueOf(Properties props, String key) {
    return Objects.requireNonNull(props.getProperty(key), () -> "Property [" + key + "] is missing");
  }
}
