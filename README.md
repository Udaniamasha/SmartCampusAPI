# Smart Campus Sensor & Room Management API

A RESTful API built with JAX-RS (Jersey 2.32) and Apache Tomcat 9, developed as coursework for 5COSC022W Client-Server Architectures at the University of Westminster.

---

## API Overview

This API manages campus Rooms and IoT Sensors deployed within them. It supports full CRUD operations for rooms and sensors, historical sensor readings, sub-resource navigation, and comprehensive error handling — all backed by thread-safe in-memory data structures (no database).

**Base URL:** `http://localhost:8080/SmartCampusAPI/api/v1`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Discovery endpoint — API metadata and links |
| GET | `/rooms` | List all rooms |
| POST | `/rooms` | Create a new room |
| GET | `/rooms/{roomId}` | Get a specific room |
| DELETE | `/rooms/{roomId}` | Delete a room (blocked if sensors assigned) |
| GET | `/sensors` | List all sensors (supports `?type=` filter) |
| POST | `/sensors` | Register a new sensor |
| GET | `/sensors/{sensorId}/readings` | Get reading history for a sensor |
| POST | `/sensors/{sensorId}/readings` | Post a new reading for a sensor |

---

## How to Build and Run

### Prerequisites
- Java JDK 11+
- Apache Maven 3.6+
- Apache Tomcat 9.x

### Steps

1. Clone the repository:
```bash
git clone https://github.com/YOUR_USERNAME/SmartCampusAPI.git
cd SmartCampusAPI
```

2. Build the project:
```bash
mvn clean install
```

3. Deploy the generated WAR file to Tomcat 9:
   - Copy `target/SmartCampusAPI-1.0-SNAPSHOT.war` to Tomcat's `webapps/` folder
   - Start Tomcat: run `bin/startup.bat` (Windows) or `bin/startup.sh` (Mac/Linux)

4. Access the API at:
```
http://localhost:8080/SmartCampusAPI/api/v1/
```

---

## Sample curl Commands

### 1. Discovery Endpoint
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/
```

### 2. Create a Room
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'
```

### 3. Create a Sensor (linked to a room)
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":22.5,"roomId":"LIB-301"}'
```

### 4. Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/SmartCampusAPI/api/v1/sensors?type=Temperature"
```

### 5. Post a Sensor Reading
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.7}'
```

### 6. Get Reading History
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/sensors/TEMP-001/readings
```

### 7. Delete a Room (triggers 409 if sensors assigned)
```bash
curl -X DELETE http://localhost:8080/SmartCampusAPI/api/v1/rooms/LIB-301
```

### 8. Create Sensor with invalid roomId (triggers 422)
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-ROOM"}'
```

---

## Report — Question Answers

### Part 1: Service Architecture & Setup

**Q1. JAX-RS Resource Lifecycle & Impact on Data Management**

By default, JAX-RS uses a per-request lifecycle, meaning a new instance of a resource class (e.g., RoomResource, SensorResource) is created for every HTTP request and destroyed after the response is sent. This is the standard behaviour defined by the JAX-RS specification.

As a result, instance-level variables do not persist between requests. Any data stored inside them would be reset every time, making them unsuitable for application state.

To handle persistence, shared data is moved outside resource classes. In this project, a MockDatabase class stores data in static final maps (ROOMS, SENSORS, SENSOR_READINGS). Static data belongs to the class itself and remains available for the entire runtime of the application.

Since multiple requests can run concurrently, all threads access the same shared data. This introduces race condition risks if unsafe structures like HashMap are used. To solve this, ConcurrentHashMap is used for thread-safe operations, while CopyOnWriteArrayList ensures safe iteration during updates. This guarantees consistency under concurrent load.

---

**Q2. Importance of HATEOAS in REST Design**

HATEOAS (Hypermedia as the Engine of Application State) is a Level 3 REST principle where API responses include links that guide clients to available actions instead of requiring hardcoded endpoints.

This improves flexibility because clients can discover endpoints dynamically starting from a single entry point (e.g., /api/v1). If server routes change, the client does not break as long as the entry point remains valid.

It also reduces coupling between client and server, making the system easier to evolve. Additionally, it improves usability since the API becomes self-explanatory through navigable links rather than relying entirely on external documentation.

---

### Part 2: Room Management

**Q3. IDs vs Full Objects in Responses**

Returning only IDs reduces payload size and is useful for lightweight communication. However, it forces clients to make multiple additional requests to fetch full details, leading to the N+1 problem and higher latency.

Returning full objects avoids extra round-trips and is more efficient for dashboards where complete data is needed at once. The trade-off is a slightly larger response size, but since room objects are small and lookups are fast, this is acceptable.

In this implementation, full objects are returned to improve client efficiency and reduce unnecessary network calls.

---

**Q4. DELETE Idempotency**

The DELETE operation is idempotent, meaning repeated calls produce the same final state.

On the first request, if the room exists and has no sensors, it is deleted, and 204 No Content is returned. If the same request is repeated, the room is already gone, so the system still returns 204 No Content without changing state.

If the room contains active sensors, deletion is blocked, and 409 Conflict is returned to prevent orphaned data.

This behaviour ensures consistent results even if clients retry requests due to network issues.

---

### Part 3: Sensor Operations & Linking

**Q5. @Consumes and Content-Type Mismatch**

The @Consumes(MediaType.APPLICATION_JSON) annotation restricts the endpoint to JSON input only. Before execution, JAX-RS checks the request's Content-Type.

If a client sends text/plain or application/xml, the request is rejected immediately with 415 Unsupported Media Type, and the method is never executed.

This happens because Jackson only supports JSON deserialization, so unsupported formats cannot be converted into Java objects. This mechanism enforces input contracts and prevents invalid data from reaching business logic.

---

**Q6. QueryParam vs Path Parameters for Filtering**

Path parameters imply identity, while query parameters represent filters. Using /sensors/type/CO2 incorrectly suggests CO2 is a resource.

Query parameters are more appropriate because they clearly express filtering intent:
- `/sensors` → all sensors
- `/sensors?type=CO2` → filtered result

They also support multiple filters easily (?type=CO2&status=ACTIVE) and avoid URL structure complexity. This makes query parameters the standard approach for collection filtering.

---

### Part 4: Sub-Resources

**Q7. Sub-Resource Locator Pattern**

The Sub-Resource Locator pattern allows a resource method to return another resource class for handling nested paths. In this project, SensorResource delegates /sensors/{id}/readings to SensorReadingResource.

This improves separation of concerns by splitting responsibilities:
- SensorResource → sensor management
- SensorReadingResource → reading history

It prevents large controllers and improves maintainability as each class remains focused. It also allows passing sensorId directly to the sub-resource, making it self-contained and easier to test.

---

### Part 5: Error Handling & Logging

**Q8. Why HTTP 422 is better than 404**

HTTP 404 indicates that a resource or endpoint does not exist. In this case, the endpoint /api/v1/sensors exists and is valid.

The issue is inside the request body (e.g., invalid roomId). HTTP 422 Unprocessable Entity is more accurate because it means:
- Request format is valid
- JSON is correctly parsed
- But semantic content is invalid

This provides clearer feedback to the client and avoids confusion about missing endpoints.

---

**Q9. Risks of Exposing Stack Traces**

Stack traces expose internal system details such as class names, package structure, library versions, and execution flow. Attackers can use this to identify vulnerabilities or understand system internals.

This is classified as CWE-209 (Information Exposure). It may also reveal dependencies with known CVEs.

To prevent this, a global exception mapper is used. It logs full details internally while returning a generic 500 Internal Server Error response without exposing sensitive information.

---

**Q10. Why Use JAX-RS Filters for Logging**

Logging is a cross-cutting concern that applies to all endpoints. Adding logs manually in each method leads to duplication and inconsistency.

JAX-RS filters solve this by handling logging centrally using ContainerRequestFilter and ContainerResponseFilter. This means:
- Logging logic is written once and applied everywhere automatically
- No risk of forgetting to log a new endpoint
- Resource methods stay clean and focused on business logic
- If the log format changes, only one file needs updating

This separation of concerns is a fundamental software engineering principle, and JAX-RS filters are the framework-provided mechanism specifically designed for it.

---

## Technology Stack

- **Language:** Java 11
- **Framework:** JAX-RS 2.1 (Jersey 2.32)
- **Server:** Apache Tomcat 9
- **Build Tool:** Maven
- **JSON:** Jackson (via jersey-media-json-jackson)
- **Data Storage:** In-memory ConcurrentHashMap (no database)
