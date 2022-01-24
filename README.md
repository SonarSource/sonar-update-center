# Sonar Update Center [![Build Status](https://api.cirrus-ci.com/github/SonarSource/sonar-update-center.svg)](https://cirrus-ci.com/github/SonarSource/sonar-update-center) [![Quality Gate Status](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.update-center%3Asonar-update-center&metric=alert_status)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.update-center%3Asonar-update-center)

## Use as a library

```
<dependency>
    <groupId>org.sonarsource.update-center</groupId>
    <artifactId>sonar-update-center</artifactId>
    <version>(latest)</version>
</dependency>
```

## Use as a mojo

### Setup

First, build and install locally.

```bash
mvn clean install -DskipTests
```

You will need the update center properties, which you can find [here](https://github.com/SonarSource/sonar-update-center-properties/) (adapt the `-DinputFile` parameter below accordingly).

### To generate metadata files

```bash
mvn org.sonarsource.update-center:sonar-update-center-mojo:LATEST:generate-metadata \
    -DinputFile=../sonar-update-center-properties/update-center-source.properties \
    -DoutputDir=output

# Currently, this requires 2 "deprecated" parameters to run. See https://jira.sonarsource.com/browse/UPC-106
mvn org.sonarsource.update-center:sonar-update-center-mojo:LATEST:generate-metadata \
    -DinputFile=../sonar-update-center-properties/update-center-source.properties \
    -DoutputDir=output \
    -DeditionsDownloadBaseUrl=https://binaries.sonarsource.com/CommercialDistribution/editions/ \
    -DeditionsOutputDir=output-editions
```

### To generate html

This will generate html snippets for every plugins plus the compatability matrix

```bash
mvn org.sonarsource.update-center:sonar-update-center-mojo:LATEST:generate-html \
    -DinputFile=../sonar-update-center-properties/update-center-source.properties \
    -DoutputDir=output
```

### To generate json files

```bash
mvn org.sonarsource.update-center:sonar-update-center-mojo:LATEST:generate-json \
    -DinputFile=../sonar-update-center-properties/update-center-source.properties \
    -DoutputDir=output
```

### License

Copyright 2010-2022 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)
