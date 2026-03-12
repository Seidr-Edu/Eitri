package no.ntnu.eitri.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validated service manifest data used by the Eitri service wrapper.
 */
record EitriServiceManifest(
        String runId,
        List<String> sourceRelpaths,
        String parserExtension,
        String writerExtension,
        boolean verbose,
        Map<String, Object> writers) {

    EitriServiceManifest {
        sourceRelpaths = sourceRelpaths != null ? List.copyOf(sourceRelpaths) : List.of();
        writers = writers != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(writers))
                : Map.of();
    }

    boolean hasWriterConfig() {
        return !writers.isEmpty();
    }

    static EitriServiceManifest empty(String runId) {
        return new EitriServiceManifest(runId, new ArrayList<>(), null, null, false, new LinkedHashMap<>());
    }
}
