<#if pluginHeader.organization?? >By
    <#if pluginHeader.organizationUrl?? ><a target="_top"
                                            href="${pluginHeader.organizationUrl}">${pluginHeader.organization}</a>
    <#else>
    ${pluginHeader.organization}
    </#if>
</#if>
<#if pluginHeader.license?? >
- ${pluginHeader.license}
</#if>
<#if pluginHeader.issueTracker?? >
- <a "href=${pluginHeader.issueTracker}">Issue Tracker</a>
</#if>