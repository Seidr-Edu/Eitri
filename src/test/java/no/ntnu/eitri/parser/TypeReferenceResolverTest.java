package no.ntnu.eitri.parser;

import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.parser.resolution.TypeReferenceResolver;
import no.ntnu.eitri.parser.resolution.TypeRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TypeReferenceResolverTest {

    @Test
    void resolveTypeReferenceNormalizesAndResolvesRegisteredType() {
        TypeRegistry registry = new TypeRegistry();
        registry.addType(UmlType.builder().fqn("com.example.Order").simpleName("Order").build());
        TypeReferenceResolver resolver = new TypeReferenceResolver(registry);

        String resolved = resolver.resolveTypeReference(" com.example.Order<java.lang.String>[] ");

        assertEquals("com.example.Order", resolved);
    }

    @Test
    void resolveTypeReferenceSkipsUnregisteredFqn() {
        TypeReferenceResolver resolver = new TypeReferenceResolver(new TypeRegistry());

        assertNull(resolver.resolveTypeReference("com.example.Unknown"));
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

    @Test
    void resolveTypeReferenceRejectsInnerClassStyleNames() {
        TypeRegistry registry = new TypeRegistry();
        TypeReferenceResolver resolver = new TypeReferenceResolver(registry);

        // Names like JCommander.Builder start with uppercase — not a valid FQN
        assertNull(resolver.resolveTypeReference("JCommander.Builder"));
        assertNull(resolver.resolveTypeReference("LogHelper.LogLevelEnum"));
        assertNull(resolver.resolveTypeReference("Outer.Inner.Deep"));
    }

    @Test
    void resolveTypeReferenceAcceptsUnconventionalPackageNames() {
        TypeRegistry registry = new TypeRegistry();
        // Package names like example.cli are valid even without com. prefix
        registry.addType(UmlType.builder().fqn("example.cli.JadxCLI").simpleName("JadxCLI").build());
        TypeReferenceResolver resolver = new TypeReferenceResolver(registry);

        assertEquals("example.cli.JadxCLI", resolver.resolveTypeReference("example.cli.JadxCLI"));
    }

    @Test
    void resolveTypeReferenceTracksSkippedUnknownFqnStats() {
        TypeRegistry registry = new TypeRegistry();
        registry.addType(UmlType.builder().fqn("com.example.Known").simpleName("Known").build());
        TypeReferenceResolver resolver = new TypeReferenceResolver(registry);

        assertEquals("com.example.Known", resolver.resolveTypeReference("com.example.Known"));
        assertNull(resolver.resolveTypeReference("com.external.Unknown"));

        var stats = resolver.getStatsSnapshot();
        assertEquals(2, stats.totalRequests());
        assertEquals(1, stats.resolvedReferences());
        assertEquals(1, stats.reusedKnownTypes());
        assertEquals(1, stats.skippedUnknownFqn());
    }

    @Test
    void normalizeToValidFqnReturnsNormalizedFqnWithoutRegistryCheck() {
        TypeReferenceResolver resolver = new TypeReferenceResolver(new TypeRegistry());

        assertEquals("com.example.Unknown", resolver.normalizeToValidFqn("com.example.Unknown"));
        assertEquals("java.util.List", resolver.normalizeToValidFqn("java.util.List<com.example.Foo>"));
        assertEquals("com.example.Order", resolver.normalizeToValidFqn(" com.example.Order[] "));
    }

    @Test
    void normalizeToValidFqnRejectsInvalidInputs() {
        TypeReferenceResolver resolver = new TypeReferenceResolver(new TypeRegistry());

        assertNull(resolver.normalizeToValidFqn(null));
        assertNull(resolver.normalizeToValidFqn(""));
        assertNull(resolver.normalizeToValidFqn("int"));
        assertNull(resolver.normalizeToValidFqn("?"));
        assertNull(resolver.normalizeToValidFqn("C"));
        assertNull(resolver.normalizeToValidFqn("String"));
        assertNull(resolver.normalizeToValidFqn("JCommander.Builder"));
    }

    @Test
    void normalizeToValidFqnRejectsCommaAndSpaceSeparatedTypes() {
        TypeReferenceResolver resolver = new TypeReferenceResolver(new TypeRegistry());

        assertNull(resolver.normalizeToValidFqn("java.lang.String, java.lang.String"));
        assertNull(resolver.normalizeToValidFqn("java.lang.String java.lang.Integer"));
    }
}
