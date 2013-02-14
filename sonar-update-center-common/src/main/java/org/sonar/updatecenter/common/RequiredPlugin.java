package org.sonar.updatecenter.common;

public class RequiredPlugin {

  public Plugin plugin;
  public Version version;

  private RequiredPlugin(Plugin plugin, Version version) {
    this.plugin = plugin;
    this.version = version;
  }

  public Plugin getPlugin() {
    return plugin;
  }

  public Version getVersion() {
    return version;
  }

  public static RequiredPlugin create(Plugin plugin, Version version) {
    return new RequiredPlugin(plugin, version);
  }

  public static RequiredPlugin create(Plugin plugin, String version) {
    return new RequiredPlugin(plugin, Version.create(version));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RequiredPlugin that = (RequiredPlugin) o;

    if (!plugin.getKey().equals(that.plugin.getKey())) {
      return false;
    }
    if (!version.equals(that.version)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = plugin.getKey().hashCode();
    result = 31 * result + version.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return plugin.getKey().toString() + ":"+ version.toString();
  }
}
