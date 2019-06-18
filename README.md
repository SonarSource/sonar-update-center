# Usage

## as a library

`
<dependency>
    <groupId>org.sonarsource.update-center</groupId>
    <artifactId>sonar-update-center</artifactId>
    <version>(latest)</version>
</dependency>
`

## as a mojo

### to generate metadata files

`
mvn org.sonarsource.update-center:sonar-update-center-mojo:LATEST:generate-metata \
    -DinputFile=update-center-source.properties \
    -DoutputDir=output
`

### To generate html

This will generate html snippets for every plugins plus the compatability matrix

`
mvn org.sonarsource.update-center:sonar-update-center-mojo:LATEST:generate-html \
    -DinputFile=update-center-source.properties \
    -DoutputDir=output
`

### To generate json files

`
mvn org.sonarsource.update-center:sonar-update-center-mojo:LATEST:generate-json \
    -DinputFile=update-center-source.properties \
    -DoutputDir=output
`

### License

Copyright 2010-2017 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)
