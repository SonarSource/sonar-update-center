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
<#if pluginHeader.getNbVersions() gt 1 >
<br>
<span style="font-size:smaller;">
<span id="moreVersionsLink" >
<a   onclick="return showMoreVersions()" href=""> More versions
    <script type="text/javascript">// <![CDATA[
    function showMoreVersions() {
        AJS.$('#moreVersions').show();
        AJS.$('#moreVersionsLink').hide();
        AJS.$('#fewerVersionsLink').show();
        return false;
    }
    // ]]&gt;</script>
</a></span>
<span id="fewerVersionsLink" style="display:none">
<a   onclick="return showFewerVersions()" href=""> Fewer versions
    <script type="text/javascript">// <![CDATA[
    function showFewerVersions() {
        AJS.$('#moreVersions').hide();
        AJS.$('#moreVersionsLink').show();
        AJS.$('#fewerVersionsLink').hide();
        return false;
    }
    // ]]&gt;</script>
</a></span>
</span>
</#if>
<#if pluginHeader.organization?? || pluginHeader.license?? || pluginHeader.issueTracker?? || pluginHeader.sources?? ||  pluginHeader.getNbVersions() gt 1>
<br>
</#if>
<div id="lastVersion">
<#list pluginHeader.getAllVersions() as pluginVersion>

    <div style="padding-top:10px;padding-bottom:5px">
    <span style="font-size:larger;"><strong>${pluginHeader.getName()} ${pluginVersion.getVersion()}</strong></span>
    <#if pluginVersion.date?? > &#8211; ${pluginVersion.date}</#if>
    <#if pluginVersion.getSonarVersionRange()?? > &#8211; Compatible with SonarQube ${pluginVersion.getSonarVersionRange()}</#if>
    <br>

    <#if pluginVersion.description?? >${pluginVersion.description}<br></#if>

    <#if pluginVersion.downloadUrl?? ><a target="_top" href="${pluginVersion.downloadUrl}">Download</a>
        <#if pluginVersion.changelogUrl?? > &#8211; </#if>
    </#if>
    <#if pluginVersion.changelogUrl?? ><a target="_top" href="${pluginVersion.changelogUrl}">Release notes</a></#if>
    <#if pluginHeader.getNbVersions() gt 1 >
        <#if pluginVersion.getVersion() = pluginHeader.getLastVersionString() >
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

