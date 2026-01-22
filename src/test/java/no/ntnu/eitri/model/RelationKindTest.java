package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RelationKind enum and arrow symbol rendering.
 */
class RelationKindTest {

    @Test
    @DisplayName("EXTENDS renders as <|--")
    void extendsArrow() {
        assertEquals("<|--", RelationKind.EXTENDS.toArrowSymbol());
    }

    @Test
    @DisplayName("IMPLEMENTS renders as <|..")
    void implementsArrow() {
        assertEquals("<|..", RelationKind.IMPLEMENTS.toArrowSymbol());
    }

    @Test
    @DisplayName("COMPOSITION renders as *--")
    void compositionArrow() {
        assertEquals("*--", RelationKind.COMPOSITION.toArrowSymbol());
    }

    @Test
    @DisplayName("AGGREGATION renders as o--")
    void aggregationArrow() {
        assertEquals("o--", RelationKind.AGGREGATION.toArrowSymbol());
    }

    @Test
    @DisplayName("ASSOCIATION renders as --")
    void associationArrow() {
        assertEquals("--", RelationKind.ASSOCIATION.toArrowSymbol());
    }

    @Test
    @DisplayName("DEPENDENCY renders as ..>")
    void dependencyArrow() {
        assertEquals("..>", RelationKind.DEPENDENCY.toArrowSymbol());
    }

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
}
