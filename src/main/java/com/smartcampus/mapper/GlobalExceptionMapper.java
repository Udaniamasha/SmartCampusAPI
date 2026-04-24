package com.smartcampus.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Global "Catch-All" Mapper - Part 5, Task 4
 * Prevents raw Java stack traces from being exposed to clients,
 * ensuring the API is secure.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable exception) {
        // Log the actual error in the server console so developers can fix it
        exception.printStackTrace();
        
        // Return a clean, non-revealing JSON message to the client
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR) // HTTP 500
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "status", 500,
                        "error", "Internal Server Error",
                        "message", "An unexpected server error occurred. Please contact support."
                ))
                .build();
    }
}