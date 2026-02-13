package no.ntnu.eitri.config;

import java.nio.file.Path;

/**
 * Result of configuration resolution.
 */
public record ConfigResolution(RunConfig runConfig, PlantUmlConfig plantUmlConfig, Path configFileUsed) {}
