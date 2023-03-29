package io.hawt.jsonschema.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import io.hawt.jsonschema.SchemaLookup;
import io.hawt.jsonschema.test.objects.ObjectWithTransientModifiers;

public class SchemaLookupTest {

    @Test
    public void testLookupSchemaForJavaLangString() throws Exception {
        SchemaLookup lookup = createSchemaLookup();
        String result = lookup.getSchemaForClass("java.lang.String");
        assertNotNull(result);
    }

    /*
     * Requires bringing fabric into the classpath, so let's omit this one
     * 
     * @Test
     * 
     * @Ignore public void testLookupMoreInterestingSchema() throws Exception {
     * SchemaLookup lookup = createSchemaLookup(); String result =
     * lookup.getSchemaForClass("io.fabric8.service.ssh.CreateSshContainerOptions");
     * System.out.println("testLookupMoreInterestingSchema - Got: \n\n" + result +
     * "\n\n"); }
     */

    @Test
    public void testFailedLookup() throws Exception {
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
    public void testObjectWithJaxbAnnotations() throws Exception {
        SchemaLookup lookup = createSchemaLookup();
        String result = lookup.getSchemaForClass("io.hawt.jsonschema.test.objects.ObjectWithJaxbAnnotations");
        System.out.println("testObjectWithJaxbAnnotations - Got: \n\n" + result + "\n\n");
        assertTrue(result.contains("\"default\" : \"default-value\""));
    }

    @Test
    public void testObjectWithValidationAnnotations() throws Exception {
        SchemaLookup lookup = createSchemaLookup();
        String result = lookup.getSchemaForClass("io.hawt.jsonschema.test.objects.ObjectWithValidationAnnotations");
        System.out.println("testObjectWithValidationAnnotations - Got: \n\n" + result + "\n\n");
    }

    @Test
    public void testObjectWithJsonAnnotations() throws Exception {
        SchemaLookup lookup = createSchemaLookup();
        String result = lookup.getSchemaForClass("io.hawt.jsonschema.test.objects.ObjectWithJsonAnnotations");
        System.out.println("testObjectWithJsonAnnotations - Got: \n\n" + result + "\n\n");
    }

    @Test
    public void testObjectWithTransientModifiers() throws Exception {
        SchemaLookup lookup = createSchemaLookup();
        String result = lookup.getSchemaForClass(ObjectWithTransientModifiers.class.getName());
        System.out.println("testObjectWithTransientModifiers - Got: \n\n" + result + "\n\n");
    }

}
