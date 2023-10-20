package io.hawt.camelcatalog.maven.plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import static io.hawt.camelcatalog.maven.plugin.util.FileHelper.loadText;

public class CamelModelGenerator {

    private static class NameSchemaPair {
        private final String name;
        private final Map<String, JsonObject> schema;

        public NameSchemaPair(String name) {
            this.name = name;
            this.schema = new LinkedHashMap<>();
        }

        public void addGroupSchema(String group, JsonObject groupSchema) {
            schema.put(group, groupSchema);
        }

        public JsonObject getGroupSchema(String group) {
            return schema.get(group);
        }

        public boolean isEmpty() {
            return schema.isEmpty();
        }

        public String getName() {
            return name;
        }

        public Map<String, JsonObject> getSchema() {
            return schema;
        }

        public Set<String> getGroups() {
            return schema.keySet();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Log log;

    private final String camelVersion;

    private final File camelCatalogDir;

    private final File schemaDir;

    private final String schemaFileName;

    private final Gson gson;

    /**
     * Known icons for the models
     */
    private final Properties icons = new Properties();

    public CamelModelGenerator(Log log, String camelVersion, File camelCatalogDir, File schemaDir, String schemaFileName) {
        this.log = log;
        this.camelVersion = camelVersion;
        this.camelCatalogDir = camelCatalogDir;
        this.schemaDir = schemaDir;
        this.schemaFileName = schemaFileName;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

        Objects.requireNonNull(this.log, "log must be initialised");
        Objects.requireNonNull(this.camelVersion, "camel version must be initialised");
        Objects.requireNonNull(this.camelCatalogDir, "camel catalog directory must be initialised");
        Objects.requireNonNull(this.schemaDir, "schema file directory must be initialised");
        Objects.requireNonNull(this.schemaFileName, "schema file name must be initialsed");

        if (!this.camelCatalogDir.canRead()) {
            throw new IllegalArgumentException(
                "Cannot read camel catalog directory: " + this.camelCatalogDir.getAbsolutePath());
        }
    }

    public String getVersion() {
        return camelVersion;
    }

    public Log getLog() {
        return log;
    }

    private void camelCatalogExtract(URL camelCatalogDir, String dataPath, BiConsumer<String, String> callback)
        throws MojoFailureException {
        try (URLClassLoader loader = new URLClassLoader(new URL[] { camelCatalogDir })) {
            InputStream is = loader.getResourceAsStream(dataPath + ".properties");
            String lines = loadText(is);

            for (String name : lines.split("\n")) {
                is = loader.getResourceAsStream(dataPath + "/" + name + ".json");
                String text = loadText(is);
                callback.accept(name, text);
            }
        } catch (Exception e) {
            throw new MojoFailureException("Error loading models from camel-catalog due " + e.getMessage(), e);
        }
    }

    protected void initIcons() throws MojoExecutionException {
        try {
            icons.load(CamelModelGenerator.class.getClassLoader().getResourceAsStream("icons.properties"));
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot load list of icons", e);
        }
    }

    private String findIcon(String name) {
        String answer = icons.getProperty(name);
        if (answer == null) {
            // use generic icon as fallback
            answer = "generic24.png";
        }
        return answer;
    }

    /**
     * The default value may need to be escaped to be safe for json
     */
    private String safeDefaultValue(String value) {
        if ("\"".equals(value)) {
            return "\\\"";
        } else if ("\\".equals(value)) {
            return "\\\\";
        } else {
            return value;
        }
    }

    private String safeDescription(String description) {
        if (description == null) {
            return "";
        }
        // need to escape " as \"
        description = description.replaceAll("\"", "\\\\\"");
        return description;
    }

    private boolean hasGroup(JsonObject model, String group) {
        if (!model.has("group"))
            return false;

        return model.get("group").getAsString().contains(group);
    }

    private String getStringValue(String name, JsonElement element) {
        if (element == null)
            return "";

        if (element.isJsonPrimitive())
            return element.getAsString();

        throw new IllegalStateException("Element " + name + " is not a string value");
    }

    private boolean getBooleanValue(String name, JsonElement element) {
        if (!element.isJsonPrimitive()) {
            throw new IllegalStateException("Element " + name + " is not a boolean value");
        }

        return element.getAsBoolean();
    }

    private JsonArray getArrayValue(String name, JsonElement element) {
        if (!element.isJsonArray()) {
            throw new IllegalStateException("Element " + name + " is not an array value");
        }

        return element.getAsJsonArray();
    }

    private JsonObject parsePropertyValue(JsonElement element) {
        if (!element.isJsonObject()) {
            return null;
        }

        JsonObject source = element.getAsJsonObject();

        JsonObject valueObject = new JsonObject();
        valueObject.addProperty("kind", getStringValue("kind", source.get("kind")));
        valueObject.addProperty("type", getStringValue("type", source.get("type")));

        JsonElement defaultValue = source.get("defaultValue");
        if (defaultValue != null) {
            valueObject.addProperty("defaultValue", safeDefaultValue(getStringValue("defaultValue", defaultValue)));
        }

        JsonElement enumValue = source.get("enum");
        if (enumValue != null) {
            valueObject.add("enum", getArrayValue("enum", enumValue));
        }

        valueObject.addProperty("description", safeDescription(getStringValue("description", source.get("description"))));
        valueObject.addProperty("title", getStringValue("displayName", source.get("displayName")));
        valueObject.addProperty("required", getBooleanValue("required", source.get("required")));
        valueObject.addProperty("deprecated", getBooleanValue("deprecated", source.get("deprecated")));
        return valueObject;
    }

    protected JsonObject parseSchemaObject(String name, String groupId, String json) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        JsonObject groupObject = jsonObject.get(groupId).getAsJsonObject();
        JsonObject srcPropsObject = jsonObject.get("properties").getAsJsonObject();

        String group = getStringValue("label", groupObject.get("label"));
        String title = getStringValue("title", groupObject.get("title"));
        String input = getStringValue("input", groupObject.get("input"));
        String output = getStringValue("output", groupObject.get("output"));
        String nextSiblingAddedAsChild = "false";
        if ("true".equals(input) && "false".equals(output)) {
            nextSiblingAddedAsChild = "true";
        }
        String description = getStringValue("description", groupObject.get("description"));
        String icon = findIcon(name);

        JsonObject contentObject = new JsonObject();
        contentObject.addProperty("type", "object");
        contentObject.addProperty("title", title);
        contentObject.addProperty("group", group);
        contentObject.addProperty("icon", icon);
        contentObject.addProperty("description", safeDescription(description));

        // eips and rests allow to be defined as a graph with inputs and outputs
        if ((group.contains("eip")) || group.contains("rest")) {
            contentObject.addProperty("acceptInput", input);
            contentObject.addProperty("acceptOutput", output);
            contentObject.addProperty("nextSiblingAddedAsChild", nextSiblingAddedAsChild);
        }

        JsonObject propsObject = new JsonObject();

        for (Map.Entry<String, JsonElement> property : srcPropsObject.asMap().entrySet()) {
            JsonObject value = parsePropertyValue(property.getValue());
            if (value == null)
                continue;

            propsObject.add(property.getKey(), value);
        }

        contentObject.add("properties", propsObject);
        JsonObject schemaObject = new JsonObject();
        schemaObject.add(name, contentObject);

        return schemaObject;

    }

    private void generationExpression(JsonObject target, Set<String> languages) {

        JsonArray enumArray = new JsonArray();
        for (String language : languages) {
            // skip abstract expression as an enum choice
            if (!"expression".equals(language)) {
                enumArray.add(language);
            }
        }

        target.addProperty("type", "object");
        target.addProperty("title", "expression");
        target.addProperty("group", "language");
        target.addProperty("icon", findIcon("generic"));
        target.addProperty("description", "Expression in the choose language");

        JsonObject expressionProp = new JsonObject();
        expressionProp.addProperty("kind", "element");
        expressionProp.addProperty("type", "string");
        expressionProp.addProperty("title", "Expression");
        expressionProp.addProperty("group", "language");
        expressionProp.addProperty("description", "The expression");
        expressionProp.addProperty("required", true);

        JsonObject langProp = new JsonObject();
        langProp.addProperty("kind", "element");
        langProp.addProperty("type", "string");
        langProp.addProperty("title", "Expression");
        langProp.addProperty("group", "language");
        langProp.addProperty("description", "The chosen language");
        langProp.addProperty("required", true);
        langProp.add("enum", enumArray);

        JsonObject properties = new JsonObject();
        properties.add("expression", expressionProp);
        properties.add("language", langProp);
        target.add("properties", properties);
    }

    private JsonObject generateGroupSchema(String name, Map<String, JsonObject> content) {
        JsonObject schema = new JsonObject();
        JsonObject schemaContent = new JsonObject();
        for (Map.Entry<String, JsonObject> child : content.entrySet()) {
            schemaContent.add(child.getKey(), child.getValue());
        }
        schema.add(name, schemaContent);
        return schema;
    }

    private void writeToFile(String name, JsonObject modelObject) throws IOException {
        FileWriter writer = new FileWriter(schemaDir + File.separator + name + "-camel-model.json");
        gson.toJson(modelObject, writer);
        writer.close();
    }

    public void generate() throws MojoFailureException, MojoExecutionException {

        initIcons();

        NameSchemaPair definitions = new NameSchemaPair("definitions");
        definitions.addGroupSchema("expression", new JsonObject()); // initialise the expression in the correct location

        NameSchemaPair rests = new NameSchemaPair("rests");
        NameSchemaPair dataformats = new NameSchemaPair("dataformats");
        NameSchemaPair languages = new NameSchemaPair("languages");
        NameSchemaPair components = new NameSchemaPair("components");

        // find the model json files and split into groups
        URL ccDir;
        try {
            ccDir = new URL("file", null, camelCatalogDir.getAbsolutePath());
        } catch (MalformedURLException ex) {
            throw new MojoFailureException("Error loading models from camel-catalog due " + ex.getMessage(), ex);
        }

        camelCatalogExtract(ccDir, "org/apache/camel/catalog/models", (String name, String text) -> {
            // use the model files to split into the groups we use in camelModel.js
            JsonObject schema = parseSchemaObject(name, "model", text);
            JsonObject schemaContent = schema.get(name).getAsJsonObject();
            if (hasGroup(schemaContent, "rest")) {
                rests.addGroupSchema(name, schemaContent);
            } else if (hasGroup(schemaContent, "dataformat")) {
                dataformats.addGroupSchema(name, schemaContent);
            } else if (hasGroup(schemaContent, "language")) {
                languages.addGroupSchema(name, schemaContent);
            } else {
                definitions.addGroupSchema(name, schemaContent);
            }
        });

        camelCatalogExtract(ccDir, "org/apache/camel/catalog/components", (String name, String text) -> {
            JsonObject schema = parseSchemaObject(name, "component", text);
            JsonObject schemaContent = schema.get(name).getAsJsonObject();
            components.addGroupSchema(name, schemaContent);
        });

        if (definitions.isEmpty()) {
            getLog().info("Cannot update " + schemaDir + " as no Camel models found in the Apache Camel version");
            return;
        }

        /*
         * Generate expression
         */
        generationExpression(definitions.getGroupSchema("expression"), languages.getGroups());

        try {
            if (!schemaDir.isDirectory() && !schemaDir.mkdirs())
                throw new IllegalStateException("Cannot create output directory for camel models");

            List<NameSchemaPair> pairs = new ArrayList<>();
            pairs.add(definitions);
            pairs.add(rests);
            pairs.add(dataformats);
            pairs.add(languages);
            pairs.add(components);

            /*
             * Create model file for import
             */
            FileWriter writer = new FileWriter(schemaDir + File.separator + schemaFileName);

            for (NameSchemaPair pair : pairs) {
                JsonObject jsonObject = generateGroupSchema(pair.getName(), pair.getSchema());
                writeToFile(pair.getName(), jsonObject); // write to json file

                writer.write("import " + pair.getName() + " from './" + pair.getName() + "-camel-model.json';\n");
            }

            writer.write("\nvar apacheCamelModelVersion = '" + getVersion() + "';\n\n");

            writer.write(String.format("export { %s, %s, %s, %s, %s, apacheCamelModelVersion };\n", definitions, rests, dataformats, languages, components));
            writer.close();

        } catch (Exception e) {
            throw new MojoFailureException("Error writing model files to schema directory " + schemaDir + ": " + e);
        }

        getLog().info("Assembled Camel models into schema directory: " + schemaDir);
    }
}
