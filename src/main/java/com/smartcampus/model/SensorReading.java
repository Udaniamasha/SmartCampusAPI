package com.smartcampus.model;

/**
 * Represents a single time-stamped measurement captured by a sensor.
 * Readings are immutable historical records — they are never updated after creation.
 */
public class SensorReading {

    private String id;        // Unique reading event ID (UUID)
    private long timestamp;   // Epoch time in milliseconds when the reading was captured
    private double value;     // The actual metric value recorded by the hardware

    // ---- Constructors ----

    public SensorReading() {
        // Default constructor required by Jackson for JSON deserialization
    }

    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    // ---- Getters ----

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getValue() {
        return value;
    }

    // ---- Setters ----

    public void setId(String id) {
        this.id = id;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
    