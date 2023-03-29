package io.hawt.camelcatalog.maven.plugin;

import static io.hawt.camelcatalog.maven.plugin.util.FileHelper.loadText;
import static io.hawt.camelcatalog.maven.plugin.util.JSonSchemaHelper.doubleQuote;
import static io.hawt.camelcatalog.maven.plugin.util.JSonSchemaHelper.getValue;
import static io.hawt.camelcatalog.maven.plugin.util.JSonSchemaHelper.parseJsonSchema;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import io.hawt.camelcatalog.maven.plugin.util.CollectionStringBuffer;
import io.hawt.camelcatalog.maven.plugin.util.JSonSchemaHelper;

public class CamelModelGenerator {

    private Log log;

    private String camelVersion;

    private File camelCatalogDir;

    private File schemaFile;

    /**
     * Known icons for the models
     */
    private final Properties icons = new Properties();

    public CamelModelGenerator(Log log, String camelVersion, File camelCatalogDir, File schemaFile) {
        this.log = log;
        this.camelVersion = camelVersion;
        this.camelCatalogDir = camelCatalogDir;
        this.schemaFile = schemaFile;

        Objects.requireNonNull(this.log, "log must be initialised");
        Objects.requireNonNull(this.camelVersion, "camel version must be initialised");
        Objects.requireNonNull(this.camelCatalogDir, "camel catalog directory must be initialised");
        Objects.requireNonNull(this.schemaFile, "schema file must be initialised");

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
                if (text != null) {
                    callback.accept(name, text);
                }
            }
        } catch (Exception e) {
            throw new MojoFailureException("Error loading models from camel-catalog due " + e.getMessage(), e);
        }
    }

    private void initIcons() throws MojoExecutionException {
        try {
            icons.load(CamelModelGenerator.class.getClassLoader().getResourceAsStream("icons.properties"));
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot load list of icons", e);
        }
    }

    private void generateExpression(Set<String> languages, FileOutputStream fos) throws IOException {
        StringBuilder sb = new StringBuilder();

        // enums as a string
        CollectionStringBuffer enums = new CollectionStringBuffer(", ");
        for (String name : languages) {
            // skip abstract expression as a enum choice
            if (!"expression".equals(name)) {
                enums.append(doubleQuote(name));
            }
        }
        String enumValues = enums.toString();

        CollectionStringBuffer cst = new CollectionStringBuffer(",\n");
        sb.append("    ").append(doubleQuote("expression")).append(": {\n");
        cst.append("      \"type\": \"object\"");
        cst.append("      \"title\": " + doubleQuote("expression"));
        cst.append("      \"group\": " + doubleQuote("language"));
        cst.append("      \"icon\": " + doubleQuote("generic24.png"));
        cst.append("      \"description\": " + doubleQuote("Expression in the choose language"));
        sb.append(cst.toString()).append(",\n");

        cst = new CollectionStringBuffer(",\n");
        sb.append("      \"properties\": {\n");
        sb.append("        ").append(doubleQuote("expression")).append(": {\n");
        cst.append("          \"kind\": " + doubleQuote("element"));
        cst.append("          \"type\": " + doubleQuote("string"));
        cst.append("          \"title\": " + doubleQuote("Expression"));
        cst.append("          \"description\": " + doubleQuote("The expression"));
        cst.append("          \"required\": true\n");
        sb.append(cst.toString());
        sb.append("        },\n"); // a property

        cst = new CollectionStringBuffer(",\n");
        sb.append("        ").append(doubleQuote("language")).append(": {\n");
        cst.append("          \"kind\": " + doubleQuote("element"));
        cst.append("          \"type\": " + doubleQuote("string"));
        cst.append("          \"title\": " + doubleQuote("Expression"));
        cst.append("          \"description\": " + doubleQuote("The chosen language"));
        cst.append("          \"required\": true");
        cst.append("          \"enum\": [ " + enumValues + " ]");
        sb.append(cst.toString()).append("\n");
        sb.append("        }\n"); // a property

        sb.append("      }\n"); // properties
        sb.append("    },\n"); // expression

        fos.write(sb.toString().getBytes());
    }

    private void generateSchema(String schema, String parent, Map<String, String> models, FileOutputStream fos,
            Iterator<String> it) throws IOException {
        while (it.hasNext()) {
            String name = it.next();
            String json = models.get(name);

            StringBuilder sb = new StringBuilder();

            List<Map<String, String>> model = parseJsonSchema(parent, json, false);
            List<Map<String, String>> properties = parseJsonSchema("properties", json, true);

            String group = getValue("label", model);
            String title = getValue("title", model);
            String input = getValue("input", model);
            String output = getValue("output", model);
            String nextSiblingAddedAsChild = "false";
            if ("true".equals(input) && "false".equals(output)) {
                nextSiblingAddedAsChild = "true";
            }
            String description = getValue("description", model);
            String icon = findIcon(name);

            // skip non categroized
            if (group == null) {
                continue;
            }

            CollectionStringBuffer cst = new CollectionStringBuffer(",\n");
            sb.append("    ").append(doubleQuote(name)).append(": {\n");
            cst.append("      \"type\": \"object\"");
            cst.append("      \"title\": " + doubleQuote(title));
            cst.append("      \"group\": " + doubleQuote(group));
            cst.append("      \"icon\": " + doubleQuote(icon));
            cst.append("      \"description\": " + doubleQuote(safeDescription(description)));
            // eips and rests allow to be defined as a graph with inputs and outputs
            if ("eips".equals(schema) || "rests".equals(schema)) {
                cst.append("      \"acceptInput\": " + doubleQuote(input));
                cst.append("      \"acceptOutput\": " + doubleQuote(output));
                cst.append("      \"nextSiblingAddedAsChild\": " + doubleQuote(nextSiblingAddedAsChild));
            }
            sb.append(cst.toString()).append(",\n");

            sb.append("      \"properties\": {\n");
            Iterator<Map<String, String>> it2 = properties.iterator();
            while (it2.hasNext()) {
                Map<String, String> option = it2.next();
                cst = new CollectionStringBuffer(",\n");

                String optionName = option.get("name");
                title = asTitle(optionName);
                String kind = option.get("kind");
                String type = option.get("type");
                String required = option.get("required");
                String deprecated = option.get("deprecated");
                description = option.get("description");
                String defaultValue = option.get("defaultValue");
                String enumValues = option.get("enum");

                // special for aggregate as it has duplicate option names
                if ("completionSize".equals(optionName) && "expression".equals(kind)) {
                    optionName = "completionSizeExpression";
                } else if ("completionTimeout".equals(optionName) && "expression".equals(kind)) {
                    optionName = "completionTimeoutExpression";
                }

                // skip inputs/outputs
                if ("inputs".equals(optionName) || "outputs".equals(optionName)) {
                    continue;
                }
                sb.append("        ").append(doubleQuote(optionName)).append(": {\n");
                cst.append("          \"kind\": " + doubleQuote(kind));
                cst.append("          \"type\": " + doubleQuote(type));
                if (defaultValue != null) {
                    cst.append("          \"defaultValue\": " + doubleQuote(safeDefaultValue(defaultValue)));
                }
                if (enumValues != null) {
                    cst.append("          \"enum\": [ " + safeEnumJson(enumValues) + " ]");
                }
                cst.append("          \"description\": " + doubleQuote(safeDescription(description)));
                cst.append("          \"title\": " + doubleQuote(title));
                if ("true".equals(required)) {
                    cst.append("          \"required\": true");
                } else {
                    cst.append("          \"required\": false");
                }
                if ("true".equals(deprecated)) {
                    cst.append("          \"deprecated\": true");
                } else {
                    cst.append("          \"deprecated\": false");
                }
                sb.append(cst.toString());
                sb.append("\n");
                if (it2.hasNext()) {
                    sb.append("        },\n"); // a property
                } else {
                    sb.append("        }\n"); // a property
                }
            }
            sb.append("      }\n"); // properties
            if (it.hasNext()) {
                sb.append("    },\n"); // name
            } else {
                sb.append("    }\n"); // name
            }
            fos.write(sb.toString().getBytes());
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

    private String asTitle(String name) {
        // capitalize the name as tooltip
        return JSonSchemaHelper.asTitle(name);
    }

    private String safeEnumJson(String values) {
        CollectionStringBuffer cst = new CollectionStringBuffer();
        cst.setSeparator(", ");
        for (String v : values.split(",")) {
            cst.append(doubleQuote(v));
        }
        return cst.toString();
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

    private boolean hasLabel(List<Map<String, String>> model, String label) {
        for (Map<String, String> row : model) {
            String entry = row.get("label");
            if (entry != null) {
                return entry.contains(label);
            }
        }
        return false;
    }

    private void writeObjectsToSchema(Map<String, String> objects, String category, String parent, FileOutputStream fos)
            throws IOException {
        Iterator<String> it;
        fos.write(("  \"" + category + "\": {\n").getBytes());
        it = objects.keySet().iterator();
        generateSchema(category, parent, objects, fos, it);
        fos.write("  }".getBytes());
    }

    public void generate() throws MojoFailureException, MojoExecutionException {

        initIcons();

        // we want expression to be first
        Map<String, String> eips = new TreeMap<String, String>();
        Map<String, String> rests = new TreeMap<String, String>();
        Map<String, String> dataformats = new TreeMap<String, String>();
        Map<String, String> languages = new TreeMap<String, String>();
        Map<String, String> components = new TreeMap<String, String>();

        // find the model json files and split into groups
        URL ccDir;
        try {
            ccDir = new URL("file", null, camelCatalogDir.getAbsolutePath());
        } catch (MalformedURLException ex) {
            throw new MojoFailureException("Error loading models from camel-catalog due " + ex.getMessage(), ex);
        }

        camelCatalogExtract(ccDir, "org/apache/camel/catalog/models", (String name, String text) -> {
            // use the model files to split into the groups we use in camelModel.js
            List<Map<String, String>> model = parseJsonSchema("model", text, false);
            if (hasLabel(model, "rest")) {
                rests.put(name, text);
            } else if (hasLabel(model, "dataformat")) {
                dataformats.put(name, text);
            } else if (hasLabel(model, "language")) {
                languages.put(name, text);
            } else {
                eips.put(name, text);
            }
        });

        camelCatalogExtract(ccDir, "org/apache/camel/catalog/components", (String name, String text) -> {
            components.put(name, text);
        });

        if (eips.isEmpty()) {
            getLog().info("Cannot update " + schemaFile + " as no Camel models found in the Apache Camel version");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(schemaFile, false);
            String version = "var _apacheCamelModelVersion = '" + getVersion() + "';\n\n";
            fos.write(version.getBytes());
            fos.write("var _apacheCamelModel =".getBytes());
            fos.write("{\n".getBytes());

            fos.write("  \"definitions\": {\n".getBytes());

            // generate expression first as its special and needed for eips
            generateExpression(languages.keySet(), fos);
            // then followed by the regular eips
            Iterator<String> it = eips.keySet().iterator();
            generateSchema("eips", "model", eips, fos, it);
            fos.write("  },\n".getBytes());

            writeObjectsToSchema(rests, "rests", "model", fos);
            fos.write(",\n".getBytes());
            writeObjectsToSchema(dataformats, "dataformats", "model", fos);
            fos.write(",\n".getBytes());
            writeObjectsToSchema(languages, "languages", "model", fos);
            fos.write(",\n".getBytes());
            writeObjectsToSchema(components, "components", "component", fos);
            fos.write("\n".getBytes());

            fos.write("}\n".getBytes());
            fos.close();

        } catch (Exception e) {
            throw new MojoFailureException("Error writing to file " + schemaFile);
        }

        getLog().info("Assembled Camel models into combined schema: " + schemaFile);
    }
}
