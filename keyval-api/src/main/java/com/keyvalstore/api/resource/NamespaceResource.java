package com.keyvalstore.api.resource;

import com.keyvalstore.api.dto.CreateNamespaceRequest;
import com.keyvalstore.api.dto.NamespaceResponse;
import com.keyvalstore.api.service.NamespaceService;
import com.keyvalstore.core.model.Namespace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/api/v1/namespaces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Namespaces", description = "Namespace management operations")
public class NamespaceResource {

    private final NamespaceService namespaceService;

    public NamespaceResource(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    @POST
    @Operation(summary = "Create a new namespace")
    @ApiResponse(responseCode = "201", description = "Namespace created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public Response createNamespace(@Valid CreateNamespaceRequest request) {
        Namespace namespace = namespaceService.createNamespace(
                request.getName(),
                request.getKeyStrategyType(),
                request.getValueStrategyType(),
                request.getConfiguration()
        );

        return Response
                .status(Response.Status.CREATED)
                .entity(new NamespaceResponse(namespace))
                .build();
    }

    @GET
    @Operation(summary = "List all namespaces")
    @ApiResponse(responseCode = "200", description = "Namespaces retrieved successfully")
    public Response listNamespaces() {
        List<Namespace> namespaces = namespaceService.listNamespaces();
        List<NamespaceResponse> responses = namespaces.stream()
                .map(NamespaceResponse::new)
                .toList();

        return Response.ok(responses).build();
    }

    @GET
    @Path("/{namespace}")
    @Operation(summary = "Get namespace by name")
    @ApiResponse(responseCode = "200", description = "Namespace retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Namespace not found")
    public Response getNamespace(@PathParam("namespace") String namespace) {
        Namespace ns = namespaceService.getNamespace(namespace);
        return Response.ok(new NamespaceResponse(ns)).build();
    }

    @DELETE
    @Path("/{namespace}")
    @Operation(summary = "Delete a namespace")
    @ApiResponse(responseCode = "204", description = "Namespace deleted successfully")
    @ApiResponse(responseCode = "404", description = "Namespace not found")
    public Response deleteNamespace(@PathParam("namespace") String namespace) {
        namespaceService.deleteNamespace(namespace);
        return Response.noContent().build();
    }

    @GET
    @Path("/{namespace}/stats")
    @Operation(summary = "Get namespace statistics")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Namespace not found")
    public Response getNamespaceStats(@PathParam("namespace") String namespace) {
        Map<String, Object> stats = namespaceService.getNamespaceStats(namespace);
        return Response.ok(stats).build();
    }
}
