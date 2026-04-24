package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Part 1, Task 2 – Discovery Endpoint
 * GET /api/v1  → returns API metadata + resource links (HATEOAS)
 */
@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {

        // Resource links map (HATEOAS)
        Map<String, String> resources = new HashMap<>();
        resources.put("rooms",    "/api/v1/rooms");
        resources.put("sensors",  "/api/v1/sensors");

        // Full metadata response
        Map<String, Object> response = new HashMap<>();
        response.put("name",        "Smart Campus Sensor & Room Management API");
        response.put("version",     "1.0");
        response.put("description", "RESTful API for managing campus rooms and IoT sensors.");
        response.put("contact",     "admin@smartcampus.ac.uk");
        response.put("resources",   resources);

        return Response.ok(response).build();
    }
}