/**
 * Parser abstraction layer for converting source code into UML model.
 * 
 * <p>This package provides an abstraction over different source parsers,
 * allowing Eitri to support multiple input formats (Java, Kotlin, etc.)
 * through a unified interface.
 * 
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link no.ntnu.eitri.parser.SourceParser} - Main interface for parsing source files</li>
 *   <li>{@link no.ntnu.eitri.parser.ParseContext} - Mutable accumulator for parsed data</li>
 *   <li>{@link no.ntnu.eitri.parser.ParseReport} - Consolidated warnings and parse metrics</li>
 *   <li>{@link no.ntnu.eitri.parser.ParseException} - Exception for parsing errors</li>
 * </ul>
 */
package no.ntnu.eitri.parser;
