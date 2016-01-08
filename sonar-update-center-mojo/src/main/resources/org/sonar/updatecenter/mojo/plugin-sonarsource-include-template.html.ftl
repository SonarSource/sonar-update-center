<script type="text/javascript">
    function checkTC(downloadLink, pluginId, url) {
        var tc = document.getElementById('tc-' + pluginId);
        if (!tc.checked) {
            alert('Please accept the Terms and Conditions');
            downloadLink.href = '#';
        } else {
            downloadLink.href = url;
        }
        return tc.checked;
    }
</script>

<h1>Latest version</h1>
<#list pluginHeader.getAllVersions() as pluginVersion>
<#if pluginVersion_index = 0 >
<table class="plugin-downloads">
    <tr class="nobar">
        <td class="version">${pluginVersion.getVersion()}</td>
        <td class="other"><#if pluginVersion.date?? >${pluginVersion.date}</#if></td>
        <td class="description"><#if pluginVersion.description?? >${pluginVersion.description}</#if></td>
        <td class="other">SonarQube <#if pluginVersion.sonarVersionRange?? >${pluginVersion.sonarVersionRange}</#if></td>
        <td class="other"><#if pluginVersion.changelogUrl?? ><a target="_top" href="${pluginVersion.changelogUrl}">Release notes</a></#if></td>
        <td><#if pluginVersion.downloadUrl?? && !pluginVersion.isArchived() ><a href="#" class="highlight" onClick="return checkTC(this, '${pluginHeader.getKey()}', '${pluginVersion.downloadUrl}')">Download</a></#if></td>
    </tr>
    <tfoot>
    <tr>
        <th colspan="6">
            <input type="checkbox" id="tc-${pluginHeader.getKey()}" name="tc-${pluginHeader.getKey()}"/>I accept the <a
                target="_blank"
                href="http://dist.sonarsource.com/SonarSource_Terms_And_Conditions.pdf">Terms
            and Conditions</a>
        </th>
    </tr>
    </tfoot>
</table>
<#if pluginHeader.getNbVersions() gt 1 >
<h2>
<span id="moreVersionsLink">
	<a onclick="return showMoreVersions()" href="">More versions
        <script type="text/javascript">// <![CDATA[
        function showMoreVersions() {
            document.getElementById('moreVersions').style.display = "inherit";
            document.getElementById('moreVersionsLink').style.display = "none";
            document.getElementById('fewerVersionsLink').style.display = "inherit";
            return false;
        }
        // ]]&gt;</script>
    </a></span>
<span id="fewerVersionsLink" style="display:none">
	<a onclick="return showFewerVersions()" href="">Fewer versions
        <script type="text/javascript">// <![CDATA[
        function showFewerVersions() {
            document.getElementById('moreVersions').style.display = "none";
            document.getElementById('moreVersionsLink').style.display = "inherit";
            document.getElementById('fewerVersionsLink').style.display = "none";
            return false;
        }
        // ]]&gt;</script>
    </a></span>
</h2>
</#if>
<div id="moreVersions" style="display:none">
    <table class="plugin-downloads">
<#else>
        <tr class="bar">
            <td class="version">${pluginVersion.getVersion()}</td>
            <td class="other"><#if pluginVersion.date?? >${pluginVersion.date}</#if></td>
            <td class="description"><#if pluginVersion.description?? >${pluginVersion.description}</#if></td>
            <td class="other">SonarQube <#if pluginVersion.sonarVersionRange?? >${pluginVersion.sonarVersionRange}</#if></td>
            <td class="other"><#if pluginVersion.changelogUrl?? ><a target="_top" href="${pluginVersion.changelogUrl}">Release notes</a></#if></td>
            <td><#if pluginVersion.downloadUrl?? && !pluginVersion.isArchived() ><a href="#" class="highlight" onClick="return checkTC(this, '${pluginHeader.getKey()}', '${pluginVersion.downloadUrl}')">Download</a></#if></td>
        </tr>
</#if>
</#list>
    </table>
</div>

