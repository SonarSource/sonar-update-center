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
<h1 style="margin-bottom: 0px;margin-top: 0px;">Download</h1>
    <#if pluginHeader.getNbVersions() gt 1 >
<span style="font-size:smaller;">
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
</span>
</#if>
<div id="lastVersion">
<#list pluginHeader.getAllVersions() as pluginVersion>
    <div style="padding-top:5px;padding-bottom:10px">
    <span><strong>${pluginHeader.getName()} ${pluginVersion.getVersion()}</strong></span>
    <#if pluginVersion.date?? > &#8211; ${pluginVersion.date}</#if>
    <#if pluginVersion.getSonarVersionRange()?? && !pluginVersion.isArchived() > &#8211; SonarQube ${pluginVersion.getSonarVersionRange()}</#if>
    <br>
    <#if pluginVersion.description?? >${pluginVersion.description}<br></#if>
    <#if pluginVersion.downloadUrl?? && !pluginVersion.isArchived() >
        <input type="checkbox" id="tc-${pluginHeader.getKey()}-${pluginVersion_index}" name="tc-${pluginHeader.getKey()}-${pluginVersion_index}"/>I accept the <a
            target="_blank"
            href="http://dist.sonarsource.com/SonarSource_Terms_And_Conditions.pdf">Terms and Conditions</a>
        &#8211;
        <a href="#" class="highlight" onClick="return checkTC(this, '${pluginHeader.getKey()}-${pluginVersion_index}', '${pluginVersion.downloadUrl}')">Download</a>
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

