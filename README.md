# Eitri

Java code parser that forges PlantUML diagrams from Java source code.

![](https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNzE3ZWM0eTQ1MXc4ZHFpanlrdmw3YTJyM2Y0dGZ2MngxOXQzeDJrNCZlcD12MV9naWZzX3NlYXJjaCZjdD1n/l3mZ67L0776T759uM/giphy.gif)

## Overview

A CLI tool that parses a Java source directory and automatically generates a PlantUML class diagram (.puml) without compiling or running the project.
This tool uses JavaParser to statically analyze .java files and extract as much class structure information as possible, including:

- Classes, interfaces, enums, annotations, records
- Fields, methods, constructors
- Inheritance (extends, implements)
- Relationships (associations, dependencies, aggregations, compositions)

## Dependencies

- [JavaParser](https://javaparser.org/) for parsing Java source code. See [JavaParser GitHub](https://github.com/javaparser/javaparser/blob/master/readme.md) for docs.

## 📦 Build

Make sure you have Java 25 and Maven installed.
Install graphviz (for rendering diagrams from PlantUML) if you want to visualize the output.

```bash
brew install graphviz
```

```bash
mvn clean package
```

This produces an executable jar:

```bash
target/eitri-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## ▶️ Usage

Generate a PlantUML diagram from any Java project’s source directory:

```bash
java -jar target/eitri-1.0-SNAPSHOT-jar-with-dependencies.jar \
 --src : Path to the root Java source folder \
 --out : Output .puml file
```

You can open the .puml in any PlantUML viewer (IntelliJ plugin, VS Code plugin, plantuml.jar, etc.).

## 🌀 Batch Running on Many Projects

Example script for running the generator on all projects inside a directory:

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
    java -jar "$JAR" --src "$src" --out "$out"
else
echo "Skipping $name (no src/main/java)"
fi
done
```
