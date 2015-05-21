<#if pluginHeader.organization?? >By
    <#if pluginHeader.organizationUrl?? >
    <a target="_top" href="${pluginHeader.organizationUrl}">${pluginHeader.organization}</a>
    <#else>
    ${pluginHeader.organization}
    </#if>
    <#if pluginHeader.license?? || pluginHeader.issueTracker?? || pluginHeader.sources?? >
    &#8211;
    </#if>
</#if>
<#if pluginHeader.license?? >
${pluginHeader.license}
    <#if pluginHeader.issueTracker?? || pluginHeader.sources?? >
    &#8211;
    </#if>
</#if>
<#if pluginHeader.issueTracker?? >
<a target="_top" href="${pluginHeader.issueTracker}">Issue Tracker</a>
    <#if pluginHeader.sources?? >
    &#8211;
    </#if>
</#if>
<#if pluginHeader.sources?? >
<a target="_top" href="${pluginHeader.sources}">Sources</a>
</#if>
<#if pluginHeader.organization?? || pluginHeader.license?? || pluginHeader.issueTracker?? || pluginHeader.sources?? >
<br>
</#if>
<div id="lastVersion">
<#list pluginHeader.getAllVersions() as pluginVersion>

    <div style="padding-top:10px;padding-bottom:5px">
    <span style="font-size:larger;"><strong>${pluginHeader.getName()} version ${pluginVersion.getVersion()}</strong></span>
    <#if pluginVersion.date?? > &#8211; ${pluginVersion.date}</#if>
    <#if pluginVersion.getSonarVersionRange()?? > &#8211; ${pluginVersion.getSonarVersionRange()}</#if>
    <br>

    <#if pluginVersion.description?? >${pluginVersion.description}<br></#if>

    <#if pluginVersion.downloadUrl?? ><a target="_top" href="${pluginVersion.downloadUrl}">Download</a>
        <#if pluginVersion.changelogUrl?? > &#8211; </#if>
    </#if>
    <#if pluginVersion.changelogUrl?? ><a target="_top" href="${pluginVersion.changelogUrl}">Release notes</a></#if>
    <#if pluginHeader.getNbVersions() gt 1 >
        <#if pluginVersion.getVersion() = pluginHeader.getLastVersionString() >
        <span id="moreVersionsLink">
            <#if pluginVersion.changelogUrl?? || pluginVersion.downloadUrl?? >
                &#8211;
            </#if>
            <a   onclick="return showMoreVersions()" href=""> Show more versions
            <script type="text/javascript">// <![CDATA[
            function showMoreVersions() {
                AJS.$('#moreVersions').show();
                AJS.$('#moreVersionsLink').hide();
                return false;
            }
            // ]]&gt;</script>
        </a></span>
    </div> <#-- Close the div style -->
</div> <#-- Close the div last version -->
<div id="moreVersions" style="display:none">
        <#else>
    </div> <#-- Close the div style -->
        </#if>
    <#else>
    </div> <#-- Close the div style -->
    </#if>
</#list>
</div> <#-- Close the div either last or more version -->

