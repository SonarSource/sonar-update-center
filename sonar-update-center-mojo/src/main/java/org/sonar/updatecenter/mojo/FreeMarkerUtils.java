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

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FreeMarkerUtils {

  private FreeMarkerUtils() {
  }

  public static void print(Map<String, Object> dataModel, File toFile, String templateName) {
    try (FileOutputStream fileOutputStream = new FileOutputStream(toFile);
      Writer writer = new OutputStreamWriter(fileOutputStream, UTF_8)) {
      freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);
      freemarker.template.Configuration cfg = new freemarker.template.Configuration();
      cfg.setClassForTemplateLoading(PluginModel.class, "");
      cfg.setObjectWrapper(new DefaultObjectWrapper());

      Template template = cfg.getTemplate(templateName);

      template.process(dataModel, writer);
      writer.flush();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to generate HTML to: " + toFile, e);
    }
  }

}
