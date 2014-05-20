/*
 * SonarSource :: Update Center :: Packaging Mojo
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
package org.sonar.updatecenter.mavenplugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for Sonar-plugin-packaging related tasks.
 * 
 * @author Evgeny Mandrikov
 */
public abstract class AbstractSonarPluginMojo extends AbstractMojo {

  public static final String SONAR_GROUPID = "org.codehaus.sonar";
  public static final String SONAR_PLUGIN_API_ARTIFACTID = "sonar-plugin-api";
  public static final String SONAR_PLUGIN_API_TYPE = "jar";

  /**
   * The Maven project.
   */
  @Component
  private MavenProject project;

  /**
   *  Maven Session
   */
  @Component
  private MavenSession session;

  /**
   * Directory containing the generated JAR.
   */
  @Parameter(property = "project.build.directory", required = true)
  private File outputDirectory;

  /**
   * Directory containing the classes and resource files that should be packaged into the JAR.
   */
  @Parameter(property = "project.build.outputDirectory", required = true)
  private File classesDirectory;

  /**
  * The directory where the app is built.
  */
  @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}", required = true)
  private File appDirectory;

  /**
   * Name of the generated JAR.
   */
  @Parameter(alias = "jarName", property = "project.build.finalName", required = true)
  private String finalName;

  /**
   * Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.
   */
  @Parameter
  private String classifier;

  @Component
  protected MavenProjectHelper projectHelper;

  /**
   * Plugin key.
   */
  @Parameter(property = "sonar.pluginKey")
  protected String pluginKey;

  @Parameter(property = "sonar.pluginTermsConditionsUrl")
  private String pluginTermsConditionsUrl;

  /**
   * Name of plugin class.
   */
  @Parameter(property = "sonar.pluginClass", required = true)
  private String pluginClass;

  @Parameter(property = "sonar.pluginName", required = true, defaultValue = "${project.name}")
  private String pluginName;

  /**
   * Plugin parent.
   */
  @Parameter(property = "sonar.pluginParent")
  protected String pluginParent;

  /**
   * Plugin's dependencies.
   */
  @Parameter(property = "sonar.requirePlugins")
  protected String requirePlugins;

  @Parameter(property = "sonar.pluginDescription", defaultValue = "${project.description}")
  private String pluginDescription;

  @Parameter(property = "sonar.pluginUrl", defaultValue = "${project.url}")
  private String pluginUrl;

  @Parameter(defaultValue = "${project.issueManagement.url}")
  private String pluginIssueTrackerUrl;

  /**
   * @since 0.3
   */
  @Parameter
  private boolean useChildFirstClassLoader = false;

  /**
   * @since 1.1
   */
  @Parameter
  private String basePlugin;

  @Parameter(property = "sonar.skipDependenciesPackaging")
  private boolean skipDependenciesPackaging = false;

  protected final MavenProject getProject() {
    return project;
  }

  protected final MavenSession getSession() {
    return session;
  }

  protected final File getOutputDirectory() {
    return outputDirectory;
  }

  /**
   * @return the main classes directory, so it's used as the root of the jar.
   */
  protected final File getClassesDirectory() {
    return classesDirectory;
  }

  public File getAppDirectory() {
    return appDirectory;
  }

  protected final String getFinalName() {
    return finalName;
  }

  protected final String getClassifier() {
    return classifier;
  }

  public String getExplicitPluginKey() {
    return pluginKey;
  }

  protected final String getPluginClass() {
    return pluginClass;
  }

  protected final String getPluginName() {
    return pluginName;
  }

  protected final String getPluginParent() {
    return pluginParent;
  }

  protected final String getRequirePlugins() {
    return requirePlugins;
  }

  protected final String getPluginDescription() {
    return pluginDescription;
  }

  protected final String getPluginUrl() {
    return pluginUrl;
  }

  protected String getPluginTermsConditionsUrl() {
    return pluginTermsConditionsUrl;
  }

  protected String getPluginIssueTrackerUrl() {
    return pluginIssueTrackerUrl;
  }

  public boolean isUseChildFirstClassLoader() {
    return useChildFirstClassLoader;
  }

  public String getBasePlugin() {
    return basePlugin;
  }

  protected boolean isSkipDependenciesPackaging() {
    return skipDependenciesPackaging;
  }

  @SuppressWarnings({"unchecked"})
  protected Set<Artifact> getDependencyArtifacts() {
    return getProject().getDependencyArtifacts();
  }

  protected Set<Artifact> getDependencyArtifacts(String scope) {
    Set<Artifact> result = new HashSet<Artifact>();
    for (Artifact dep : getDependencyArtifacts()) {
      if (scope.equals(dep.getScope())) {
        result.add(dep);
      }
    }
    return result;
  }

  @SuppressWarnings({"unchecked"})
  protected Set<Artifact> getIncludedArtifacts() {
    Set<Artifact> result = new HashSet<Artifact>();
    Set<Artifact> artifacts = getProject().getArtifacts();
    ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
    for (Artifact artifact : artifacts) {
      if (filter.include(artifact)) {
        result.add(artifact);
      }
    }
    return result;
  }

  protected final Artifact getSonarPluginApiArtifact() {
    Set<Artifact> dependencies = getDependencyArtifacts();
    if (dependencies != null) {
      for (Artifact dep : dependencies) {
        if (SONAR_GROUPID.equals(dep.getGroupId()) && SONAR_PLUGIN_API_ARTIFACTID.equals(dep.getArtifactId())
          && SONAR_PLUGIN_API_TYPE.equals(dep.getType())) {
          return dep;
        }
      }
    }
    return null;
  }

  protected String getMessage(String title, List<String> ids) {
    StringBuilder message = new StringBuilder();
    message.append(title);
    message.append("\n\n");
    for (String id : ids) {
      message.append("\t").append(id).append("\n");
    }
    return message.toString();
  }
}
