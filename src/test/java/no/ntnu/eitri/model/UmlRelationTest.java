package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UmlRelation and its PlantUML rendering.
 */
class UmlRelationTest {
    
    @Nested
    @DisplayName("Arrow symbol mapping")
    class ArrowSymbolMapping {

        @Test
        @DisplayName("EXTENDS: Parent <|-- Child")
        void extendsRelation() {
            UmlRelation relation = UmlRelation.extendsRelation(
                    "com.example.Child",
                    "com.example.Parent"
            );

            // For hierarchy: toType (parent) arrowSymbol fromType (child)
            String result = relation.toPlantUml("Child", "Parent");
            assertEquals("Parent <|-- Child", result);
        }

        @Test
        @DisplayName("IMPLEMENTS: Interface <|.. Implementor")
        void implementsRelation() {
            UmlRelation relation = UmlRelation.implementsRelation(
                    "com.example.OrderRepository",
                    "com.example.Repository"
            );

            String result = relation.toPlantUml("OrderRepository", "Repository");
            assertEquals("Repository <|.. OrderRepository", result);
        }

        @Test
        @DisplayName("COMPOSITION: A *-- B")
        void compositionRelation() {
            UmlRelation relation = UmlRelation.builder()
                    .fromTypeId("com.example.Order")
                    .toTypeId("com.example.LineItem")
                    .kind(RelationKind.COMPOSITION)
                    .build();

            String result = relation.toPlantUml("Order", "LineItem");
            assertEquals("Order *-- LineItem", result);
        }

        @Test
        @DisplayName("AGGREGATION: A o-- B")
        void aggregationRelation() {
            UmlRelation relation = UmlRelation.builder()
                    .fromTypeId("com.example.Department")
                    .toTypeId("com.example.Employee")
                    .kind(RelationKind.AGGREGATION)
                    .build();

            String result = relation.toPlantUml("Department", "Employee");
            assertEquals("Department o-- Employee", result);
        }

        @Test
        @DisplayName("ASSOCIATION: A -- B")
        void associationRelation() {
            UmlRelation relation = UmlRelation.association(
                    "com.example.Customer",
                    "com.example.Order",
                    null
            );

            String result = relation.toPlantUml("Customer", "Order");
            assertEquals("Customer -- Order", result);
        }

        @Test
        @DisplayName("DEPENDENCY: A ..> B")
        void dependencyRelation() {
            UmlRelation relation = UmlRelation.dependency(
                    "com.example.OrderService",
                    "com.example.Repository",
                    null
            );

            String result = relation.toPlantUml("OrderService", "Repository");
            assertEquals("OrderService ..> Repository", result);
        }

        @Test
        @DisplayName("NESTED: Outer +-- Inner : nested")
        void nestedRelation() {
            UmlRelation relation = UmlRelation.nestedRelation(
                    "com.example.Outer",
                    "com.example.Outer$Inner"
            );

            String result = relation.toPlantUml("Outer", "Inner");
            assertEquals("Outer +-- Inner : nested", result);
        }

        @Test
        @DisplayName("NESTED relation has correct kind and label")
        void nestedRelationProperties() {
            UmlRelation relation = UmlRelation.nestedRelation(
                    "com.example.Container",
                    "com.example.Container$Nested"
            );

            assertEquals(RelationKind.NESTED, relation.getKind());
            assertEquals("nested", relation.getLabel());
            assertEquals("com.example.Container", relation.getFromTypeId());
            assertEquals("com.example.Container$Nested", relation.getToTypeId());
        }
    }

    @Nested
    @DisplayName("Labels and multiplicities")
    class LabelsAndMultiplicities {

        @Test
        @DisplayName("Relation with label")
        void relationWithLabel() {
            UmlRelation relation = UmlRelation.dependency(
                    "com.example.OrderService",
                    "com.example.Repository",
                    "uses"
            );

            String result = relation.toPlantUml("OrderService", "Repository");
            assertEquals("OrderService ..> Repository : uses", result);
        }

        @Test
        @DisplayName("Association with multiplicities")
        void associationWithMultiplicities() {
            UmlRelation relation = UmlRelation.builder()
                    .fromTypeId("com.example.Customer")
                    .toTypeId("com.example.Order")
                    .kind(RelationKind.ASSOCIATION)
                    .fromMultiplicity("1")
                    .toMultiplicity("0..*")
                    .label("places")
                    .build();

            String result = relation.toPlantUml("Customer", "Order");
            assertEquals("Customer \"1\" -- \"0..*\" Order : places", result);
        }

        @Test
        @DisplayName("Aggregation with label and multiplicity")
        void aggregationWithAll() {
            UmlRelation relation = UmlRelation.builder()
                    .fromTypeId("com.example.Order")
                    .toTypeId("com.example.Product")
                    .kind(RelationKind.AGGREGATION)
                    .toMultiplicity("1..*")
                    .label("contains")
                    .build();

            String result = relation.toPlantUml("Order", "Product");
            assertEquals("Order o-- \"1..*\" Product : contains", result);
        }

        @Test
        @DisplayName("Hierarchy relation ignores multiplicities")
        void hierarchyIgnoresMultiplicities() {
            UmlRelation relation = UmlRelation.builder()
                    .fromTypeId("com.example.Dog")
                    .toTypeId("com.example.Animal")
                    .kind(RelationKind.EXTENDS)
                    .fromMultiplicity("1")  // Should be ignored
                    .toMultiplicity("1")    // Should be ignored
                    .build();

            String result = relation.toPlantUml("Dog", "Animal");
            // Multiplicities not shown for hierarchy
            assertEquals("Animal <|-- Dog", result);
        }
    }

    @Nested
    @DisplayName("Member-to-member relations")
    class MemberToMemberRelations {

        @Test
        @DisplayName("Field to field relation")
        void fieldToField() {
            UmlRelation relation = UmlRelation.builder()
                    .fromTypeId("com.example.Order")
                    .toTypeId("com.example.Status")
                    .kind(RelationKind.DEPENDENCY)
                    .fromMember("status")
                    .toMember("PAID")
                    .label("transitions")
                    .build();

            String result = relation.toPlantUml("Order", "Status");
            assertEquals("Order::status ..> Status::PAID : transitions", result);
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
