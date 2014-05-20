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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Evgeny Mandrikov
 */
@Mojo(name = "check-dependencies", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class CheckDependenciesMojo extends AbstractSonarPluginMojo {

  private static final String[] GWT_ARTIFACT_IDS = {"gwt-user", "gwt-dev", "sonar-gwt-api"};
  private static final String[] LOG_GROUP_IDS = {"log4j", "commons-logging"};

  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!isSkipDependenciesPackaging()) {
      checkApiDependency();
      checkLogDependencies();
      checkGwtDependencies();
    }
  }

  private void checkApiDependency() throws MojoExecutionException {
    Artifact sonarApi = getSonarPluginApiArtifact();

    if (sonarApi == null) {
      throw new MojoExecutionException(
        SONAR_GROUPID + ":" + SONAR_PLUGIN_API_ARTIFACTID + " should be declared in dependencies");
    }

    if (!Artifact.SCOPE_PROVIDED.equals(sonarApi.getScope())) {
      throw new MojoExecutionException(
        SONAR_GROUPID + ":" + SONAR_PLUGIN_API_ARTIFACTID + " should be declared with scope '" + Artifact.SCOPE_PROVIDED + "'");
    }
  }

  private void checkLogDependencies() throws MojoExecutionException {
    List<String> ids = new ArrayList<String>();
    for (Artifact dep : getIncludedArtifacts()) {
      if (ArrayUtils.contains(LOG_GROUP_IDS, dep.getGroupId())) {
        ids.add(dep.getDependencyConflictId());
      }
    }
    if (!ids.isEmpty()) {
      StringBuilder message = new StringBuilder();
      message.append("Dependencies on the following log libraries should be excluded or declared with scope 'provided':")
        .append("\n\t")
        .append(StringUtils.join(ids, ", "))
        .append('\n');
      getLog().warn(message.toString());
    }
  }

  private void checkGwtDependencies() {
    List<String> ids = new ArrayList<String>();
    for (Artifact dep : getDependencyArtifacts(Artifact.SCOPE_COMPILE)) {
      if (ArrayUtils.contains(GWT_ARTIFACT_IDS, dep.getArtifactId())) {
        ids.add(dep.getDependencyConflictId());
      }
    }
    if (!ids.isEmpty()) {
      getLog().warn(getMessage("GWT dependencies should be defined with scope 'provided':", ids));
    }
  }
}
