#!/bin/bash

# Demo script showcasing KeyVal Store features

BASE_URL="http://localhost:8080"

echo "KeyVal Store Demo"
echo "================="
echo ""

# Create namespaces
echo "1. Creating namespaces..."
echo ""

echo "Creating 'users' namespace (JSON_BLOB strategy)..."
curl -X POST $BASE_URL/api/v1/namespaces \
  -H "Content-Type: application/json" \
  -d '{
    "name": "users",
    "keyStrategyType": "HASH",
    "valueStrategyType": "JSON_BLOB",
    "configuration": {}
  }'
echo ""
echo ""

echo "Creating 'counters' namespace (ATOMIC_INCREMENT strategy)..."
curl -X POST $BASE_URL/api/v1/namespaces \
  -H "Content-Type: application/json" \
  -d '{
    "name": "counters",
    "keyStrategyType": "HASH",
    "valueStrategyType": "ATOMIC_INCREMENT",
    "configuration": {}
  }'
echo ""
echo ""

echo "Creating 'cache' namespace (STRING strategy)..."
curl -X POST $BASE_URL/api/v1/namespaces \
  -H "Content-Type: application/json" \
  -d '{
    "name": "cache",
    "keyStrategyType": "HASH",
    "valueStrategyType": "STRING",
    "configuration": {}
  }'
echo ""
echo ""

# List namespaces
echo "2. Listing all namespaces..."
curl -X GET $BASE_URL/api/v1/namespaces
echo ""
echo ""

# Store user data (JSON)
echo "3. Storing user data..."
curl -X PUT $BASE_URL/api/v1/namespaces/users/kv/user123 \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "age": 30}'
echo ""
echo ""

curl -X PUT $BASE_URL/api/v1/namespaces/users/kv/user456 \
  -H "Content-Type: application/json" \
  -d '{"name": "Jane Smith", "email": "jane@example.com", "age": 28}'
echo ""
echo ""

# Retrieve user data
echo "4. Retrieving user data..."
curl -X GET $BASE_URL/api/v1/namespaces/users/kv/user123
echo ""
echo ""

# Atomic increment operations
echo "5. Testing atomic increment..."
curl -X POST $BASE_URL/api/v1/namespaces/counters/kv/page_views/increment
echo ""
echo ""

curl -X POST $BASE_URL/api/v1/namespaces/counters/kv/page_views/increment
echo ""
echo ""

curl -X POST $BASE_URL/api/v1/namespaces/counters/kv/page_views/increment
echo ""
echo ""

echo "Getting counter value..."
curl -X GET $BASE_URL/api/v1/namespaces/counters/kv/page_views
echo ""
echo ""

# Cache operations
echo "6. Testing cache operations..."
curl -X PUT $BASE_URL/api/v1/namespaces/cache/kv/session_abc123 \
  -H "Content-Type: application/json" \
  -d '"active"'
echo ""
echo ""

curl -X GET $BASE_URL/api/v1/namespaces/cache/kv/session_abc123
echo ""
echo ""

# List keys
echo "7. Listing keys in 'users' namespace..."
curl -X GET "$BASE_URL/api/v1/namespaces/users/kv?limit=10"
echo ""
echo ""

# Get namespace stats
echo "8. Getting namespace statistics..."
curl -X GET $BASE_URL/api/v1/namespaces/users/stats
echo ""
echo ""

# Health check
echo "9. Checking system health..."
curl -X GET $BASE_URL/admin/health
echo ""
echo ""

# Metrics
echo "10. Getting metrics..."
curl -X GET $BASE_URL/admin/metrics
echo ""
echo ""

echo "Demo completed!"
