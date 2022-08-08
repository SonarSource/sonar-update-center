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
        <th><strong>SonarQube Version</strong></th>
        <#list matrix.sqVersions as sqVersion>
        <th>
          ${sqVersion.displayVersion}
          <#if sqVersion.isLts() >
          <br/> <strong>(LTS)</strong>
          </#if>
        </th>
        </#list>
    </tr>
    <tr>
        <td style="white-space:nowrap"><strong>Plugin / Release Date</strong></td>
        <#list matrix.sqVersions as sqVersion>
        <td${sqVersion.isLts()?string(' class="lts"', '')}>${(sqVersion.releaseDate)!}</td>
        </#list>
    </tr>
  </thead>
  <tbody>
    <#list matrix.plugins as plugin>
    <tr>
        <td><strong>
        <#if plugin.homepageUrl?? >
        <a target="_top" href="${plugin.homepageUrl}">${plugin.name}</a>
        <#else>
        ${plugin.name}
        </#if>
        </strong>
        </td>
        <#list matrix.sqVersions as sqVersion>
        <td${sqVersion.isLts()?string(' class="lts"', '')}>
          <#if plugin.supports(sqVersion.realVersion) >
          ${plugin.supportedVersion(sqVersion.realVersion)}
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
