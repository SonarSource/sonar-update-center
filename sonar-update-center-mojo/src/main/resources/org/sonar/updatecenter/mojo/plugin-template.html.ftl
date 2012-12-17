<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
<head>
    <title>${pluginHeader.getName()}</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="generator" content="Sonar Update Center"/>
    <style type="text/css">
        @import url("style.css");
    </style>
</head>
<body>
<table cellpadding="0" cellspacing="0">
    <tr>
        <td><strong>Name</strong></td>
        <td>${pluginHeader.getName()}</td>
    </tr>
    <tr>
        <td><strong>Latest version</strong></td>
        <td><strong>${pluginHeader.getVersion()}</strong> ( ${pluginHeader.getDate()} )</td>
    </tr>
    <tr>
        <td><strong>Requires Sonar version</strong></td>
        <td>
            <strong>${pluginHeader.getSonarVersion()}</strong> or higher
            ( check <a href="http://docs.codehaus.org/display/SONAR/Plugin+version+matrix" target="_top">version compatibility</a> )
        </td>
    </tr>
    <tr>
        <td><strong>Download</strong></td>
        <td><a href="${pluginHeader.getDownloadUrl()}">${pluginHeader.getDownloadUrl()}</a></td>
    </tr>
    <#if pluginHeader.getLicense()?? >
    <tr>
        <td><strong>License</strong></td>
        <td>${pluginHeader.getLicense()}</td>
    </tr>
    </#if>
    <#if pluginHeader.getDevelopers()?? >
    <tr>
        <td><strong>Developers</strong></td>
        <td>${pluginHeader.getDevelopers()}</td>
    </tr>
    </#if>
    <#if pluginHeader.getIssueTracker()?? >
    <tr>
        <td><strong>Issue tracker</strong></td>
        <td>${pluginHeader.getIssueTracker()}</td>
    </tr>
    </#if>
    <#if pluginHeader.getSources()?? >
    <tr>
        <td><strong>Sources</strong></td>
        <td>${pluginHeader.getSources()}</td>
    </tr>
    </#if>
</table>
</body>
</html>
