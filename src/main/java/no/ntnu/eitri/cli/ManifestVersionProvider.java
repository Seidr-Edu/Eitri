package no.ntnu.eitri.cli;

import picocli.CommandLine.IVersionProvider;

/**
 * Provides version information from the JAR manifest.
 */
public class ManifestVersionProvider implements IVersionProvider {
    @Override
    public String[] getVersion() {
        String version = getClass().getPackage().getImplementationVersion();
        return new String[] { version != null ? version : "Unknown version" };
    }
}