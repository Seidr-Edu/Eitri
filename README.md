# Eitri

**Source code parser that forges UML diagrams.**

![](https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNzE3ZWM0eTQ1MXc4ZHFpanlrdmw3YTJyM2Y0dGZ2MngxOXQzeDJrNCZlcD12MV9naWZzX3NlYXJjaCZjdD1n/l3mZ67L0776T759uM/giphy.gif)

## Overview

Eitri is a CLI tool that parses source directories and generates class diagrams **without compiling or running the project**. It resolves the parser and writer through registries, keyed by file extension. By default, `.java` uses [JavaParser](https://javaparser.org/) and output is written as PlantUML (`.puml`).

### Features

- **Type extraction**: Classes, interfaces, enums, annotations, records
- **Member extraction**: Fields, methods, constructors with full signatures
- **Relationship detection**:
  - Inheritance (`extends`) and implementation (`implements`)
  - Composition, aggregation, association (from fields)
  - Dependencies (from method parameters/return types)
- **Package grouping**: Types organized by package in the diagram
- **Pluggable pipeline**: Parsers and writers are discovered via registries (file extension based)
- **Highly configurable**: Filter members by visibility, hide/show relation types, customize layout
- **YAML configuration**: Store settings in a config file for reproducibility
- **Multiple source paths**: Combine sources from multiple directories

## Dependencies

- **Java 25** or later
- **Maven** for building
- [JavaParser](https://github.com/javaparser/javaparser) with symbol solver (bundled default parser)
- [picocli](https://picocli.info/) for CLI (bundled)
- [SnakeYAML](https://github.com/snakeyaml/snakeyaml) for config files (bundled)

Optional: Install Graphviz to render diagrams locally:

```bash
brew install graphviz  # macOS
apt install graphviz   # Debian/Ubuntu
```

## 📦 Build

```bash
mvn clean package
```

This produces an executable uber-jar:

```
target/eitri-1.0-jar-with-dependencies.jar
```

## 🐳 Container Image (GHCR)

Images are published to GitHub Container Registry on every published GitHub Release:

- `ghcr.io/Seidr-Edu/eitri:<release-tag>`
- `ghcr.io/Seidr-Edu/eitri:latest`

Pull the image:

```bash
docker pull ghcr.io/Seidr-Edu/eitri:latest
```

The container entrypoint is service mode (`eitri-service.sh`). For direct CLI use,
override the entrypoint:

```bash
docker run --rm --entrypoint java \
  -v "$PWD/src/main/java:/work/input:ro" \
  -v "$PWD/output:/work/output" \
  ghcr.io/Seidr-Edu/eitri:latest \
  -jar /app/eitri.jar \
  --src /work/input \
  --out /work/output/diagram.puml
```

If the package is private on first publish, set it to public in GitHub Packages before using it from external orchestrators.

## Service Mode

Service mode is the production container contract for the pipeline step. The
service reads `/run/config/manifest.yaml`, consumes `/input/repo` read-only, and
writes all canonical outputs under `/run`.

### Manifest schema

```yaml
version: 1
run_id: 20260312T080000Z__example       # Optional. Auto-generated if absent.
source_relpaths:                        # Required. One or more paths relative to /input/repo.
  - src/main/java
  - shared/src/main/java
parser_extension: .java                # Optional.
writer_extension: .puml                # Optional.
verbose: false                         # Optional. Default false.
writers:                               # Optional. Passed through to Eitri config as-is.
  plantuml:
    diagramName: diagram
    hidePrivate: true
```

Validation rules:

- `version` is required and must be `1`.
- `source_relpaths` is required and must be a non-empty array of strings.
- `parser_extension` and `writer_extension` must be strings if present.
- `verbose` must be a boolean if present.
- `writers` must be a mapping/object if present.
- Unknown top-level manifest keys are rejected.
- `source_relpaths` must stay within `/input/repo`; absolute paths, `..`, and `:` are rejected.

The wrapper preserves Eitri's current PlantUML configurability by materializing
the `writers` subtree into a temporary `.eitri.config.yaml` before invoking the
existing CLI runner.

### Environment variables

| Variable | Purpose |
|---|---|
| `EITRI_MANIFEST` | Override manifest path (default `/run/config/manifest.yaml`) |

### Runtime mounts

| Host path | Container path | Access | Purpose |
|---|---|---|---|
| `runs/<runId>/services/eitri/input/repo` | `/input/repo` | read-only | Staged repository snapshot |
| `runs/<runId>/services/eitri/config` | `/run/config` | read-only | Manifest file |
| `runs/<runId>/services/eitri/run` | `/run` | read-write | Canonical outputs |

Do not mount the whole run tree into the container. Only mount the staged repo,
manifest directory, and writable run directory.

### Canonical outputs

| Path | Description |
|---|---|
| `/run/artifacts/model/diagram.puml` | Generated PlantUML diagram |
| `/run/artifacts/model/diagram_v2.puml` | Slightly degraded PlantUML diagram variant |
| `/run/artifacts/model/diagram_v3.puml` | Moderately degraded PlantUML diagram variant |
| `/run/artifacts/model/logs/` | Service log and materialized config |
| `/run/outputs/run_report.json` | Machine-readable service report (`eitri_service_report.v1`) |
| `/run/outputs/summary.md` | Human-readable run summary |

### Service report schema

The report always includes at least:

```json
{
  "service_schema_version": "eitri_service_report.v1",
  "run_id": "20260312T080000Z__example",
  "status": "passed | error",
  "reason": null,
  "status_detail": null,
  "started_at": "2026-03-12T08:00:00Z",
  "finished_at": "2026-03-12T08:00:02Z",
  "type_count": 2,
  "relation_count": 0,
  "inputs": {
    "source_root": "/input/repo",
    "source_relpaths": ["src/main/java", "shared/src/main/java"],
    "parser_extension": ".java",
    "writer_extension": ".puml",
    "verbose": false
  },
  "artifacts": {
    "diagram_path": "/run/artifacts/model/diagram.puml",
    "diagram_v2_path": "/run/artifacts/model/diagram_v2.puml",
    "diagram_v3_path": "/run/artifacts/model/diagram_v3.puml",
    "logs_dir": "/run/artifacts/model/logs"
  },
  "degradation": {
    "variants": [
      {
        "variant": "diagram_v2",
        "diagram_path": "/run/artifacts/model/diagram_v2.puml",
        "percentage": 8,
        "minimum": 2,
        "eligible_candidate_count": 12,
        "applied_count": 2,
        "applied": [
          {
            "kind": "omit_field",
            "owner_fqn": "demo.Service",
            "target": "cache",
            "detail": "Removed field cache : Cache"
          }
        ]
      }
    ]
  }
}
```

If manifest validation or Eitri execution fails, the service still writes
`run_report.json` and `summary.md` unless `/run` is not writable. The
orchestrator should branch on `run_report.json.status`, not on the process exit
code.

### Exit codes

| Code | Meaning |
|---|---|
| `0` | `run_report.json` was emitted |
| `1` | The service could not emit a report, usually because `/run` was not writable |

### Example `docker run`

```bash
docker run --rm \
  -e EITRI_MANIFEST=/run/config/manifest.yaml \
  -v /srv/pipeline/runs/<runId>/services/eitri/input/repo:/input/repo:ro \
  -v /srv/pipeline/runs/<runId>/services/eitri/config:/run/config:ro \
  -v /srv/pipeline/runs/<runId>/services/eitri/run:/run \
  ghcr.io/Seidr-Edu/eitri:latest
```

### Orchestrator integration

1. Stage the repository snapshot at `runs/<runId>/services/eitri/input/repo/`.
2. Write `runs/<runId>/services/eitri/config/manifest.yaml`.
3. Launch the container with `/input/repo`, `/run/config`, and `/run`.
4. Consume `runs/<runId>/services/eitri/run/outputs/run_report.json`.
5. Consume `runs/<runId>/services/eitri/run/artifacts/model/diagram.puml`.
6. Use `diagram_v2.puml` and `diagram_v3.puml` when running degraded-diagram pipeline experiments.

## ▶️ Usage

### Basic Usage

```bash
java -jar target/eitri-1.0-jar-with-dependencies.jar \
  --src src/main/java \
  --out diagram.puml
```

A successful non-dry CLI run now writes these sibling artifacts beside the
requested output path:

- `diagram.puml`: best-effort canonical diagram
- `diagram_v2.puml`: slightly degraded variant
- `diagram_v3.puml`: moderately degraded variant
- `run_report.json`: local machine-readable report with degradation details
- `summary.md`: local human-readable summary

### Choose Parser and Writer

Parsers and writers are selected by extension id. These are resolved through the registries (built-ins plus any ServiceLoader-provided implementations).

```bash
java -jar eitri.jar \
  --src src/main/java \
  --out diagram.puml \
  --parser .java \
  --writer .puml
```

### Multiple Source Directories

```bash
java -jar eitri.jar \
  --src src/main/java \
  --src ../shared-lib/src/main/java \
  --out combined-diagram.puml
```

### With Filtering Options

```bash
java -jar eitri.jar \
  --src src/main/java \
  --out diagram.puml \
  --hide-private \
  --hide-dependency \
  --hide-unlinked
```

### CLI Options

```
Usage: eitri [OPTIONS] -s=<path> [-s=<path>]... -o=<file>
```

Run `java -jar eitri.jar --help` for full option list.

## ⚙️ YAML Configuration

Create an `.eitri.config.yaml` file for reusable settings. See `.eitri.config.example.yaml` for an example.

To choose a parser or writer in config, set the extension ids:

```yaml
parserExtension: .java
writerExtension: .puml
```

Use with:

```bash
java -jar eitri.jar --config eitri.yaml --src src/main/java --out diagram.puml
```

CLI arguments override YAML settings.

## 📊 Output Example

Running Eitri on its own source code produces:

```
Generated diagram.puml with 27 types and 55 relations.
```

The output includes package grouping:

```
@startuml diagram

hide empty members

package no.ntnu.eitri.model {
  +class UmlType { ... }
  +class UmlField { ... }
  +enum Visibility { ... }
}

package no.ntnu.eitri.parser {
  +interface SourceParser { ... }
  +class ParseContext { ... }
}

package no.ntnu.eitri.parser.java {
  +class JavaSourceParser { ... }
}

SourceParser <|.. JavaSourceParser
UmlType o-- "*" UmlField
...

@enduml
```

## 🌀 Batch Processing

Process all projects in a directory:

```bash
#!/usr/bin/env bash

JAR="target/eitri-1.0-jar-with-dependencies.jar"
PROJECTS_ROOT="/path/to/projects"
OUT_ROOT="/path/to/diagrams"

mkdir -p "$OUT_ROOT"

for project in "$PROJECTS_ROOT"/*; do
  name=$(basename "$project")
  src="$project/src/main/java"
  out="$OUT_ROOT/$name.puml"

  if [ -d "$src" ]; then
    echo "Processing $name"
    java -jar "$JAR" --src "$src" --out "$out" --hide-private --hide-dependency
  else
    echo "Skipping $name (no src/main/java)"
  fi
done
```

## 🔧 Viewing Diagrams

Open `.puml` files with:

- **IntelliJ IDEA**: PlantUML integration plugin
- **VS Code**: PlantUML extension
- **Command line**: `java -jar plantuml.jar diagram.puml` (generates PNG/SVG)
- **Online**: [PlantUML Web Server](http://www.plantuml.com/plantuml)

## Known Limitations

- The Java parser intentionally skips directory trees named `test` and `tests` while scanning sources. This is a convention-based heuristic to avoid test code in diagrams, but it can also skip production code if a project uses those names for non-test source directories.
- Symbol solving is best-effort. During parsing, JavaParser may log warnings like `Failed to add jar type solver for ...` for some classpath/dependency jars (for example certain test/runtime artifacts). These warnings do not fail the run; unsupported jars are skipped and parsing continues.
- Type declarations use FQNs directly in PlantUML output (with package grouping blocks), rather than only simple class names to ensure no conflict between the same class name across packages.

## License

MIT
