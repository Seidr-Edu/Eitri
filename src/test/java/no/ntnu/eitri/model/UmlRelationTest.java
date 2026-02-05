package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UmlRelation model behavior.
 */
class UmlRelationTest {
    
    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("extendsRelation sets kind and ids")
        void extendsRelation() {
            UmlRelation relation = UmlRelation.extendsRelation(
                    "com.example.Child",
                    "com.example.Parent"
            );

            assertEquals(RelationKind.EXTENDS, relation.getKind());
            assertEquals("com.example.Child", relation.getFromTypeId());
            assertEquals("com.example.Parent", relation.getToTypeId());
        }

        @Test
        @DisplayName("implementsRelation sets kind and ids")
        void implementsRelation() {
            UmlRelation relation = UmlRelation.implementsRelation(
                    "com.example.OrderRepository",
                    "com.example.Repository"
            );

            assertEquals(RelationKind.IMPLEMENTS, relation.getKind());
            assertEquals("com.example.OrderRepository", relation.getFromTypeId());
            assertEquals("com.example.Repository", relation.getToTypeId());
        }

        @Test
        @DisplayName("dependency factory sets label")
        void dependencyRelation() {
            UmlRelation relation = UmlRelation.dependency(
                    "com.example.OrderService",
                    "com.example.Repository",
                    "uses"
            );

            assertEquals(RelationKind.DEPENDENCY, relation.getKind());
            assertEquals("uses", relation.getLabel());
        }

        @Test
        @DisplayName("nestedRelation sets kind/label and ids")
        void nestedRelation() {
            UmlRelation relation = UmlRelation.nestedRelation(
                    "com.example.Outer",
                    "com.example.Outer$Inner"
            );
            assertEquals(RelationKind.NESTED, relation.getKind());
            assertEquals("nested", relation.getLabel());
            assertEquals("com.example.Outer", relation.getFromTypeId());
            assertEquals("com.example.Outer$Inner", relation.getToTypeId());
        }
    }

    @Nested
    @DisplayName("Member-to-member relations")
    class MemberToMemberRelations {

        @Test
        @DisplayName("Member relation stores members")
        void memberRelationStoresMembers() {
            UmlRelation relation = UmlRelation.builder()
                    .fromTypeId("com.example.Order")
                    .toTypeId("com.example.Status")
                    .kind(RelationKind.DEPENDENCY)
                    .fromMember("status")
                    .toMember("PAID")
                    .label("transitions")
                    .build();

            assertEquals("status", relation.getFromMember());
            assertEquals("PAID", relation.getToMember());
            assertEquals("transitions", relation.getLabel());
        }

        @Test
        @DisplayName("isMemberRelation returns true when both members set")
        void isMemberRelation() {
            UmlRelation relation = UmlRelation.builder()
                    .fromTypeId("A")
                    .toTypeId("B")
                    .kind(RelationKind.ASSOCIATION)
                    .fromMember("x")
                    .toMember("y")
                    .build();

            assertTrue(relation.isMemberRelation());
        }

        @Test
        @DisplayName("isMemberRelation returns false when only one member set")
        void isNotMemberRelation() {
            UmlRelation relation = UmlRelation.builder()
                    .fromTypeId("A")
                    .toTypeId("B")
                    .kind(RelationKind.ASSOCIATION)
                    .fromMember("x")
                    .build();

            assertFalse(relation.isMemberRelation());
        }
    }

    @Nested
    @DisplayName("Deduplication")
    class Deduplication {

        @Test
        @DisplayName("Hierarchy relations have ordered dedup key")
        void hierarchyDedup() {
            UmlRelation r1 = UmlRelation.extendsRelation("Child", "Parent");
            UmlRelation r2 = UmlRelation.extendsRelation("Child", "Parent");

            assertEquals(r1.getDeduplicationKey(), r2.getDeduplicationKey());
        }

        @Test
        @DisplayName("Non-hierarchy relations normalize order")
        void nonHierarchyDedup() {
            UmlRelation r1 = UmlRelation.association("A", "B", null);
            UmlRelation r2 = UmlRelation.association("B", "A", null);

            // Both should have same dedup key (normalized)
            assertEquals(r1.getDeduplicationKey(), r2.getDeduplicationKey());
        }

        @Test
        @DisplayName("Different kinds have different dedup keys")
        void differentKindsDedup() {
            UmlRelation assoc = UmlRelation.association("A", "B", null);
            UmlRelation dep = UmlRelation.dependency("A", "B", null);

            assertNotEquals(assoc.getDeduplicationKey(), dep.getDeduplicationKey());
        }
    }
}
