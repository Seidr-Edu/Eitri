package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeKindTest {

    @Test
    @DisplayName("Abstract kinds are abstract")
    void abstractKinds() {
        assertTrue(TypeKind.ABSTRACT_CLASS.isAbstract());
        assertTrue(TypeKind.INTERFACE.isAbstract());
    }

    @Test
    @DisplayName("Non-abstract kinds are not abstract")
    void nonAbstractKinds() {
        assertFalse(TypeKind.CLASS.isAbstract());
        assertFalse(TypeKind.ENUM.isAbstract());
        assertFalse(TypeKind.ANNOTATION.isAbstract());
        assertFalse(TypeKind.RECORD.isAbstract());
    }

    @Test
    @DisplayName("Exactly two type kinds are abstract")
    void abstractKindCount() {
        long abstractCount = java.util.Arrays.stream(TypeKind.values())
                .filter(TypeKind::isAbstract)
                .count();
        assertEquals(2, abstractCount);
    }
}
