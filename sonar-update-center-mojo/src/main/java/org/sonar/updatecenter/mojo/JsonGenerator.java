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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.sonar.updatecenter.common.Component;
import org.sonar.updatecenter.common.UpdateCenter;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class JsonGenerator {

  public interface Factory<T> {
    T create(
      String resourceFile,
      Gson gson,
      Schema jsonSchema);
  }

  protected final String resourceFile;
  protected final File outputDirectory;
  protected final UpdateCenter center;
  protected final Log log;
  protected final Gson gson;
  protected final Schema jsonSchema;

  protected JsonGenerator(
    String resourceFile,
    UpdateCenter center,
    File outputDirectory,
    Log log,
    Gson gson,
    Schema jsonSchema) {
    this.resourceFile = resourceFile;
    this.outputDirectory = outputDirectory;
    this.center = center;
    this.log = log;
    this.gson = gson;
    this.jsonSchema = jsonSchema;
  }

  public static <T> T create(String jsonSchemaResource, Factory<T> factory) {
    Gson jsonGenerator = new GsonBuilder()
      .disableHtmlEscaping()
      .setPrettyPrinting()
      .create();

    Schema jsonSchema;
    try (InputStream inputStream = JsonGenerator.class.getResourceAsStream("/" + jsonSchemaResource)) {
      JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
      jsonSchema = SchemaLoader.load(rawSchema);
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }

    return factory.create(
      jsonSchemaResource,
      jsonGenerator,
      jsonSchema);
  }


  protected void serializeToFile(Component component, Object from) throws IOException {
    String jsonOutputString = gson.toJson(from);

    try {
      checkComplianceWithSchema(jsonOutputString);
    } catch (ValidationException exception) {
      log.error(component.getKey() + " json not compliant with schema");
      throw exception;
    }

    File file = new File(outputDirectory, component.getKey() + ".json");
    log.info("Generate json data for component " + component.getKey() + " in: " + file);

    FileUtils.writeStringToFile(file, jsonOutputString, UTF_8);

    // copy the schema
    FileUtils.copyURLToFile(
      PluginsJsonGenerator.class.getResource("/" + resourceFile),
      new File(outputDirectory, resourceFile));

  }


  protected void checkComplianceWithSchema(String inputJson) {
    this.jsonSchema.validate(new JSONObject(inputJson));
  }

  @CheckForNull
  protected static URL safeCreateURLFromString(@Nullable String mayBeAnURL) {
    if (mayBeAnURL == null) {
      return null;
    }
    try {
      return new URL(mayBeAnURL);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }

}
