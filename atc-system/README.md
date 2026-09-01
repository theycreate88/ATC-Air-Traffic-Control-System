# Smart Air Traffic Control & Flight Scheduling System

A JavaFX-based desktop application that simulates a **Smart Air Traffic Control (ATC) and Flight Scheduling System** through an interactive tactical command-center interface.

The system combines **data structures, graph algorithms, flight scheduling, collision avoidance, emergency handling, database integration, multithreading, and real-time visualization** into a single air-traffic simulation based on a stylized map of Pakistan.

The application demonstrates the practical use of **Dijkstra's shortest-path algorithm, PriorityQueue, HashMap, custom sorting algorithms, queues, multithreading, JDBC, and relational database design**.

---

## Key Features

### Flight Management

* Generate flights between supported airports.
* Automatically calculate routes using **Dijkstra's shortest-path algorithm**.
* Support multi-hop routes between airports.
* Track flight movement and status during the simulation.
* Display flights and routes in real time on the tactical map.

### Database-Driven Airport Network

The application uses a relational database to store the **static airport network information** used by the simulation.

The database stores:

* Airport names
* Airport identifiers/codes
* X coordinates
* Y coordinates
* Connections between airports
* Distance between connected airports

The database **does not store individual flights, emergency events, or simulation state**. Flight objects and their real-time state are managed by the Java application during execution.

The stored airport and route information is loaded by the application and used to construct the flight graph.

### Route Planning

The airport network is represented as a weighted graph.

* **Airports** are represented as graph vertices.
* **Routes between airports** are represented as weighted edges.
* The **distance between airports** is used as the edge weight.
* Dijkstra's algorithm calculates the shortest available route.

Example:

```text
Airport A
    |
  120 km
    |
Airport B
    |
  180 km
    |
Airport C
```

The graph can be populated from the airport and route information stored in the database.

---

## Runway Scheduling

The system uses a **PriorityQueue** to simulate runway scheduling.

Flights can be prioritized and managed through airport departure queues before entering the active flight simulation.

The system maintains:

* Airport departure queues
* Runway scheduling
* Flight priorities
* Flight movement after departure

---

## Collision Avoidance

A background collision-avoidance process continuously monitors active aircraft.

When two aircraft approach the configured safety threshold, the system can:

1. Detect the potential conflict.
2. Identify the affected aircraft.
3. Adjust the aircraft's altitude.
4. Display a visual proximity warning.
5. Dynamically recalculate the remaining route using Dijkstra's algorithm.

The collision-avoidance process runs independently from the JavaFX user interface so that the simulation remains responsive.

---

## Head-On Traffic Management

Flights traveling in opposite directions along the same route are automatically separated into parallel lanes.

The system uses **right-hand-side lane separation** to prevent aircraft traveling toward each other from occupying the exact same position on the route.

This provides a more realistic representation of air traffic movement.

---

## Emergency Handling

The system supports emergency scenarios including:

* **Weather Emergency**
* **Fuel Shortage**

When an emergency is declared, the system:

1. Determines the aircraft's current live position.
2. Evaluates available airports.
3. Identifies a suitable nearby airport.
4. Calculates a new route using Dijkstra's algorithm.
5. Diverts the aircraft toward the selected airport.

If the nearest suitable airport is the airport from which the aircraft recently departed, the aircraft can visibly turn around and return.

Once a diversion has been assigned, repeated emergency actions will not continuously change the aircraft's destination.

### Weather Emergency Visualization

Weather emergencies are visually represented on the tactical map using a hazard overlay along the affected flight's remaining route.

---

# Technology Stack

| Technology               | Purpose                                     |
| ------------------------ | ------------------------------------------- |
| **Java**                 | Core application and simulation logic       |
| **JavaFX**               | Desktop GUI and real-time visualization     |
| **CSS**                  | Cyberpunk interface styling                 |
| **JDBC**                 | Database connectivity                       |
| **SQL Database**         | Airport and route data storage              |
| **Maven**                | Build and dependency management             |
| **Dijkstra's Algorithm** | Shortest-path route planning                |
| **PriorityQueue**        | Runway and flight scheduling                |
| **HashMap**              | Fast flight lookup and graph representation |
| **QuickSort**            | Flight ID sorting                           |
| **MergeSort**            | Time-based sorting                          |
| **Multithreading**       | Background collision detection              |

---

# Data Structures & Algorithms

The project applies several data structures and algorithms to simulate an ATC environment.

## Graph

The airport network is modeled as a **weighted graph**.

Each airport represents a vertex, while a connection between two airports represents an edge.

The edge weight is the distance between the two airports.

```text
          250 km
   A ---------------- B
   |                  |
   |                  |
 150 km             180 km
   |                  |
   |                  |
   C ---------------- D
          120 km
```

The graph is represented using an adjacency-list structure.

---

## Dijkstra's Algorithm

Dijkstra's algorithm is used to calculate the shortest route between airports.

It is used for:

* Initial flight route planning
* Multi-hop route calculation
* Emergency diversions
* Collision-avoidance route recalculation

The algorithm uses the **distance stored for each airport connection** as the edge weight.

With a priority queue implementation, the approximate complexity is:

```text
O((V + E) log V)
```

Where:

* `V` = number of airports
* `E` = number of airport connections

---

## HashMap

HashMaps are used for efficient access to application data and graph structures.

For example, airport and flight objects can be accessed through unique identifiers.

Flight ID searches are designed for approximately:

```text
O(1)
```

average-case lookup complexity.

---

## PriorityQueue

Priority queues are used to manage runway scheduling and flight priorities.

This allows flights to be processed according to their assigned priority rather than simply following insertion order.

---

## QuickSort

A custom QuickSort implementation is used to sort flights according to supported flight attributes, including Flight ID.

Average complexity:

```text
O(n log n)
```

---

## MergeSort

A custom MergeSort implementation is used for time-based flight sorting, such as:

* Departure time
* Arrival time

Complexity:

```text
O(n log n)
```

---

## Multithreading

A background thread continuously monitors aircraft positions and checks for potential collisions.

This prevents collision detection from blocking the JavaFX Application Thread.

---

# Database Design

The database is specifically responsible for storing the **static airport and route network**.

It acts as the source of information from which the application builds its graph.

### Airport Information

The airport data contains information such as:

```text
+----------------------+
|       Airports       |
+----------------------+
| Airport ID           |
| Airport Name         |
| Airport Code         |
| X Coordinate         |
| Y Coordinate         |
+----------------------+
```

The X and Y coordinates are used by the JavaFX tactical map to determine where each airport should appear visually.

### Airport Routes

The route data contains connections between airports and their distances:

```text
+----------------------+
|       Routes         |
+----------------------+
| Route ID             |
| Source Airport       |
| Destination Airport  |
| Distance             |
+----------------------+
```

The distance is used as the **weight of the graph edge** for Dijkstra's algorithm.

For example:

```text
Islamabad ---- 280 km ---- Lahore
     |
     |
   190 km
     |
     |
 Peshawar
```

The database provides the information needed to construct these connections when the application starts.

### What the Database Does Not Store

The database is **not** used as persistent storage for:

* Flights
* Flight positions
* Flight status
* Emergency events
* Collision events
* Altitude changes
* Simulation state
* Flight schedules

These are handled dynamically by the Java application while the simulation is running.

---

# System Architecture

```text
                    SQL DATABASE
                         |
                         |
             Airport & Route Information
                         |
                         v
                    JDBC Layer
                         |
                         v
                Flight Graph Creation
                         |
                         v
              +-----------------------+
              |      ATC ENGINE       |
              |                       |
              | Flight Management     |
              | Route Planning        |
              | Scheduling            |
              | Emergencies            |
              | Collision Avoidance   |
              +-----------+-----------+
                          |
                          v
              +-----------------------+
              |      JavaFX GUI       |
              |                       |
              | Control Panel         |
              | Tactical Map          |
              | Flight Manifest       |
              | System Log            |
              +-----------------------+
```

---

# Project Layout

```text
src/
└── com/
    └── atc/
        ├── Main.java
        │   └── JavaFX application entry point
        │
        ├── model/
        │   ├── Airport.java
        │   │   └── Airport model and scheduling structures
        │   │
        │   └── Flight.java
        │       └── Flight data model
        │
        ├── graph/
        │   ├── Edge.java
        │   │   └── Weighted graph edge
        │   │
        │   └── FlightGraph.java
        │       └── Graph representation and Dijkstra's algorithm
        │
        ├── database/
        │   └── Database-related classes
        │       └── JDBC connection and airport/route retrieval
        │
        ├── util/
        │   └── SortUtils.java
        │       └── Custom QuickSort and MergeSort
        │
        ├── engine/
        │   └── ATCEngine.java
        │       └── Simulation engine, movement,
        │           emergencies and collision avoidance
        │
        └── gui/
            ├── MapView.java
            │   └── JavaFX tactical map
            │
            ├── ControlPanel.java
            │   └── Flight generation and emergency controls
            │
            ├── EventLogPanel.java
            │   └── System event log
            │
            └── FlightsTablePanel.java
                └── Flight manifest, search, filtering and sorting

resources/
└── atc-theme.css
    └── Cyberpunk interface styling
```

---

# User Interface

The application uses a three-column tactical command-center interface.

## Left — Control Deck

Provides controls for:

* Generating flights
* Selecting departure and destination airports
* Declaring emergencies
* Selecting emergency types
* Viewing system events

The system log provides real-time feedback about flight and simulation events.

## Center — Tactical Map

The tactical map displays:

* Airports
* Airport connections
* Aircraft
* Flight routes
* Emergency hazards
* Collision warnings
* Aircraft movement

Airport positions are based on their **X and Y coordinates retrieved from the database**.

Flight markers are color-coded according to their current state:

```text
Cyan    → Active
Orange  → Proximity Warning
Red     → Emergency
Green   → Landed
```

## Right — Flight Manifest

The Flight Manifest provides:

* Flight ID search
* Departure airport filtering
* Flight information
* Sorting controls
* Real-time flight status

---

# Application Workflow

## 1. Load Airport Network

When the application starts:

```text
Database
    ↓
Retrieve Airports
    ↓
Retrieve Routes
    ↓
Create Graph
    ↓
Display Airports on Map
```

Airport X/Y coordinates determine their visual positions, while route distances become graph edge weights.

---

## 2. Generate a Flight

The user selects:

```text
Departure Airport
Destination Airport
```

The system:

1. Creates the flight.
2. Searches the airport graph.
3. Runs Dijkstra's algorithm.
4. Generates a multi-hop route if required.
5. Adds the flight to the scheduling system.
6. Displays the aircraft on the tactical map.

---

## 3. Flight Movement

The aircraft moves along its calculated route.

Its position is continuously updated as the simulation progresses.

---

## 4. Collision Detection

The background collision-avoidance thread periodically checks active aircraft.

If two aircraft enter the configured safety distance:

```text
Distance < 30 px
```

the system performs collision avoidance and can dynamically recalculate the affected aircraft's remaining route.

---

## 5. Emergency Diversion

When an emergency occurs:

```text
Aircraft Current Position
            ↓
Find Suitable Airport
            ↓
Calculate New Route
            ↓
Dijkstra
            ↓
Update Flight Route
            ↓
Continue Simulation
```

---

## 6. Landing

When the aircraft reaches its final destination:

```text
Flight Status → LANDED
```

The aircraft is removed from active traffic management and displayed as landed.

---

# Complexity Overview

| Operation           | Data Structure / Algorithm |         Complexity |
| ------------------- | -------------------------- | -----------------: |
| Flight ID lookup    | HashMap                    |       O(1) average |
| Route calculation   | Dijkstra + PriorityQueue   |   O((V + E) log V) |
| QuickSort           | Custom QuickSort           | O(n log n) average |
| MergeSort           | Custom MergeSort           |         O(n log n) |
| Priority scheduling | PriorityQueue              |           O(log n) |
| Collision checking  | Pairwise comparison        |    O(n²) per cycle |

---

# Build & Run

## Requirements

* JDK 17 or later
* Maven
* JavaFX
* SQL database
* Appropriate JDBC driver

---

## Maven

Maven is the recommended way to build and run the project.

```bash
mvn clean javafx:run
```

---

## Manual JavaFX SDK

If JavaFX is configured manually:

```bash
mkdir -p out

javac -d out \
  --module-path /path/to/javafx-sdk/lib \
  --add-modules javafx.controls,javafx.graphics \
  $(find src -name "*.java")
```

Copy the resources:

```bash
cp resources/atc-theme.css out/
```

Run:

```bash
java \
  --module-path /path/to/javafx-sdk/lib \
  --add-modules javafx.controls,javafx.graphics \
  -cp out \
  com.atc.Main
```

---

# Database Configuration

Configure the database connection before running the application.

Typical configuration values include:

```text
Database URL
Database Name
Username
Password
JDBC Driver
```

The application uses JDBC to retrieve the airport and route information required to initialize the graph.

Database credentials should not be hardcoded in the source code. Use environment variables or an external configuration file and exclude sensitive configuration files from version control.

---

# Project Objectives

This project was developed to demonstrate the practical implementation of:

* Object-oriented programming
* Data structures
* Graph theory
* Dijkstra's shortest-path algorithm
* Priority-based scheduling
* HashMap-based searching
* QuickSort
* MergeSort
* Queues
* Multithreading
* Database connectivity
* JDBC
* JavaFX
* Real-time simulation
* Event-driven programming

The database component additionally demonstrates how **persistent airport and route data can be integrated with an in-memory graph structure** to power a real-time simulation.

---

# Future Improvements

Potential future improvements include:

* Real-time weather data
* More airport locations
* Multiple runway management
* Airspace restriction zones
* Advanced collision-resolution algorithms
* Fuel consumption simulation
* Aircraft type and performance modeling
* Real-world airport coordinates
* Real-world flight route data
* Flight replay functionality
* Traffic statistics and analytics
* 3D air-traffic visualization
* Administrator interface for managing airports and routes

---

# Conclusion

The **Smart Air Traffic Control & Flight Scheduling System** combines algorithms, data structures, database systems, multithreading, and graphical visualization into a practical air-traffic simulation.

The database provides the foundation of the simulated airspace by storing **airport information, X/Y coordinates, and distances between connected airports**. This information is loaded into the Java application and transformed into a weighted graph used by Dijkstra's algorithm for intelligent route planning.

The result is an interactive ATC simulation that demonstrates how **database-driven data can work together with algorithms and real-time JavaFX visualization** to create a complete software system.
