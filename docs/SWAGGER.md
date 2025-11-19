# OpenAPI/Swagger Integration

This document describes how to access and use the OpenAPI specification and Swagger UI for the KeyVal Store API.

## Accessing the API Documentation

### Swagger UI (Interactive Documentation)

Once the application is running, you can access the interactive Swagger UI at:

```
http://localhost:8080/swagger
```

The Swagger UI provides:
- Interactive API documentation
- Try-it-out functionality to test endpoints directly
- Request/response examples
- Schema definitions

### OpenAPI Specification (JSON/YAML)

The OpenAPI 3.0 specification is available in multiple formats:

**JSON Format:**
```
http://localhost:8080/openapi.json
```

**YAML Format:**
```
http://localhost:8080/openapi.yaml
```

**Static Spec:**
```
docs/openapi.yaml
```

## Features

### Fully Annotated Resources

All REST resources are annotated with OpenAPI 3.0 annotations:
- **NamespaceResource**: Namespace management endpoints
- **KeyValueResource**: Key-value CRUD operations
- **AdminResource**: Cluster administration and monitoring

### Automatic Spec Generation

The OpenAPI specification is generated automatically from the code using:
- JAX-RS annotations (@Path, @GET, @POST, etc.)
- OpenAPI annotations (@Operation, @ApiResponse, @Tag, etc.)
- DropWizard Swagger Bundle

### Configuration

Swagger is configured in the application YAML files:

```yaml
swagger:
  resourcePackage: com.constelld.keyvalstore.api.resource
  title: KeyVal Store API
  description: Distributed Key-Value Store with Off-Heap Storage and Clustering Support
  version: 1.0.0
  contact: support@constelld.com
  license: Apache 2.0
  licenseUrl: https://www.apache.org/licenses/LICENSE-2.0.html
  schemes:
    - http
  uriPrefix: /api
```

## Using the Swagger UI

### 1. Start the Application

```bash
# Build the application
mvn clean package

# Run single node
./run-single.sh

# Or run 3-node cluster
./run-cluster.sh
```

### 2. Access Swagger UI

Open your browser to:
```
http://localhost:8080/swagger
```

### 3. Explore the API

- Click on any endpoint to see details
- Click "Try it out" to test the endpoint
- Fill in parameters
- Click "Execute" to send the request
- View the response

### Example: Create a Namespace

1. Navigate to **Namespaces** → **POST /api/v1/namespaces**
2. Click "Try it out"
3. Enter request body:
   ```json
   {
     "namespace": "users",
     "keyStrategy": "HASH",
     "valueStrategy": "STRING"
   }
   ```
4. Click "Execute"
5. View the response (should be 201 Created)

### Example: Store a Value

1. Navigate to **Key-Value** → **PUT /api/v1/namespaces/{namespace}/kv/{key}**
2. Click "Try it out"
3. Enter:
   - namespace: `users`
   - key: `user:123`
   - value: `John Doe`
4. Click "Execute"
5. View the response (should be 200 OK)

## Code Generation

Generate client SDKs from the OpenAPI spec:

### Using OpenAPI Generator

```bash
# Download the spec
curl http://localhost:8080/openapi.yaml -o openapi.yaml

# Generate Java client
openapi-generator-cli generate \
  -i openapi.yaml \
  -g java \
  -o client/java \
  --additional-properties=library=okhttp-gson

# Generate Python client
openapi-generator-cli generate \
  -i openapi.yaml \
  -g python \
  -o client/python

# Generate TypeScript client
openapi-generator-cli generate \
  -i openapi.yaml \
  -g typescript-fetch \
  -o client/typescript
```

### Using Swagger Codegen

```bash
# Generate Go client
swagger-codegen generate \
  -i http://localhost:8080/openapi.json \
  -l go \
  -o client/go
```

## Integration with Tools

### Postman

1. Open Postman
2. Click "Import"
3. Enter URL: `http://localhost:8080/openapi.json`
4. Click "Import"
5. All endpoints will be available in a new collection

### Insomnia

1. Open Insomnia
2. Click "Import"
3. Select "From URL"
4. Enter: `http://localhost:8080/openapi.json`
5. Click "Fetch and Import"

### cURL

```bash
# Get the OpenAPI spec
curl http://localhost:8080/openapi.json | jq .

# Get the OpenAPI spec in YAML
curl http://localhost:8080/openapi.yaml
```

## Development Workflow

### Adding New Endpoints

1. Create/update the resource class in `keyval-api/src/main/java/.../resource/`
2. Add JAX-RS annotations (`@Path`, `@GET`, `@POST`, etc.)
3. Add OpenAPI annotations:
   ```java
   @POST
   @Path("/example")
   @Operation(summary = "Example endpoint")
   @ApiResponse(responseCode = "200", description = "Success")
   public Response example() {
       // Implementation
   }
   ```
4. Build and run the application
5. The new endpoint will automatically appear in Swagger UI

### Updating API Documentation

The OpenAPI spec is generated from code annotations. To update:

1. Modify the annotations in the resource classes
2. Rebuild the application
3. Restart the application
4. The Swagger UI will reflect the changes

No manual spec editing is required!

## Customization

### Change Swagger UI Path

Edit `KeyValStoreConfiguration` to customize the Swagger path:

```yaml
swagger:
  swaggerUiPath: /api-docs  # Default is /swagger
```

### Add Security Definitions

Update swagger configuration to add authentication:

```yaml
swagger:
  securityDefinitions:
    api_key:
      type: apiKey
      name: X-API-Key
      in: header
```

### Customize Appearance

The Swagger UI can be customized via configuration:

```yaml
swagger:
  includePaths:
    - /api/v1/.*  # Only include v1 endpoints
  prettyPrint: true
```

## Troubleshooting

### Swagger UI Not Loading

Check that:
1. Application is running
2. No errors in logs
3. Access the correct URL: `http://localhost:8080/swagger`

### Endpoints Not Appearing

Verify:
1. Resource package is correct in config: `com.constelld.keyvalstore.api.resource`
2. Resources are registered in `KeyValStoreApplication.run()`
3. JAX-RS annotations are present on methods

### Spec Not Updating

1. Rebuild the application: `mvn clean package`
2. Restart the application
3. Clear browser cache
4. Hard refresh: Ctrl+F5 or Cmd+Shift+R

## References

- [OpenAPI Specification](https://swagger.io/specification/)
- [Swagger Annotations](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)
- [DropWizard Swagger](https://github.com/smoketurner/dropwizard-swagger)
- [OpenAPI Generator](https://openapi-generator.tech/)

## Multi-Node Cluster

When running a 3-node cluster, Swagger UI is available on each node:

- Node 1: http://localhost:8080/swagger
- Node 2: http://localhost:8081/swagger
- Node 3: http://localhost:8082/swagger

All nodes serve the same API specification.
