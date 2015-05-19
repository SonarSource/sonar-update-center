‡<table>
  <thead>
    <tr>
      <td><strong>Version</strong></td>
      <td><strong>Date</strong></td>
      <td><strong>Description</strong></td>
      <td><strong>SonarQube</strong></td>
      <td>&nbsp;</td>
      <td>&nbsp;</td>
    </tr>
  </thead>
              
  <tbody>
    <#list pluginHeader.getAllVersions() as pluginVersion>
    <tr>
      <td class="version">${pluginVersion.getVersion()}</td>
      <td class="other"><#if pluginVersion.date?? >${pluginVersion.date}</#if></td>
      <td class="description"><#if pluginVersion.description?? >${pluginVersion.description}</#if></td>
      <td class="other"><#if pluginVersion.getSonarVersionRange()?? >${pluginVersion.getSonarVersionRange()}</#if></td>
      <td class="other"><#if pluginVersion.changelogUrl?? ><a target="_top" href="${pluginVersion.changelogUrl}">Release notes</a></#if></td>
      <td><#if pluginVersion.downloadUrl?? ><a target="_top" href="${pluginVersion.downloadUrl}">Download</a></#if></td> 
    </tr>
    </#list>
  </tbody>
</table>
