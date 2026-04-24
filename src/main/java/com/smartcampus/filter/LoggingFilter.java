package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 – API Request & Response Logging Filter
 *
 * Implements BOTH ContainerRequestFilter and ContainerResponseFilter in one class.
 * This is a "cross-cutting concern" — it runs for EVERY request/response automatically
 * without any changes to individual resource methods.
 *
 * @Provider registers it with the JAX-RS runtime automatically.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    // Using java.util.logging.Logger as required by the spec
    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Runs BEFORE the request reaches any resource method.
     * Logs: HTTP method + full request URI
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String uri    = requestContext.getUriInfo().getRequestUri().toString();

        LOGGER.info("[REQUEST]  " + method + " " + uri);
    }

    /**
     * Runs AFTER the resource method has executed and a response is ready.
     * Logs: HTTP status code of the outgoing response
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        int    statusCode   = responseContext.getStatus();
        String method       = requestContext.getMethod();
        String uri          = requestContext.getUriInfo().getRequestUri().toString();

        LOGGER.info("[RESPONSE] " + method + " " + uri + " → HTTP " + statusCode);
    }
}