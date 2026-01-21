# Eitri

Java code parser that forges PlantUML diagrams from Java source code.

![](https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNzE3ZWM0eTQ1MXc4ZHFpanlrdmw3YTJyM2Y0dGZ2MngxOXQzeDJrNCZlcD12MV9naWZzX3NlYXJjaCZjdD1n/l3mZ67L0776T759uM/giphy.gif)

## Overview

A small CLI tool that parses a Java source directory and automatically generates a PlantUML class diagram (.puml) without compiling or running the project.
This tool uses JavaParser to statically analyze .java files and extract:

- Classes, interfaces, enums
- Fields and methods
- extends / implements relationships
- Basic associations between user‑defined types
- Generics and common JDK types are filtered out to avoid diagram noise.

## 📦 Build

Make sure you have Java 17+ and Maven installed.
Install graphviz (for rendering diagrams from PlantUML) if you want to visualize the output.

```bash
brew install graphviz
```

```bash
mvn clean package
```

This produces an executable jar:

```bash
target/java-uml-generator-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## ▶️ Usage

Generate a PlantUML diagram from any Java project’s source directory:

```bash
java -jar target/java-uml-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
 --src /path/to/project/src/main/java \
 --out /path/to/output/diagram.puml
--src : Path to the root Java source folder
--out : Output .puml file
```

You can open the .puml in any PlantUML viewer (IntelliJ plugin, VS Code plugin, plantuml.jar, etc.).

## 🌀 Batch Running on Many Projects

Example script for running the generator on all projects inside a directory:

```bash
#!/usr/bin/env bash

JAR="target/java-uml-generator-1.0-SNAPSHOT-jar-with-dependencies.jar"
PROJECTS_ROOT="/path/to/projects"
OUT_ROOT="/path/to/diagrams"

mkdir -p "$OUT_ROOT"

for project in "$PROJECTS_ROOT"/*; do
  name=$(basename "$project")
  src="$project/src/main/java"
out="$OUT_ROOT/$name.puml"

if [ -d "$src" ]; then
echo "Processing $name"
    java -jar "$JAR" --src "$src" --out "$out"
else
echo "Skipping $name (no src/main/java)"
fi
done
```

## 📝 Notes

- The tool works without compiling the Java project
- No dependency resolution required
- Output diagrams intentionally exclude:
  - JDK types (String, List, Set, Map, etc.)
  - primitives
  - generic type details (Set<String> → Set)
- You can customize filtering or relationships in App.java
