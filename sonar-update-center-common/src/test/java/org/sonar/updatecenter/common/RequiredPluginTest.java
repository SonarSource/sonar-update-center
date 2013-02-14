package org.sonar.updatecenter.common;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class RequiredPluginTest {

  @Test
  public void should_create_from_text(){
    RequiredPlugin requiredPlugin = RequiredPlugin.create(new Plugin("plugin"), "1.0");
    assertThat(requiredPlugin.getPlugin().getKey()).isEqualTo("plugin");
    assertThat(requiredPlugin.getVersion().getName()).isEqualTo("1.0");
  }
}
