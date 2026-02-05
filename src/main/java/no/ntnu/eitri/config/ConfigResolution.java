package no.ntnu.eitri.config;

import java.nio.file.Path;

/**
 * Result of configuration resolution.
 */
public record ConfigResolution(EitriConfig config, Path configFileUsed) {}
