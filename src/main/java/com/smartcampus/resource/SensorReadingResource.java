package com.smartcampus.resource;

import com.smartcampus.dao.MockDatabase;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Part 4.2 – Historical Data Management  (Sub-Resource)
 *
 * This class is NOT annotated with @Path at class level.
 * It is reached exclusively via the Sub-Resource Locator in SensorResource:
 *   @Path("/{sensorId}/readings")  →  returns new SensorReadingResource(sensorId)
 *
 * Handles:
 *   GET  /api/v1/sensors/{sensorId}/readings  – fetch reading history
 *   POST /api/v1/sensors/{sensorId}/readings  – append new reading
 *                                               + update parent sensor's currentValue
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    // Constructor called by the sub-resource locator in SensorResource
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ----------------------------------------------------------------
    // Part 4.2 – GET all readings for this sensor (history)
    // ----------------------------------------------------------------
    @GET
    public Response getReadings() {
        List<SensorReading> readings = MockDatabase.SENSOR_READINGS.get(sensorId);

        if (readings == null) {
            // Sensor exists (validated by locator) but has no reading list yet — return empty
            return Response.ok(new ArrayList<>()).build();
        }

        return Response.ok(readings).build();
    }

    // ----------------------------------------------------------------
    // Part 4.2 – POST new reading  (403 if sensor is MAINTENANCE/OFFLINE)
    // Side effect: updates parent Sensor's currentValue
    // ----------------------------------------------------------------
    @POST
    public Response addReading(SensorReading reading) {

        // ── NULL CHECK FIRST ─────────────────────────────────────────
        // Must be before any field access to avoid NullPointerException
        if (reading == null) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "status",  400,
                            "error",   "Bad Request",
                            "message", "Reading body is required."
                    ))
                    .build();
        }

        Sensor sensor = MockDatabase.SENSORS.get(sensorId);

        // Part 5.3 – State Constraint: block readings from MAINTENANCE sensors
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is currently under MAINTENANCE " +
                    "and cannot accept new readings. " +
                    "Please wait until the sensor is restored to ACTIVE status."
            );
        }

        // Also block OFFLINE sensors (they are physically disconnected)
        if ("OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is OFFLINE and cannot record readings."
            );
        }

        // Auto-generate a UUID reading ID if not provided
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }

        // Auto-set timestamp to now if not provided
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Ensure the reading history list exists
        MockDatabase.SENSOR_READINGS.putIfAbsent(sensorId, new java.util.concurrent.CopyOnWriteArrayList<>());

        // Persist the reading
        MockDatabase.SENSOR_READINGS.get(sensorId).add(reading);

        // ---------------------------------------------------------------
        // SIDE EFFECT (Part 4.2 requirement):
        // Update the parent Sensor's currentValue to reflect the latest measurement
        // ---------------------------------------------------------------
        sensor.setCurrentValue(reading.getValue());

        URI location = URI.create("/api/v1/sensors/" + sensorId + "/readings/" + reading.getId());
        return Response.created(location).entity(reading).build();   // 201
    }
}