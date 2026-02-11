package no.ntnu.eitri.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeReferenceResolverTest {

    @Test
    void resolveTypeReferenceNormalizesAndCreatesPlaceholder() {
        TypeRegistry registry = new TypeRegistry();
        TypeReferenceResolver resolver = new TypeReferenceResolver(registry);

        String resolved = resolver.resolveTypeReference(" com.example.Order<java.lang.String>[] ");

        assertEquals("com.example.Order", resolved);
        assertTrue(registry.hasType("com.example.Order"));
        assertEquals("Order", registry.getType("com.example.Order").getSimpleName());
    }

    @Test
    void resolveTypeReferenceSkipsPrimitiveAndWildcardTypes() {
        TypeReferenceResolver resolver = new TypeReferenceResolver(new TypeRegistry());

        assertNull(resolver.resolveTypeReference("int"));
        assertNull(resolver.resolveTypeReference("?"));
        assertEquals("Number", resolver.resolveTypeReference("? extends Number"));
        assertEquals("Number", resolver.resolveTypeReference("? super Number"));
    }
}
