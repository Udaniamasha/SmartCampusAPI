package com.smartcampus.dao;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockDatabase {
    // Thread-safe maps for our in-memory database
    public static final Map<String, Room> ROOMS = new ConcurrentHashMap<>();
    public static final Map<String, Sensor> SENSORS = new ConcurrentHashMap<>();

    // Maps SensorID -> List of Readings (Required for Historical Management)
    public static final Map<String, List<SensorReading>> SENSOR_READINGS = new ConcurrentHashMap<>();
}