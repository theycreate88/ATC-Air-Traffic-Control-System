package com.atc;

import com.atc.engine.ATCEngine;
import com.atc.gui.ControlPanel;
import com.atc.gui.EventLogPanel;
import com.atc.gui.FlightsTablePanel;
import com.atc.gui.MapView;
import com.atc.gui.RadarView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        ATCEngine engine = new ATCEngine();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setPadding(new Insets(14));

        Label header = new Label("AIR TRAFFIC CONTROL SYSTEM");
        header.getStyleClass().add("panel-title");
        header.setStyle("-fx-font-size: 18px; -fx-padding: 0 0 10 0;");
        root.setTop(header);

        ControlPanel controlPanel = new ControlPanel(engine);
        EventLogPanel eventLogPanel = new EventLogPanel(engine);
        VBox leftColumn = new VBox(14, controlPanel, eventLogPanel);
        leftColumn.setPrefWidth(300);
        VBox.setVgrow(eventLogPanel, Priority.ALWAYS);
        root.setLeft(leftColumn);
        BorderPane.setMargin(leftColumn, new Insets(0, 10, 0, 0));

        MapView mapView = new MapView(engine);
        engine.setOnEmergencyDeclared(f -> mapView.focusOnFlight(f.getFlightId()));
        BorderPane centerWrap = new BorderPane(mapView);
        centerWrap.getStyleClass().add("glass-panel");
        root.setCenter(centerWrap);
        BorderPane.setMargin(centerWrap, new Insets(0, 10, 0, 0));

        FlightsTablePanel flightsTablePanel = new FlightsTablePanel(engine);
        RadarView radarView = new RadarView(engine);
        VBox rightColumn = new VBox(14, flightsTablePanel, radarView);
        rightColumn.setPrefWidth(420);
        VBox.setVgrow(radarView, Priority.ALWAYS);
        root.setRight(rightColumn);

        Scene scene = new Scene(root, 1400, 820);
        var css = getClass().getResource("/atc-theme.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("Air Traffic Control & Flight Scheduling System");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            engine.shutdown();
            System.exit(0);
        });
        stage.show();

 
        engine.generateFlight();
        engine.generateFlight();
        engine.generateFlight();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
