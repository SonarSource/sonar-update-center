<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
<head>
    <title>${pluginHeader.getName()}</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="generator" content="Sonar Update Center"/>
    <style type="text/css">
        @import url("http://www.sonarsource.com/wp-content/themes/sonarsource/style.css");
        
        /* Reset some sonarsource styles because we are in the iframe */
        
        body {
          min-width: 0;
        }
        
        #content {
          width: 100%; 
        }
        
        #page-content {
          width: 100%;
          padding-right: 0;
          border-right: none;
          float: none;
          margin-right: 0;
          padding-bottom: 0;
        }
    </style>
</head>
<body>
  <div id="main">
    <div id="content">
      <div id="page-content">

       
        <script type="text/javascript">
          function checkTC(downloadLink, pluginId, url) {
            var tc = document.getElementById('tc-' + pluginId);
            if (!tc.checked) {
              alert('Please accept the Terms and Conditions');
              downloadLink.href='#';
            } else {
              downloadLink.href=url;
            }
            return tc.checked;
          }
        </script>
         
        <div class="entry">
          <table class="plugin-downloads">
            <thead>
              <tr>
                <th colspan="6">
                  <input type="checkbox" id="tc-${pluginHeader.getKey()}" name="tc-${pluginHeader.getKey()}" />I accept the <a target="_blank" href="http://dist.sonarsource.com/SonarSource_Terms_And_Conditions.pdf">Terms and Conditions</a>
                </th>
              </tr>
            </thead>
              
              <tr class="header">
                <td>Version</td>
                <td>Date</td>
                <td>Description</td>
                <td>SonarQube</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
              </tr>
              
            <tbody id="select-${pluginHeader.getKey()}">
              <#list pluginHeader.getAllVersions() as pluginVersion>
              <tr class="<#if pluginVersion_index = 0 >nobar<#else>bar</#if>">
                <td class="version">${pluginVersion.getVersion()}</td>
                <td class="other"><#if pluginVersion.date?? >${pluginVersion.date}</#if></td>
                <td class="description"><#if pluginVersion.description?? >${pluginVersion.description}</#if></td>
                <td class="other"><#if pluginVersion.sonarVersionRange?? >${pluginVersion.sonarVersionRange}</#if></td>
                <td class="other"><#if pluginVersion.changelogUrl?? ><a target="_top" href="${pluginVersion.changelogUrl}">Release notes</a></#if></td>
                <td><#if pluginVersion.downloadUrl?? ><a href="#" class="highlight" onClick="return checkTC(this, '${pluginHeader.getKey()}', '${pluginVersion.downloadUrl}')">Download</a></#if></td> 
              </tr>
              </#list>
            
            </tbody>
          
          </table>
        </div>
      </div>
    </div>   
  </div>
</body>
</html>
