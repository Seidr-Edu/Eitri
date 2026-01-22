/**
 * JavaParser-based implementation of the source parser.
 * 
 * <p>This package provides Java source file parsing using the JavaParser library,
 * extracting type declarations, members, and relationships to build a UML model.
 * 
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link no.ntnu.eitri.parser.java.JavaSourceParser} - Main parser implementation</li>
 *   <li>{@link no.ntnu.eitri.parser.java.TypeVisitor} - AST visitor for type declarations</li>
 *   <li>{@link no.ntnu.eitri.parser.java.MemberExtractor} - Field and method extraction</li>
 *   <li>{@link no.ntnu.eitri.parser.java.RelationDetector} - Relationship detection</li>
 * </ul>
 */
package no.ntnu.eitri.parser.java;
