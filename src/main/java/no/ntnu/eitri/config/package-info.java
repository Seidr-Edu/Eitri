/**
 * Configuration layer for Eitri.
 * <p>
 * This package contains the configuration system for the Eitri CLI tool,
 * supporting both command-line arguments and YAML configuration files.
 * </p>
 *
 * <h2>Core Classes</h2>
 * <ul>
 *   <li>{@link no.ntnu.eitri.config.EitriConfig} - Central configuration object</li>
 *   <li>{@link no.ntnu.eitri.config.ConfigLoader} - Loads and merges configuration</li>
 *   <li>{@link no.ntnu.eitri.config.LayoutDirection} - Diagram layout direction enum</li>
 *   <li>{@link no.ntnu.eitri.config.ConfigException} - Configuration error exception</li>
 * </ul>
 *
 * <h2>Configuration Resolution Order</h2>
 * <ol>
 *   <li>Built-in defaults</li>
 *   <li>.eitri.config.yaml in working directory</li>
 *   <li>Explicit --config file</li>
 *   <li>CLI flags (highest priority)</li>
 * </ol>
 *
 * <h2>Example YAML Configuration</h2>
 * <pre>
 * layout:
 *   direction: left-to-right
 *   groupInheritance: 2
 *
 * visibility:
 *   hidePrivate: true
 *
 * members:
 *   hideEmptyMembers: true
 *
 * display:
 *   hideCircle: false
 *   showStereotypes: true
 *
 * relations:
 *   showDependency: false
 * </pre>
 */
package no.ntnu.eitri.config;
