package no.ntnu.eitri.parser;

import no.ntnu.eitri.parser.resolution.TypeReferenceResolver;
import no.ntnu.eitri.parser.resolution.TypeRegistry;
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
        assertNull(resolver.resolveTypeReference("? extends Number"));
        assertNull(resolver.resolveTypeReference("? super Number"));
    }

    @Test
    void resolveTypeReferenceSkipsUnqualifiedTypeNames() {
        TypeReferenceResolver resolver = new TypeReferenceResolver(new TypeRegistry());

        assertNull(resolver.resolveTypeReference("C"));
        assertNull(resolver.resolveTypeReference("Number"));
    }
}
