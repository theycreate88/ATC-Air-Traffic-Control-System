# Smart Air Traffic Control & Flight Scheduling System (JavaFX Cyberpunk Edition)

A Java desktop application simulating an ATC system for a stylized map of
Pakistan, now rendered as a futuristic tactical command-center dashboard in
JavaFX (dark glassmorphic panels, neon cyan/orange/red/green states,
monospace HUD typography). The simulation core is unchanged: graph-based
route planning (Dijkstra), priority-queue runway scheduling, a background
collision-avoidance thread with automatic lane separation for head-on
traffic, weather-emergency diversions, and custom sorting algorithms for the
flights table.

## Project layout

```
src/com/atc/
  Main.java                     - JavaFX Application entry point; builds the 3-column layout
  model/Airport.java            - graph node: FIFO departure queue + runway PriorityQueue
  model/Flight.java             - flight data model
  graph/Edge.java                - weighted graph edge
  graph/FlightGraph.java         - HashMap adjacency list + Dijkstra's algorithm
  util/SortUtils.java            - custom QuickSort / MergeSort
  engine/ATCEngine.java          - simulation core: HashMap registry, movement,
                                    background collision-avoidance thread, emergencies
  gui/MapView.java               - JavaFX Canvas tactical radar display (neon glow rendering)
  gui/ControlPanel.java          - flight generation + emergency controls (left column)
  gui/EventLogPanel.java         - scrolling terminal-style system log (left column)
  gui/FlightsTablePanel.java     - TableView, O(1) search, filters, sort buttons (right column)
resources/
  atc-theme.css                  - cyberpunk color palette, glassmorphic panels, glow effects
```

## Layout

Three-column tactical dashboard inside a `BorderPane`:
- **Left** — Control Deck (generate flight / declare emergency) + System Log terminal
- **Center** — Tactical Map: animated radar display with glowing airports, routes, and
  color-coded flight markers (cyan = active, orange = proximity warning, red = emergency,
  green = landed)
- **Right** — Flight Manifest table with ID search, departure filter, and sort controls

## Build & run

### Option A: Maven (recommended — handles the JavaFX SDK for you)

Requires Maven and JDK 17+.

```bash
mvn clean javafx:run
```

### Option B: Manual javac + JavaFX SDK

Requires a JDK and the [JavaFX SDK](https://openjfx.io/) downloaded separately.

```bash
mkdir -p out
javac -d out --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.graphics $(find src -name "*.java")
cp resources/atc-theme.css out/
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.graphics -cp out com.atc.Main
```

## Using the app

- **+ GENERATE FLIGHT** opens a dialog with Departure/Destination dropdowns; confirming
  creates a flight along the Dijkstra-computed multi-hop route and animates it on the map.
- **Declare Emergency**: pick an active/scheduled flight and a type (Weather or Fuel
  Shortage), then click "Create Emergency". The nearest safe airport is measured from the
  plane's actual live position (not a graph node) — if that's the airport it just departed
  from, the plane visibly turns around mid-air; otherwise it diverts forward. Once a
  diversion is locked in for a flight, repeated clicks won't reassign a new destination.
  A Weather emergency also renders a glowing red/orange hazard overlay along the flight's
  remaining path.
- **Head-on traffic**: flights are automatically offset into parallel lanes (right-hand
  side of their direction of travel), so two flights on the same route in opposite
  directions never actually converge on the same point.
- The **collision-avoidance thread** runs every second in the background, checks all
  active/emergency flight pairs for a separation under 30px, and (on the JavaFX
  Application Thread) bumps one flight's altitude, flashes both flights orange briefly,
  and dynamically recalculates the affected flight's remaining route with Dijkstra.
- The **Flight Manifest** table can be searched by Flight ID (O(1) HashMap lookup),
  filtered by departure airport, and re-sorted using the custom QuickSort (Flight ID) or
  MergeSort (Departure/Arrival Time) implementations.

