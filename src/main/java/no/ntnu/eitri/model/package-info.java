/**
 * Internal UML model for Eitri.
 * <p>
 * This package contains the domain model for representing UML class diagrams.
 * It is independent of both the parser (JavaParser) and the writer (PlantUML).
 * </p>
 *
 * <h2>Core Types</h2>
 * <ul>
 *   <li>{@link no.ntnu.eitri.model.UmlModel} - Root container for the diagram</li>
 *   <li>{@link no.ntnu.eitri.model.UmlType} - A class, interface, enum, annotation, or record</li>
 *   <li>{@link no.ntnu.eitri.model.UmlField} - A field in a type</li>
 *   <li>{@link no.ntnu.eitri.model.UmlMethod} - A method or constructor in a type</li>
 *   <li>{@link no.ntnu.eitri.model.UmlParameter} - A method parameter</li>
 *   <li>{@link no.ntnu.eitri.model.UmlRelation} - A relationship between types</li>
 * </ul>
 *
 * <h2>Supporting Types</h2>
 * <ul>
 *   <li>{@link no.ntnu.eitri.model.UmlStereotype} - Stereotype annotation on a type</li>
 *   <li>{@link no.ntnu.eitri.model.UmlGeneric} - Generic type parameter</li>
 *   <li>{@link no.ntnu.eitri.model.UmlNote} - Note attached to an element</li>
 * </ul>
 *
 * <h2>Enums</h2>
 * <ul>
 *   <li>{@link no.ntnu.eitri.model.Visibility} - PUBLIC, PRIVATE, PROTECTED, PACKAGE</li>
 *   <li>{@link no.ntnu.eitri.model.TypeKind} - CLASS, ABSTRACT_CLASS, INTERFACE, ENUM, ANNOTATION, RECORD</li>
 *   <li>{@link no.ntnu.eitri.model.RelationKind} - EXTENDS, IMPLEMENTS, COMPOSITION, AGGREGATION, ASSOCIATION, DEPENDENCY</li>
 *   <li>{@link no.ntnu.eitri.model.Modifier} - STATIC, ABSTRACT, FINAL</li>
 * </ul>
 */
package no.ntnu.eitri.model;
