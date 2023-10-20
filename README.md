# Camel Catalog Schema Generator

This package contains the Maven generator [mojo](https://maven.apache.org/plugin-developers) used to convert the [Camel Catalog](https://camel.apache.org/manual/camel-catalog.html) into a JSON object that can be consumed by JavaScript clients.

The mojo should be called from a client project's `pom.xml` as such:

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
