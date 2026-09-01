package com.atc.gui;

import com.atc.engine.ATCEngine;
import com.atc.model.Airport;
import com.atc.model.Flight;
import com.atc.util.SortUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Right-column data panel: a TableView of every tracked flight, with an
 * O(1)-HashMap-backed ID search box, a departure-airport filter, and buttons
 * that invoke the custom QuickSort / MergeSort implementations.
 */
public class FlightsTablePanel extends VBox {

    private final ATCEngine engine;
    private final TableView<Flight> table = new TableView<>();
    private final ObservableList<Flight> rows = FXCollections.observableArrayList();
    private final TextField searchField = new TextField();
    private final ComboBox<String> departureFilter = new ComboBox<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private String currentSort = "id"; // id | dep | arr
    private String activeFilterAirport = "All";

    private String searchedFlightId = null;
    // Table shows exactly this many rows before its own internal scrollbar kicks
    // in.
    private static final int VISIBLE_ROWS = 8;
    private static final double ROW_HEIGHT = 26;
    private static final double HEADER_HEIGHT = 28;

    public FlightsTablePanel(ATCEngine engine) {
        this.engine = engine;
        getStyleClass().add("glass-panel");
        setSpacing(8);
        setPadding(new Insets(4));

        Label title = new Label("FLIGHT MANIFEST");
        title.getStyleClass().add("panel-title");

        buildTable();
        FlowPane searchRow = buildSearchRow();
        FlowPane sortRow = buildSortRow();

        // Fixed height for exactly VISIBLE_ROWS rows; TableView's own virtualized
        // scrollbar takes over automatically once more rows are added than fit.
        table.setFixedCellSize(ROW_HEIGHT);
        double fixedHeight = HEADER_HEIGHT + ROW_HEIGHT * VISIBLE_ROWS + 2;
        table.setPrefHeight(fixedHeight);
        table.setMinHeight(fixedHeight);
        table.setMaxHeight(fixedHeight);

        getChildren().addAll(title, searchRow, table, sortRow);

        Timeline refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refresh()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        refresh();
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        table.setItems(rows);
        table.getStyleClass().add("table-view");
        table.setPlaceholder(new Label("No flights"));

        TableColumn<Flight, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getFlightId()));

        TableColumn<Flight, String> depCol = new TableColumn<>("DEP");
        depCol.setCellValueFactory(
                c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDeparture().getName()));

        TableColumn<Flight, String> destCol = new TableColumn<>("DEST");
        destCol.setCellValueFactory(
                c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDestination().getName()));

        TableColumn<Flight, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<Flight, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("status-emergency-cell", "status-active-cell",
                        "status-landed-cell", "status-scheduled-cell");
                if (empty || status == null) {
                    setText(null);
                    return;
                }
                setText(status.toUpperCase());
                switch (status) {
                    case Flight.STATUS_EMERGENCY:
                        getStyleClass().add("status-emergency-cell");
                        break;
                    case Flight.STATUS_ACTIVE:
                        getStyleClass().add("status-active-cell");
                        break;
                    case Flight.STATUS_LANDED:
                        getStyleClass().add("status-landed-cell");
                        break;
                    default:
                        getStyleClass().add("status-scheduled-cell");
                }
            }
        });

        TableColumn<Flight, String> depTimeCol = new TableColumn<>("DEP TIME");
        depTimeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                timeFormat.format(new Date(c.getValue().getDepartureTime()))));

        TableColumn<Flight, String> etaCol = new TableColumn<>("ETA");
        etaCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                timeFormat.format(new Date(c.getValue().getExpectedArrivalTime()))));

        TableColumn<Flight, String> altCol = new TableColumn<>("ALT");
        altCol.setCellValueFactory(
                c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getAltitude() + " ft"));

        table.getColumns().addAll(idCol, depCol, destCol, statusCol, depTimeCol, etaCol, altCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private FlowPane buildSearchRow() {
        searchField.setPromptText("search flight ID");
        searchField.getStyleClass().add("text-field");
        searchField.setPrefWidth(140);

        Button searchBtn = new Button("FIND");
        searchBtn.getStyleClass().add("neon-button");
        searchBtn.setOnAction(e -> searchFlight());

        departureFilter.getItems().add("All");
        for (Airport a : engine.getAirports()) {
            departureFilter.getItems().add(a.getName());
        }
        departureFilter.getSelectionModel().selectFirst();
        departureFilter.getStyleClass().add("combo-box");
        departureFilter.setOnAction(e -> {
            activeFilterAirport = departureFilter.getSelectionModel().getSelectedItem();
            refresh();
        });

        FlowPane row = new FlowPane(8, 8, searchField, searchBtn, departureFilter);
        return row;
    }

    private FlowPane buildSortRow() {
        Button sortIdBtn = new Button("SORT: ID");
        sortIdBtn.getStyleClass().add("neon-button");
        sortIdBtn.setOnAction(e -> {
            currentSort = "id";
            refresh();
        });

        Button sortDepBtn = new Button("SORT: DEP TIME");
        sortDepBtn.getStyleClass().add("neon-button");
        sortDepBtn.setOnAction(e -> {
            currentSort = "dep";
            refresh();
        });

        Button sortArrBtn = new Button("SORT: ETA ");
        sortArrBtn.getStyleClass().add("neon-button");
        sortArrBtn.setOnAction(e -> {
            currentSort = "arr";
            refresh();
        });

        return new FlowPane(6, 6, sortIdBtn, sortDepBtn, sortArrBtn);
    }

    private void searchFlight() {
        String id = searchField.getText() == null ? "" : searchField.getText().trim();

        if (id.isEmpty()) {
            searchedFlightId = null;
            refresh();
            return;
        }

        Flight found = engine.findFlightById(id);

        if (found == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "No flight found with ID: " + id);
            alert.showAndWait();
            return;
        }

        searchedFlightId = id;
        rows.setAll(found);
    }

    private void refresh() {

        if (searchedFlightId != null) {

            Flight found = engine.findFlightById(searchedFlightId);

            if (found != null) {
                rows.setAll(found);
            } else {
                rows.clear();
            }

            return;
        }

        List<Flight> flights = new ArrayList<>(engine.getFlights());
        if (!"All".equals(activeFilterAirport)) {
            List<Flight> filtered = new ArrayList<>();
            for (Flight f : flights) {
                if (f.getDeparture().getName().equals(activeFilterAirport)) {
                    filtered.add(f);
                }
            }
            flights = filtered;
        }

        switch (currentSort) {
            case "dep":
                SortUtils.mergeSort(flights, SortUtils.BY_DEPARTURE_TIME);
                break;
            case "arr":
                SortUtils.mergeSort(flights, SortUtils.BY_ARRIVAL_TIME);
                break;
            default:
                SortUtils.quickSort(flights, SortUtils.BY_FLIGHT_ID);
        }

        rows.setAll(flights);
    }
}
