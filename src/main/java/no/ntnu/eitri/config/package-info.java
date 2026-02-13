/**
 * Configuration layer for Eitri.
 * <p>
 * Runtime concerns are modeled by {@link no.ntnu.eitri.config.RunConfig} and
 * PlantUML writer concerns are modeled by {@link no.ntnu.eitri.config.PlantUmlConfig}.
 * </p>
 *
 * <h2>Core Classes</h2>
 * <ul>
 *   <li>{@link no.ntnu.eitri.config.RunConfig} - CLI/runtime configuration</li>
 *   <li>{@link no.ntnu.eitri.config.PlantUmlConfig} - PlantUML writer settings</li>
 *   <li>{@link no.ntnu.eitri.config.ConfigService} - Resolves and validates configuration</li>
 *   <li>{@link no.ntnu.eitri.config.ConfigLoader} - Strict YAML loader for writer settings</li>
 *   <li>{@link no.ntnu.eitri.config.ConfigResolution} - Combined run + writer config result</li>
 *   <li>{@link no.ntnu.eitri.config.LayoutDirection} - Diagram layout direction enum</li>
 *   <li>{@link no.ntnu.eitri.config.ConfigException} - Configuration error exception</li>
 * </ul>
 *
 * <h2>Configuration Resolution Order</h2>
 * <ol>
 *   <li>Core runtime settings from CLI</li>
 *   <li>Optional writer settings from explicit --config</li>
 *   <li>Optional writer settings from .eitri.config.yaml in working directory</li>
 * </ol>
 */
package no.ntnu.eitri.config;
