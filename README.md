# Sonar Update Center [![Build Status](https://travis-ci.org/SonarSource/sonar-update-center.svg?branch=master)](https://travis-ci.org/SonarSource/sonar-update-center) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.sonarsource.update-center%3Asonar-update-center&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.sonarsource.update-center%3Asonar-update-center)

## Use as a library

```
<dependency>
    <groupId>org.sonarsource.update-center</groupId>
    <artifactId>sonar-update-center</artifactId>
    <version>(latest)</version>
</dependency>
```

## Use as a mojo

### To generate metadata files

```
mvn org.sonarsource.update-center:sonar-update-center-mojo:LATEST:generate-metadata \
    -DinputFile=update-center-source.properties \
    -DoutputDir=output
```

### To generate html

This will generate html snippets for every plugins plus the compatability matrix

```
mvn org.sonarsource.update-center:sonar-update-center-mojo:LATEST:generate-html \
    -DinputFile=update-center-source.properties \
    -DoutputDir=output
```

### To generate json files

```
mvn org.sonarsource.update-center:sonar-update-center-mojo:LATEST:generate-json \
    -DinputFile=update-center-source.properties \
    -DoutputDir=output
```

### License

Copyright 2010-2017 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)
