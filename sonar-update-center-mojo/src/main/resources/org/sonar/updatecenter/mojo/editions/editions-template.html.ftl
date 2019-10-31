<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
<head>
    <title>Compatibility matrix</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="generator" content="Sonar Update Center"/>
    <style type="text/css">
        @import url("styles.css");
    </style>
    <script type="text/javascript">
        function checkTC(downloadLink, url) {
            var tc = document.getElementById('tc');
            if (!tc.checked) {
                alert('Please accept the Terms and Conditions');
                downloadLink.href = '#';
            } else {
                downloadLink.href = url;
            }
            return tc.checked;
        }
    </script>
</head>
<body>
<input type="checkbox" id="tc" name="tc"/>I accept the <a
    target="_blank"
    href="http://dist.sonarsource.com/SonarSource_Terms_And_Conditions.pdf">Terms and Conditions</a>
<table cellpadding="0" cellspacing="0">
  <thead>
    <tr>
        <th><strong>SonarQube Version</strong></th>
        <#list sqVersions as sqVersion>
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
        <#list sqVersions as sqVersion>
        <td>${(sqVersion.releaseDate)!}</td>
        </#list>
    </tr>
  </thead>
  <tbody>
    <tr>
        <td><strong>
        ${edition.name}
        </strong>
        </td>
        <#list sqVersions as sqVersion>
        <td>
          <#if edition.supports(sqVersion.realVersion) >
          <a href="#" onClick="return checkTC(this, '${edition.getDownloadUrlForSQVersion(sqVersion.realVersion)}')">Download</a>
          <#else>
          <img class="emoticon" alt="(not compatible)" src="error.png"></img>
          </#if>
        </td>
        </#list>
    </tr>
  </tbody>
</table>
</body>
</html>
