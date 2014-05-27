<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
<head>
    <title>${pluginHeader.getName()}</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="generator" content="Sonar Update Center"/>
    <style type="text/css">
        @import url("style-confluence.css");
    </style>
</head>
<body>
<table cellpadding="0" cellspacing="0">
    <tr>
        <td><strong>Name</strong></td>
        <td>${pluginHeader.getName()}</td>
    </tr>
    <#if pluginHeader.license?? >
    <tr>
        <td><strong>License</strong></td>
        <td>${pluginHeader.license}</td>
    </tr>
    </#if>
    <#if pluginHeader.organization?? >
    <tr>
        <td><strong>Author</strong></td>
        <td>
          <#if pluginHeader.organizationUrl?? >
            <a href="${pluginHeader.organizationUrl}">${pluginHeader.organization}</a>
          <#else>
            ${pluginHeader.organization}
          </#if>
        </td>
    </tr>
    </#if>
    <#if pluginHeader.issueTracker?? >
    <tr>
        <td style="white-space:nowrap"><strong>Issue tracker</strong></td>
        <td>${pluginHeader.issueTracker}</td>
    </tr>
    </#if>
    <#if pluginHeader.sources?? >
    <tr>
        <td><strong>Sources</strong></td>
        <td>${pluginHeader.sources}</td>
    </tr>
    </#if>
</table>
<br/>
<table>
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
    <tr class="nobar">
      <td class="version">${pluginVersion.getVersion()}</td>
      <td class="other"><#if pluginVersion.date?? >${pluginVersion.date}</#if></td>
      <td class="description"><#if pluginVersion.description?? >${pluginVersion.description}</#if></td>
      <td class="other"><#if pluginVersion.getSonarVersionRange()?? >${pluginVersion.getSonarVersionRange()}</#if></td>
      <td class="other"><#if pluginVersion.changelogUrl?? ><a href="${pluginVersion.changelogUrl}">Release notes</a></#if></td>
      <td><#if pluginVersion.downloadUrl?? ><a href="${pluginVersion.downloadUrl}">Download</a></#if></td> 
    </tr>
    </#list>
  </tbody>
</table>
</body>
</html>
