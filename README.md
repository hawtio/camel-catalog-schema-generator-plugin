# Hawtio Camel Catalog Schema Generator

This package contains the maven generator [mojo](https://maven.apache.org/plugin-developers) used to convert the [Camel Catalog](https://camel.apache.org/camel-k/next/architecture/cr/camel-catalog.html) into a JSON object that can be consumed by javascript clients.

The mojo should be called from a client project's `pom.xml` as such:

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>io.hawt</groupId>
        <artifactId>hawtio-camel-catalog-generator-plugin</artifactId>
        <version>${version.io.hawt.plugin}</version>
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
    ...
    ...
```

The result will be a file named `camelModel.js` generated in the build directory. This can be then consumed as a javascript source file in the client application.