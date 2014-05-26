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
    <tr>
        <td style="white-space:nowrap"><strong>Latest version</strong></td>
        <td>
            <strong>${pluginHeader.getLatestVersion().getVersion()}</strong> ( ${pluginHeader.getLatestVersion().getDate()} )<br/>
            <br/>
            <#if pluginHeader.getSonarLtsVersion()?? >
            <#if pluginHeader.getLatestVersion().compatibleWithLts() >
            Compatible
            <#else>
            Not compatible
            </#if>
            with LTS version (SonarQube&#0153; ${pluginHeader.getSonarLtsVersion()})
            <br/>
            </#if>
            Requires SonarQube&#0153; ${pluginHeader.getLatestVersion().getSonarVersion()} or higher
            ( check <a href="http://docs.codehaus.org/display/SONAR/Plugin+version+matrix" target="_top">version compatibility</a> )
            <br/>
            Download: <a href="${pluginHeader.getLatestVersion().getDownloadUrl()}">${pluginHeader.getLatestVersion().getDownloadUrl()}</a>
        </td>
    </tr>
    <#if pluginHeader.ltsVersion?? >
    <tr>
        <td style="white-space:nowrap"><strong>Compatible with LTS version</strong></td>
        <td>
            <strong>${pluginHeader.ltsVersion.getVersion()}</strong> ( ${pluginHeader.ltsVersion.getDate()} )<br/>
            <br/>
            Requires SonarQube&#0153; ${pluginHeader.ltsVersion.getSonarVersion()} or higher
            ( check <a href="http://docs.codehaus.org/display/SONAR/Plugin+version+matrix" target="_top">version compatibility</a> )
            <br/>
            Download: <a href="${pluginHeader.getLtsVersion().getDownloadUrl()}">${pluginHeader.ltsVersion.getDownloadUrl()}</a>
        </td>
    </tr>
    </#if>
    <#if pluginHeader.license?? >
    <tr>
        <td><strong>License</strong></td>
        <td>${pluginHeader.license}</td>
    </tr>
    </#if>
    <#if pluginHeader.developers?? >
    <tr>
        <td><strong>Developers</strong></td>
        <td>${pluginHeader.developers}</td>
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
</body>
</html>
