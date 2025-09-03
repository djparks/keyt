#!/bin/bash

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Java is not installed. Please install Java 17 or greater."
    exit 1
fi

# Get Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)

# Check if Java version is 17 or greater
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "Java 17 or greater is required. Current version is $JAVA_VERSION."
    exit 1
fi

# Run the Java application with all passed parameters
java -jar keyt.jar "$@"