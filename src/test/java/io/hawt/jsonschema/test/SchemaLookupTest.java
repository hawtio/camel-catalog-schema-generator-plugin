package io.hawt.jsonschema.test;

import io.hawt.jsonschema.SchemaLookup;
import io.hawt.jsonschema.test.objects.ObjectWithTransientModifiers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SchemaLookupTest {

    @Test
    public void testLookupSchemaForJavaLangString() {
        SchemaLookup lookup = createSchemaLookup();
        String result = lookup.getSchemaForClass("java.lang.String");
        assertNotNull(result);
    }


    @Test
    public void testFailedLookup() {
        SchemaLookup lookup = createSchemaLookup();
        try {
            lookup.getSchemaForClass("James");
            fail("Should have thrown a NoClassDefFoundException");
        } catch (Exception e) {
            // pass
        }
    }

    protected SchemaLookup createSchemaLookup() {
        SchemaLookup lookup = new SchemaLookup();
        lookup.init();
        return lookup;
    }

    // right now these just verify the lookup doesn't bail
    @Test
    public void testObjectWithJaxbAnnotations() {
        SchemaLookup lookup = createSchemaLookup();
        String result = lookup.getSchemaForClass("io.hawt.jsonschema.test.objects.ObjectWithJaxbAnnotations");
        System.out.println("testObjectWithJaxbAnnotations - Got: \n\n" + result + "\n\n");
        assertTrue(result.contains("\"default\" : \"default-value\""));
    }

    @Test
    public void testObjectWithValidationAnnotations() {
        SchemaLookup lookup = createSchemaLookup();
        String result = lookup.getSchemaForClass("io.hawt.jsonschema.test.objects.ObjectWithValidationAnnotations");
        System.out.println("testObjectWithValidationAnnotations - Got: \n\n" + result + "\n\n");
    }

    @Test
    public void testObjectWithJsonAnnotations() {
        SchemaLookup lookup = createSchemaLookup();
        String result = lookup.getSchemaForClass("io.hawt.jsonschema.test.objects.ObjectWithJsonAnnotations");
        System.out.println("testObjectWithJsonAnnotations - Got: \n\n" + result + "\n\n");
    }

    @Test
    public void testObjectWithTransientModifiers() {
        SchemaLookup lookup = createSchemaLookup();
        String result = lookup.getSchemaForClass(ObjectWithTransientModifiers.class.getName());
        System.out.println("testObjectWithTransientModifiers - Got: \n\n" + result + "\n\n");
    }

}
