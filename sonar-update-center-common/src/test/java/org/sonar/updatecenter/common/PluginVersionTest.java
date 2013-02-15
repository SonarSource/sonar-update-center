package org.sonar.updatecenter.common;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class PluginVersionTest {

  @Test
  public void should_create_from_text(){
    PluginVersion pluginVersion = PluginVersion.create(new Plugin("plugin"), "1.0");
    assertThat(pluginVersion.getPlugin().getKey()).isEqualTo("plugin");
    assertThat(pluginVersion.getVersion().getName()).isEqualTo("1.0");
  }
}
