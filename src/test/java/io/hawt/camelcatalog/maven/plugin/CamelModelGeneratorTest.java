package io.hawt.camelcatalog.maven.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map.Entry;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author phantomjinx
 */
public class CamelModelGeneratorTest {

    private static final String TARGET_DIR = "./target";
    private File schemaDir = new File(TARGET_DIR + "/schemas");
    private String schemaFileName = "camel-model.ts";
    private String camelVersion = "3.20.2";
    private Logger logger = new ConsoleLogger();
    private Log log = new DefaultLog(logger);

    @BeforeEach
    protected void setUp() throws Exception {
    }

    @AfterEach
    protected void tearDown() throws Exception {
    }

    private String resource(String fileName) throws Exception {
        URL restResource = getClass().getClassLoader().getResource(fileName);
        assertNotNull(restResource);
        String text = Files.readString(Paths.get(restResource.toURI()));
        assertTrue(text.length() > 0);
        return text;
    }
    
    private JsonObject validJson(String filename) throws IOException {
        File jsonFile = new File(schemaDir, filename);
        assertTrue(jsonFile.exists());
        String content = Files.readString(jsonFile.toPath());
        assertTrue(content.length() > 0);
        JsonElement schemaElement = JsonParser.parseString(content);
        return jsonObject(schemaElement);
    }

    @Test
    public void testParseSchemaObject() throws Exception {
        String srcRest = resource("source-rest.json");
        JsonObject expRestObject = JsonParser.parseString(resource("expected-rest.json")).getAsJsonObject();
        String srcAggregate = resource("source-aggregate.json");
        JsonObject expAggObject = JsonParser.parseString(resource("expected-aggregate.json")).getAsJsonObject();

        CamelModelGenerator generator = new CamelModelGenerator(log, camelVersion,
                new File(TARGET_DIR + "/camel-catalog"), schemaDir, schemaFileName);
        generator.initIcons();

        JsonObject restObject = generator.parseSchemaObject("rest", "model", srcRest);
        assertEquals(expRestObject, restObject);
        
        JsonObject aggObject = generator.parseSchemaObject("aggregate", "model", srcAggregate);
        assertEquals(expAggObject, aggObject);
    }

    @Test
    public void testGenerator() throws Exception {
        
        CamelModelGenerator generator = new CamelModelGenerator(log, camelVersion,
                new File(TARGET_DIR + "/camel-catalog"), schemaDir, schemaFileName);
        generator.generate();

        assertTrue(schemaDir.exists());
        assertTrue(schemaDir.isDirectory());
        assertTrue(schemaDir.list().length > 0);

        JsonObject definitions = validJson("definitions-camel-model.json");
        JsonObject rests = validJson("rests-camel-model.json");
        JsonObject dataformats = validJson("dataformats-camel-model.json");
        JsonObject languages = validJson("languages-camel-model.json");
        JsonObject components = validJson("components-camel-model.json");

        JsonObject defContent = jsonObject(definitions.get("definitions"));
        assertEquals(135, defContent.entrySet().size());
        checkChildObject(defContent, "expression", "expression");
        checkChildObject(defContent, "contextScan", "Context Scan");
        checkChildObject(defContent, "zookeeperServiceDiscovery", "Zookeeper Service Discovery");
        JsonObject aggregate = checkChildObject(defContent, "aggregate", "Aggregate");
        JsonObject expAggObject = JsonParser.parseString(resource("expected-aggregate.json")).getAsJsonObject();
        JsonObject expAggContent = checkChildObject(expAggObject, "aggregate", "Aggregate");
        assertEquals(expAggContent, aggregate);
        
        JsonObject restContent = jsonObject(rests.get("rests"));
        assertEquals(23, restContent.entrySet().size());
        checkChildObject(restContent, "apiKey", "Api Key");
        checkChildObject(restContent, "securityDefinitions", "Rest Security Definitions");
        JsonObject rest = checkChildObject(restContent, "rest", "Rest");
        JsonObject expRestObject = JsonParser.parseString(resource("expected-rest.json")).getAsJsonObject();
        JsonObject expRestContent = checkChildObject(expRestObject, "rest", "Rest");
        assertEquals(expRestContent, rest);

        JsonObject datafmtContent = jsonObject(dataformats.get("dataformats"));
        assertEquals(46, datafmtContent.entrySet().size());
        checkChildObject(datafmtContent, "any23", "Any23");
        checkChildObject(datafmtContent, "zipFile", "Zip File");

        JsonObject langContent = jsonObject(languages.get("languages"));
        assertEquals(24, langContent.entrySet().size());
        checkChildObject(langContent, "constant", "Constant");
        checkChildObject(langContent, "xtokenize", "XML Tokenize");

        JsonObject compContent = jsonObject(components.get("components"));
        assertEquals(352, compContent.entrySet().size());
        checkChildObject(compContent, "activemq", "ActiveMQ");
        checkChildObject(compContent, "kubernetes-nodes", "Kubernetes Nodes");
        checkChildObject(compContent, "zookeeper-master", "ZooKeeper Master");
        
        checkForNulls(definitions);
        checkForNulls(rests);
        checkForNulls(dataformats);
        checkForNulls(languages);
        checkForNulls(components);
    }

    private void checkForNulls(JsonElement jsonElement) {
        assertNotNull(jsonElement);
        
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            for(Entry<String, JsonElement> entry : jsonObject.asMap().entrySet()) {
                checkForNulls(entry.getValue());
            }
        } else if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (int i = 0; i < jsonArray.size(); ++i) {
                checkForNulls(jsonArray.get(i));
            }
        }
    }

    private JsonObject jsonObject(JsonElement element) {
        assertNotNull(element);
        assertTrue(element.isJsonObject());
        return element.getAsJsonObject();
    }

    private JsonObject checkChildObject(JsonObject parent, String id, String title) {
        JsonObject child = jsonObject(parent.get(id));
        JsonElement typeElement = child.get("type");
        assertNotNull(typeElement);
        assertEquals("object", typeElement.getAsString());
        JsonElement titleElement = child.get("title");
        assertNotNull(titleElement);
        assertEquals(title, titleElement.getAsString());
        return child;
    }
}
