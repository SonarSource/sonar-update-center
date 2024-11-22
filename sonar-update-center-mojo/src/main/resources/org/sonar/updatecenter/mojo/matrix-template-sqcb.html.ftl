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
        <th><strong>SonarQube Community Build version</strong></th>
        <#list matrix.communityBuildVersions as sonarqubeCommunityBuild>
        <th>
          ${sonarqubeCommunityBuild.displayVersion}
          <#if sonarqubeCommunityBuild.isLta() >
          <br/> <strong>(LTA)</strong>
          </#if>
        </th>
        </#list>
    </tr>
    <tr>
        <td style="white-space:nowrap"><strong>Plugin / Release Date</strong></td>
        <#list matrix.communityBuildVersions as sonarqubeCommunityBuild>
        <td${sonarqubeCommunityBuild.isLta()?string(' class="lta"', '')}>${(sonarqubeCommunityBuild.releaseDate)!}</td>
        </#list>
    </tr>
  </thead>
  <tbody>
    <#list matrix.pluginsForCommunityBuild as plugin>
    <tr>
        <td><strong>
        <#if plugin.homepageUrl?? >
        <a target="_top" href="${plugin.homepageUrl}">${plugin.name}</a>
        <#else>
        ${plugin.name}
        </#if>
        </strong>
        </td>
        <#list matrix.communityBuildVersions as sonarqubeCommunityBuild>
        <td${sonarqubeCommunityBuild.isLta()?string(' class="lta"', '')}>
          <#if plugin.supports(sonarqubeCommunityBuild.realVersion) >
          ${plugin.supportedVersion(sonarqubeCommunityBuild.realVersion)}
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
