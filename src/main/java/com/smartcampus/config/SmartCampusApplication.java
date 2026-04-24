package com.smartcampus.config;

import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.mapper.GlobalExceptionMapper;
import com.smartcampus.mapper.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.mapper.RoomNotEmptyExceptionMapper;
import com.smartcampus.mapper.SensorUnavailableExceptionMapper;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * Part 1.1 – Application Configuration
 *
 * @ApplicationPath sets the versioned base URI for all endpoints: /api/v1
 *
 * getClasses() registers every resource class, exception mapper, and filter
 * with the JAX-RS runtime. Only classes listed here will be active.
 *
 * NOTE: SensorReadingResource is NOT registered here because it is a sub-resource —
 * it is instantiated programmatically by SensorResource's sub-resource locator method,
 * NOT by the JAX-RS runtime directly.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // ── Resources (API Endpoints) ──────────────────────────────────
        classes.add(DiscoveryResource.class);   // GET /api/v1
        classes.add(RoomResource.class);         // /api/v1/rooms
        classes.add(SensorResource.class);       // /api/v1/sensors (+ sub-resource locator)

        // ── Exception Mappers ──────────────────────────────────────────
        classes.add(RoomNotEmptyExceptionMapper.class);            // 409
        classes.add(LinkedResourceNotFoundExceptionMapper.class);  // 422
        classes.add(SensorUnavailableExceptionMapper.class);       // 403
        classes.add(GlobalExceptionMapper.class);                  // 500 (catch-all)

        // ── Filters (cross-cutting concerns) ──────────────────────────
        classes.add(LoggingFilter.class);        // request + response logging

        return classes;
    }
}