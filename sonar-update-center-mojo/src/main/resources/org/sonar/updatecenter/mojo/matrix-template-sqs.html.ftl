<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
<head>
    <title>Compatibility matrix</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="generator" content="Sonar Update Center"/>
    <style type="text/css">
        @import url("styles.css");
    </style>
</head>
<body>
<table cellpadding="0" cellspacing="0">
  <thead>
    <tr>
        <th><strong>SonarQube Server version</strong></th>
        <#list matrix.sonarqubeServerVersions as sonarqubeServerVersion>
        <th>
          ${sonarqubeServerVersion.displayVersion}
          <#if sonarqubeServerVersion.isLta() >
          <br/> <strong>(LTA)</strong>
          </#if>
        </th>
        </#list>
    </tr>
    <tr>
        <td style="white-space:nowrap"><strong>Plugin / Release Date</strong></td>
        <#list matrix.sonarqubeServerVersions as sonarqubeServerVersion>
        <td${sonarqubeServerVersion.isLta()?string(' class="lta"', '')}>${(sonarqubeServerVersion.releaseDate)!}</td>
        </#list>
    </tr>
  </thead>
  <tbody>
    <#list matrix.pluginsForSonarQubeServer as plugin>
    <tr>
        <td><strong>
        <#if plugin.homepageUrl?? >
        <a target="_top" href="${plugin.homepageUrl}">${plugin.name}</a>
        <#else>
        ${plugin.name}
        </#if>
        </strong>
        </td>
        <#list matrix.sonarqubeServerVersions as sonarqubeServerVersion>
        <td${sonarqubeServerVersion.isLta()?string(' class="lta"', '')}>
          <#if plugin.supports(sonarqubeServerVersion.realVersion) >
          ${plugin.supportedVersion(sonarqubeServerVersion.realVersion)}
          <#else>
          <img class="emoticon" alt="(not compatible)" src="error.png" />
          </#if>
        </td>
        </#list>
    </tr>
    </#list>
  </tbody>
</table>
</body>
</html>
