package com.smartcampus.resource;

import com.smartcampus.dao.MockDatabase;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Part 2 – Room Management
 * Handles: GET /rooms, POST /rooms, GET /rooms/{id}, DELETE /rooms/{id}
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    // ----------------------------------------------------------------
    // Part 2.1 – GET all rooms
    // ----------------------------------------------------------------
    @GET
    public Response getAllRooms() {
        List<Room> rooms = new ArrayList<>(MockDatabase.ROOMS.values());
        return Response.ok(rooms).build();
    }

    // ----------------------------------------------------------------
    // Part 2.1 – POST create a new room  → 201 Created + Location header
    // ----------------------------------------------------------------
    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().isBlank()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "status",  400,
                            "error",   "Bad Request",
                            "message", "Room 'id' field is required."
                    ))
                    .build();
        }

        if (MockDatabase.ROOMS.containsKey(room.getId())) {
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity(Map.of(
                            "status",  409,
                            "error",   "Conflict",
                            "message", "A room with id '" + room.getId() + "' already exists."
                    ))
                    .build();
        }

        MockDatabase.ROOMS.put(room.getId(), room);

        URI location = URI.create("/api/v1/rooms/" + room.getId());
        return Response.created(location).entity(room).build();   // 201 + Location header
    }

    // ----------------------------------------------------------------
    // Part 2.1 – GET single room by ID
    // ----------------------------------------------------------------
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = MockDatabase.ROOMS.get(roomId);
        if (room == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "status",  404,
                            "error",   "Not Found",
                            "message", "Room '" + roomId + "' does not exist."
                    ))
                    .build();
        }
        return Response.ok(room).build();
    }

    // ----------------------------------------------------------------
    // Part 2.2 – DELETE room  (blocked if sensors still assigned → 409)
    // ----------------------------------------------------------------
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = MockDatabase.ROOMS.get(roomId);

        // Idempotent: if already gone, return 204 No Content (no error)
        if (room == null) {
            return Response.noContent().build();   // 204 – idempotent behaviour
        }

        // Business Logic Constraint: block deletion if sensors are still linked
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Cannot delete room '" + roomId + "'. It still has " +
                    room.getSensorIds().size() + " sensor(s) assigned. " +
                    "Remove all sensors before decommissioning the room."
            );
        }

        MockDatabase.ROOMS.remove(roomId);
        return Response.noContent().build();   // 204 No Content – success
    }
}