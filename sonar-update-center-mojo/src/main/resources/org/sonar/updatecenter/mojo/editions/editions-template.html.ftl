<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
<head>
    <title>Compatibility matrix</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="generator" content="Sonar Update Center"/>
    <style type="text/css">
        @import url("style-confluence.css");
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
          </br> <strong>(LTS)</strong>
          </#if>
        </th>
        </#list>
    </tr>
    <tr>
        <td style="white-space:nowrap"><strong>Edition / Release Date</strong></td>
        <#list matrix.sqVersions as sqVersion>
        <td>${(sqVersion.releaseDate)!}</td>
        </#list>
    </tr>
  </thead>
  <tbody>
    <#list matrix.editions as edition>
    <tr>
        <td><strong>
        ${edition.name}
        </strong>
        </td>
        <#list matrix.sqVersions as sqVersion>
        <td>
          <#if edition.supports(sqVersion.realVersion) >
          <a href="${downloadBaseUrl}/${edition.supportedEdition(sqVersion.realVersion).getZipFileName()}">Download</a>
          <#else>
          <img class="emoticon" alt="(not compatible)" src="error.png"></img>
          </#if>
        </td>
        </#list>
    </tr>
    </#list>
  </tbody>
</table>
</body>
</html>
