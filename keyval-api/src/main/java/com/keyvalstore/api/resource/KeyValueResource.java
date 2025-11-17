package com.keyvalstore.api.resource;

import com.keyvalstore.api.dto.KeyValueResponse;
import com.keyvalstore.api.service.KeyValueService;
import com.keyvalstore.core.exception.KeyNotFoundException;
import com.keyvalstore.core.model.ConsistencyLevel;
import com.keyvalstore.core.model.KeyValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/api/v1/namespaces/{namespace}/kv")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Key-Value", description = "Key-value operations")
public class KeyValueResource {

    private final KeyValueService keyValueService;

    public KeyValueResource(KeyValueService keyValueService) {
        this.keyValueService = keyValueService;
    }

    @PUT
    @Path("/{key}")
    @Operation(summary = "Set a key-value pair")
    @ApiResponse(responseCode = "200", description = "Key-value set successfully")
    @ApiResponse(responseCode = "404", description = "Namespace not found")
    public Response putKeyValue(
            @PathParam("namespace") String namespace,
            @PathParam("key") String key,
            @Parameter(description = "Value as string") String value,
            @QueryParam("consistency") @DefaultValue("QUORUM") ConsistencyLevel consistencyLevel) {

        KeyValue keyValue = keyValueService.put(namespace, key, value, consistencyLevel);
        return Response.ok(new KeyValueResponse(keyValue)).build();
    }

    @POST
    @Path("/{key}/increment")
    @Operation(summary = "Increment a value (for ATOMIC_INCREMENT strategy)")
    @ApiResponse(responseCode = "200", description = "Value incremented successfully")
    @ApiResponse(responseCode = "400", description = "Invalid operation for namespace strategy")
    @ApiResponse(responseCode = "404", description = "Namespace not found")
    public Response incrementValue(
            @PathParam("namespace") String namespace,
            @PathParam("key") String key,
            @QueryParam("consistency") @DefaultValue("QUORUM") ConsistencyLevel consistencyLevel) {

        KeyValue keyValue = keyValueService.increment(namespace, key, consistencyLevel);
        return Response.ok(new KeyValueResponse(keyValue)).build();
    }

    @GET
    @Path("/{key}")
    @Operation(summary = "Get a value by key")
    @ApiResponse(responseCode = "200", description = "Value retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Key or namespace not found")
    public Response getKeyValue(
            @PathParam("namespace") String namespace,
            @PathParam("key") String key,
            @QueryParam("consistency") @DefaultValue("QUORUM") ConsistencyLevel consistencyLevel) {

        KeyValue keyValue = keyValueService.get(namespace, key, consistencyLevel)
                .orElseThrow(() -> new KeyNotFoundException(namespace, key));

        return Response.ok(new KeyValueResponse(keyValue)).build();
    }

    @DELETE
    @Path("/{key}")
    @Operation(summary = "Delete a key")
    @ApiResponse(responseCode = "204", description = "Key deleted successfully")
    @ApiResponse(responseCode = "404", description = "Key or namespace not found")
    public Response deleteKeyValue(
            @PathParam("namespace") String namespace,
            @PathParam("key") String key,
            @QueryParam("consistency") @DefaultValue("QUORUM") ConsistencyLevel consistencyLevel) {

        boolean deleted = keyValueService.delete(namespace, key, consistencyLevel);

        if (!deleted) {
            throw new KeyNotFoundException(namespace, key);
        }

        return Response.noContent().build();
    }

    @GET
    @Operation(summary = "List keys in a namespace")
    @ApiResponse(responseCode = "200", description = "Keys retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Namespace not found")
    public Response listKeys(
            @PathParam("namespace") String namespace,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("100") int limit) {

        List<String> keys = keyValueService.listKeys(namespace, offset, limit);
        long total = keyValueService.count(namespace);

        Map<String, Object> response = Map.of(
                "keys", keys,
                "offset", offset,
                "limit", limit,
                "total", total
        );

        return Response.ok(response).build();
    }

    @HEAD
    @Path("/{key}")
    @Operation(summary = "Check if a key exists")
    @ApiResponse(responseCode = "200", description = "Key exists")
    @ApiResponse(responseCode = "404", description = "Key or namespace not found")
    public Response existsKeyValue(
            @PathParam("namespace") String namespace,
            @PathParam("key") String key) {

        boolean exists = keyValueService.exists(namespace, key);

        if (!exists) {
            throw new KeyNotFoundException(namespace, key);
        }

        return Response.ok().build();
    }
}
