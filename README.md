# Camel Catalog Schema Generator

![Maven Central](https://img.shields.io/maven-central/v/io.hawt/camel-catalog-schema-generator-plugin)
[![Test](https://github.com/hawtio/camel-catalog-schema-generator-plugin/actions/workflows/test.yml/badge.svg)](https://github.com/hawtio/camel-catalog-schema-generator-plugin/actions/workflows/test.yml)

This package contains the Maven generator [mojo](https://maven.apache.org/plugin-developers) used to convert the [Camel Catalog](https://camel.apache.org/manual/camel-catalog.html) into a JSON object that can be consumed by JavaScript clients.

## Specifying Camel version

The version of Camel Catalog to be generated is specified by the `org.apache.camel:camel-catalog` dependency in your project's POM.

```xml
<dependency>
    <groupId>org.apache.camel</groupId>
    <artifactId>camel-catalog</artifactId>
    <version>4.0.1</version>
</dependency>
```

## Generating model

The mojo should be called from your project's POM as such:

```xml
<plugin>
    <groupId>io.hawt</groupId>
    <artifactId>camel-catalog-generator-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <id>generate-camel-model</id>
            <goals>
                <goal>generate-camel-model</goal>
            </goals>
            <configuration>
                <buildDir>.</buildDir>
                <schemaFile>camelModel.js</schemaFile>
            </configuration>
        </execution>
    </executions>
</plugin>
```

The result will be a file named `camelModel.js` generated in the build directory. This can be then consumed as a JavaScript source file in the client application.
