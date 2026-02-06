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
target/eitri-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## ▶️ Usage

### Basic Usage

```bash
java -jar target/eitri-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --src src/main/java \
  --out diagram.puml
```

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

JAR="target/eitri-1.0-SNAPSHOT-jar-with-dependencies.jar"
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

## License

MIT
