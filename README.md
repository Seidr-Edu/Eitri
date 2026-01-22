# Eitri

**Java code parser that forges PlantUML diagrams from Java source code.**

![](https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNzE3ZWM0eTQ1MXc4ZHFpanlrdmw3YTJyM2Y0dGZ2MngxOXQzeDJrNCZlcD12MV9naWZzX3NlYXJjaCZjdD1n/l3mZ67L0776T759uM/giphy.gif)

## Overview

Eitri is a CLI tool that parses Java source directories and automatically generates PlantUML class diagrams (`.puml`) **without compiling or running the project**. It uses [JavaParser](https://javaparser.org/) for static analysis with symbol resolution.

### Features

- **Type extraction**: Classes, interfaces, enums, annotations, records
- **Member extraction**: Fields, methods, constructors with full signatures
- **Relationship detection**:
  - Inheritance (`extends`) and implementation (`implements`)
  - Composition, aggregation, association (from fields)
  - Dependencies (from method parameters/return types)
- **Package grouping**: Types organized by package in the diagram
- **Highly configurable**: Filter members by visibility, hide/show relation types, customize layout
- **YAML configuration**: Store settings in a config file for reproducibility
- **Multiple source paths**: Combine sources from multiple directories

## Dependencies

- **Java 25** or later
- **Maven** for building
- [JavaParser](https://github.com/javaparser/javaparser) with symbol solver (bundled)
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

Create an `.eitri.config.yaml` file for reusable settings:

```yaml
# .eitri.config.yaml
diagramName: my-project
direction: tb

# Visibility filtering
hidePrivate: true
hideProtected: false
hidePackage: false

# Member filtering
hideFields: false
hideMethods: false
hideEmptyMembers: true

# Relation filtering
showInheritance: true
showImplements: true
showComposition: true
showAggregation: true
showAssociation: true
showDependency: false # Often too noisy

# Display
hideCircle: false
hideUnlinked: false
showStereotypes: true
showGenerics: true
showMultiplicities: true
showLabels: true

# Custom skinparam lines
skinparamLines:
  - "skinparam classAttributeIconSize 0"
  - "skinparam linetype ortho"
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
