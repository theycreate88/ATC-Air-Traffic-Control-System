package com.atc.gui;

import com.atc.engine.ATCEngine;
import com.atc.model.Airport;
import com.atc.model.Flight;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Optional;

/**
 * Left-column control deck: flight generation (departure/destination prompt)
 * and emergency declaration (flight + type selectors).
 */
public class ControlPanel extends VBox {

    private final ATCEngine engine;
    private final ComboBox<String> flightSelector = new ComboBox<>();
    private final ComboBox<String> emergencyTypeSelector = new ComboBox<>();

    public ControlPanel(ATCEngine engine) {
        this.engine = engine;
        getStyleClass().add("glass-panel");
        setSpacing(10);
        setPadding(new Insets(4));

        Label title = new Label("CONTROL DECK");
        title.getStyleClass().add("panel-title");

        Button generateBtn = new Button("+ GENERATE FLIGHT");
        generateBtn.getStyleClass().add("neon-button");
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.setOnAction(e -> promptGenerateFlight());

        Separator sep = new Separator();
        sep.getStyleClass().add("section-divider");

        Label emergencyLabel = new Label("DECLARE EMERGENCY");
        emergencyLabel.getStyleClass().add("panel-title");

        Label flightLbl = new Label("FLIGHT");
        flightLbl.getStyleClass().add("field-label");
        flightSelector.getStyleClass().add("combo-box");
        flightSelector.setMaxWidth(Double.MAX_VALUE);
        flightSelector.setPromptText("select flight");

        Label typeLbl = new Label("EMERGENCY TYPE");
        typeLbl.getStyleClass().add("field-label");
        emergencyTypeSelector.getItems().addAll("Weather", "Medical Emergency");
        emergencyTypeSelector.getSelectionModel().selectFirst();
        emergencyTypeSelector.getStyleClass().add("combo-box");
        emergencyTypeSelector.setMaxWidth(Double.MAX_VALUE);

        Button emergencyBtn = new Button("\u26A0 CREATE EMERGENCY");
        emergencyBtn.getStyleClass().add("neon-button-danger");
        emergencyBtn.setMaxWidth(Double.MAX_VALUE);
        emergencyBtn.setOnAction(e -> triggerEmergency());

        getChildren().addAll(title, generateBtn, sep, emergencyLabel,
                flightLbl, flightSelector, typeLbl, emergencyTypeSelector, emergencyBtn);

        Timeline refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshFlightSelector()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        refreshFlightSelector();
    }

    private void refreshFlightSelector() {
        String previouslySelected = flightSelector.getSelectionModel().getSelectedItem();
        flightSelector.getItems().clear();
        for (Flight f : engine.getActiveOrScheduledFlights()) {
            flightSelector.getItems().add(f.getFlightId());
        }
        if (previouslySelected != null && flightSelector.getItems().contains(previouslySelected)) {
            flightSelector.getSelectionModel().select(previouslySelected);
        }
    }

    /** Shows a dialog with two dropdowns (departure / destination) and generates the flight. */
    private void promptGenerateFlight() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Generate Flight");
        dialog.setHeaderText(null);
        if (getClass().getResource("/atc-theme.css") != null) {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/atc-theme.css").toExternalForm());
        }
        dialog.getDialogPane().setStyle("-fx-background-color:#161b22;");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> depBox = new ComboBox<>();
        ComboBox<String> destBox = new ComboBox<>();
        for (Airport a : engine.getAirports()) {
            depBox.getItems().add(a.getName());
            destBox.getItems().add(a.getName());
        }
        depBox.getStyleClass().add("combo-box");
        destBox.getStyleClass().add("combo-box");
        depBox.getSelectionModel().selectFirst();
        if (destBox.getItems().size() > 1) {
            destBox.getSelectionModel().select(1);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        Label depL = new Label("DEPARTURE");
        depL.getStyleClass().add("field-label");
        Label destL = new Label("DESTINATION");
        destL.getStyleClass().add("field-label");
        grid.add(depL, 0, 0);
        grid.add(depBox, 1, 0);
        grid.add(destL, 0, 1);
        grid.add(destBox, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String depName = depBox.getSelectionModel().getSelectedItem();
        String destName = destBox.getSelectionModel().getSelectedItem();
        if (depName == null || destName == null) return;

        if (depName.equals(destName)) {
            showAlert(Alert.AlertType.WARNING, "Departure and destination must be different airports.");
            return;
        }

        Airport dep = engine.findAirport(depName);
        Airport dest = engine.findAirport(destName);
        Flight f = engine.generateFlight(dep, dest);
        if (f == null) {
            showAlert(Alert.AlertType.ERROR, "No valid route could be found between "
                    + depName + " and " + destName + ".");
        }
    }

    private void triggerEmergency() {
        String flightId = flightSelector.getSelectionModel().getSelectedItem();
        if (flightId == null) {
            showAlert(Alert.AlertType.WARNING, "No active flight selected.");
            return;
        }
        String type = emergencyTypeSelector.getSelectionModel().getSelectedItem();
        Flight existing = engine.findFlightById(flightId);
        boolean ok = engine.createEmergency(flightId, type);
        if (!ok) {
            String message = (existing != null && Flight.STATUS_EMERGENCY.equals(existing.getStatus()))
                    ? "Flight " + flightId + " is already diverting to " + existing.getDestination().getName()
                        + ". Its diversion airport is locked in and won't change."
                    : "Could not create emergency for " + flightId;
            showAlert(Alert.AlertType.WARNING, message);
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.getDialogPane().setStyle("-fx-background-color:#161b22; -fx-text-fill:#c9d1d9;");
        alert.showAndWait();
    }
}
