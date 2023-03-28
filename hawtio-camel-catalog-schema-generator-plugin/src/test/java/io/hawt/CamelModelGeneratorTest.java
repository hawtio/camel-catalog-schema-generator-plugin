package io.hawt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.hawt.camelcatalog.maven.plugin.CamelModelGenerator;

/**
 * @author phantomjinx
 */
public class CamelModelGeneratorTest {

	private static final String TARGET_DIR = "./target";
	private String camelVersion = "3.20.2";
	private Logger logger  = new ConsoleLogger();
	private Log log = new DefaultLog(logger);

	@BeforeEach
	protected void setUp() throws Exception {
	}

	@AfterEach
	protected void tearDown() throws Exception {
	}

	@Test
	public void testGenerator() throws Exception {	
		File schemaFile = new File(TARGET_DIR + "/camelModel.ts");
		CamelModelGenerator generator = new CamelModelGenerator(
			log,
			camelVersion,
			new File(TARGET_DIR + "/camel-catalog"),
			schemaFile
		);
		generator.generate();

		assertTrue(schemaFile.exists());
		assertTrue(schemaFile.length() > 0);

		String content = Files.readString(schemaFile.toPath());

		String versionVar = "var _apacheCamelModelVersion = '" + camelVersion + "';";
		assertTrue(content.contains(versionVar));

		// Remove line1
		content = content.replace(versionVar, "");
		
		String schemaVar = "var _apacheCamelModel ={";
		assertTrue(content.contains(schemaVar));

		// Remove var and replace with just a brace
		content = content.replace(schemaVar, "{");

		JsonElement schemaElement = JsonParser.parseString(content);
		JsonObject schema = jsonObject(schemaElement);
		JsonObject definitions = jsonObject(schema.get("definitions"));
		assertEquals(135, definitions.entrySet().size());
		checkChildObject(definitions, "expression", "expression");
		checkChildObject(definitions, "contextScan", "Context Scan");
		checkChildObject(definitions, "zookeeperServiceDiscovery", "Zookeeper Service Discovery");

		JsonObject rests = jsonObject(schema.get("rests"));
		assertEquals(23, rests.entrySet().size());
		checkChildObject(rests, "apiKey", "Api Key");
		checkChildObject(rests, "securityDefinitions", "Rest Security Definitions");
		
		JsonObject dataformats = jsonObject(schema.get("dataformats"));
		assertEquals(46, dataformats.entrySet().size());
		checkChildObject(dataformats, "any23", "Any23");
		checkChildObject(dataformats, "zipFile", "Zip File");

		JsonObject languages = jsonObject(schema.get("languages"));
		assertEquals(24, languages.entrySet().size());
		checkChildObject(languages, "constant", "Constant");
		checkChildObject(languages, "xtokenize", "XML Tokenize");

		JsonObject components = jsonObject(schema.get("components"));
		assertEquals(352, components.entrySet().size());
		checkChildObject(components, "activemq", "ActiveMQ");
		checkChildObject(components, "kubernetes-nodes", "Kubernetes Nodes");
		checkChildObject(components, "zookeeper-master", "ZooKeeper Master");
	}
	
	private JsonObject jsonObject(JsonElement element) {
		assertNotNull(element);
		assertTrue(element.isJsonObject());
		return element.getAsJsonObject();
	}

	private void checkChildObject(JsonObject parent, String id, String title) {
		JsonObject child = jsonObject(parent.get(id));
		JsonElement typeElement = child.get("type");
		assertNotNull(typeElement);
		assertEquals("object", typeElement.getAsString());
		JsonElement titleElement = child.get("title");
		assertNotNull(titleElement);
		assertEquals(title, titleElement.getAsString());
	}
}
