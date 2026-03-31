#!/usr/bin/env bash
set -e

# Clone and Diagram Script
# Usage: ./scripts/clone-and-diagram.sh <git-url> [source-root]
# Example: ./scripts/clone-and-diagram.sh https://github.com/TooTallNate/Java-WebSocket.git
# Example with custom source root: ./scripts/clone-and-diagram.sh https://github.com/auth0/java-jwt.git lib/src/main/java

if [ -z "$1" ]; then
  echo "Usage: $0 <git-url> [source-root]"
  echo "Example: $0 https://github.com/TooTallNate/Java-WebSocket.git"
  echo "Example with custom source root: $0 https://github.com/auth0/java-jwt.git lib/src/main/java"
  exit 1
fi

GIT_URL="$1"
CUSTOM_SRC_ROOT="$2"
REPO_NAME=$(basename "$GIT_URL" .git)
EITRI_JAR="target/eitri-1.0-SNAPSHOT-jar-with-dependencies.jar"
REPOS_DIR="repos"
GENERATED_DIR="generated"

echo "=== Clone and Diagram ==="
echo "Repository: $REPO_NAME"
echo "Git URL: $GIT_URL"
echo ""

# Create repos directory if it doesn't exist
mkdir -p "$REPOS_DIR"

# Clone or update the repository
if [ -d "$REPOS_DIR/$REPO_NAME" ]; then
  echo "Repository already exists. Updating..."
  cd "$REPOS_DIR/$REPO_NAME"
  git stash
  git pull
  cd - > /dev/null
else
  echo "Cloning repository..."
  git clone "$GIT_URL" "$REPOS_DIR/$REPO_NAME"
fi
echo ""

# Determine source directory
if [ -n "$CUSTOM_SRC_ROOT" ]; then
  SRC_DIR="$REPOS_DIR/$REPO_NAME/$CUSTOM_SRC_ROOT"
  if [ ! -d "$SRC_DIR" ]; then
    echo "Error: Specified source root not found: $SRC_DIR"
    exit 1
  fi
else
  SRC_DIR="$REPOS_DIR/$REPO_NAME/src/main/java"
  if [ ! -d "$SRC_DIR" ]; then
    echo "Warning: Standard Maven structure not found at $SRC_DIR"
    echo "Trying alternative locations..."

    # Try root src
    if [ -d "$REPOS_DIR/$REPO_NAME/src" ]; then
      SRC_DIR="$REPOS_DIR/$REPO_NAME/src"
    else
      echo "Error: Could not find source directory"
      echo "Tip: Specify a source root manually: $0 <git-url> <source-root>"
      echo "Example: $0 $GIT_URL lib/src/main/java"
      exit 1
    fi
  fi
fi

echo "Source directory: $SRC_DIR"
echo ""

# Create output directory
mkdir -p "$GENERATED_DIR"

# Generate diagram
echo "Generating diagram..."
CONFIG_FILE="$(cd "$(dirname "$0")/.." && pwd)/.eitri.config.yaml"
java -jar "$EITRI_JAR" \
  --src "$SRC_DIR" \
  --out "$GENERATED_DIR/$REPO_NAME.puml" \
  --config "$CONFIG_FILE"

echo ""
echo "=== Done ==="
echo "Diagram saved to: $GENERATED_DIR/$REPO_NAME.puml"
