package com.smartcampus.resource;

import com.smartcampus.dao.MockDatabase;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Part 3 – Sensor Operations & Linking
 * Part 4 – Sub-Resource Locator for readings
 *
 * Handles: GET /sensors, POST /sensors
 * Sub-resource locator: GET|POST /sensors/{sensorId}/readings
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    // ----------------------------------------------------------------
    // Part 3.2 – GET all sensors  (optional ?type= query param filter)
    // ----------------------------------------------------------------
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> all = new ArrayList<>(MockDatabase.SENSORS.values());

        if (type != null && !type.isBlank()) {
            // Case-insensitive filtering – superior to path-param approach
            List<Sensor> filtered = new ArrayList<>();
            for (Sensor s : all) {
                if (type.equalsIgnoreCase(s.getType())) {
                    filtered.add(s);
                }
            }
            return Response.ok(filtered).build();
        }

        return Response.ok(all).build();
    }

    // ----------------------------------------------------------------
    // Part 3.1 – POST create sensor  (validates roomId exists → 422)
    // ----------------------------------------------------------------
    @POST
    public Response createSensor(Sensor sensor) {
        // Validate required fields
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "status",  400,
                            "error",   "Bad Request",
                            "message", "Sensor 'id' field is required."
                    ))
                    .build();
        }

        // Foreign-key integrity check: roomId must reference an existing room
        if (sensor.getRoomId() == null || !MockDatabase.ROOMS.containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: room '" + sensor.getRoomId() +
                    "' does not exist in the system. " +
                    "Please create the room first before assigning sensors to it."
            );
        }

        // Duplicate sensor check
        if (MockDatabase.SENSORS.containsKey(sensor.getId())) {
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity(Map.of(
                            "status",  409,
                            "error",   "Conflict",
                            "message", "A sensor with id '" + sensor.getId() + "' already exists."
                    ))
                    .build();
        }

        // Set default status if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        // Persist sensor
        MockDatabase.SENSORS.put(sensor.getId(), sensor);

        // Link sensor ID into the parent room's sensorIds list (bidirectional link)
        MockDatabase.ROOMS.get(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        // Initialise empty reading history list for this sensor
        MockDatabase.SENSOR_READINGS.put(sensor.getId(), new CopyOnWriteArrayList<>());

        URI location = URI.create("/api/v1/sensors/" + sensor.getId());
        return Response.created(location).entity(sensor).build();   // 201
    }

    // ----------------------------------------------------------------
    // Part 4.1 – Sub-Resource Locator pattern
    // Delegates /sensors/{sensorId}/readings to SensorReadingResource
    // ----------------------------------------------------------------
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        // Validate sensor exists before delegating
        Sensor sensor = MockDatabase.SENSORS.get(sensorId);
        if (sensor == null) {
            // Throwing NotFoundException here is safe; JAX-RS will map it to 404
            throw new NotFoundException("Sensor '" + sensorId + "' does not exist.");
        }
        // Return the sub-resource instance, injecting the sensorId context
        return new SensorReadingResource(sensorId);
    }
}