package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RelationKind semantic behavior.
 */
class RelationKindTest {

    @Test
    @DisplayName("EXTENDS and IMPLEMENTS are hierarchy relations")
    void hierarchyRelations() {
        assertTrue(RelationKind.EXTENDS.isHierarchy());
        assertTrue(RelationKind.IMPLEMENTS.isHierarchy());
    }

    @Test
    @DisplayName("Non-hierarchy relations")
    void nonHierarchyRelations() {
        assertFalse(RelationKind.COMPOSITION.isHierarchy());
        assertFalse(RelationKind.AGGREGATION.isHierarchy());
        assertFalse(RelationKind.ASSOCIATION.isHierarchy());
        assertFalse(RelationKind.DEPENDENCY.isHierarchy());
    }

    @Test
    @DisplayName("IMPLEMENTS and DEPENDENCY use dotted lines")
    void dottedLineRelations() {
        assertTrue(RelationKind.IMPLEMENTS.isDotted());
        assertTrue(RelationKind.DEPENDENCY.isDotted());
    }

    @Test
    @DisplayName("Solid line relations")
    void solidLineRelations() {
        assertFalse(RelationKind.EXTENDS.isDotted());
        assertFalse(RelationKind.COMPOSITION.isDotted());
        assertFalse(RelationKind.AGGREGATION.isDotted());
        assertFalse(RelationKind.ASSOCIATION.isDotted());
    }

    @Test
    @DisplayName("NESTED is not a hierarchy relation")
    void nestedNotHierarchy() {
        assertFalse(RelationKind.NESTED.isHierarchy());
    }

    @Test
    @DisplayName("NESTED is a nesting relation")
    void nestedIsNesting() {
        assertTrue(RelationKind.NESTED.isNesting());
    }

    @Test
    @DisplayName("Non-NESTED relations are not nesting")
    void nonNestedNotNesting() {
        assertFalse(RelationKind.EXTENDS.isNesting());
        assertFalse(RelationKind.IMPLEMENTS.isNesting());
        assertFalse(RelationKind.COMPOSITION.isNesting());
        assertFalse(RelationKind.AGGREGATION.isNesting());
        assertFalse(RelationKind.ASSOCIATION.isNesting());
        assertFalse(RelationKind.DEPENDENCY.isNesting());
    }

    @Test
    @DisplayName("NESTED uses solid line")
    void nestedSolidLine() {
        assertFalse(RelationKind.NESTED.isDotted());
    }
}
