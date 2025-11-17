#!/bin/bash

# Run tests

echo "Running tests..."
mvn clean test

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ All tests passed!"
else
    echo ""
    echo "❌ Some tests failed!"
    exit 1
fi
