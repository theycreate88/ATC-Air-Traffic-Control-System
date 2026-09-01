package com.atc.gui;

import com.atc.engine.ATCEngine;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * Green-on-black "terminal" style scrolling log of engine events (departures,
 * landings, emergencies, collision-avoidance reroutes) -- the classic
 * command-center readout.
 */
public class EventLogPanel extends VBox {

    private final ATCEngine engine;
    private final ListView<String> listView = new ListView<>();

    public EventLogPanel(ATCEngine engine) {
        this.engine = engine;
        getStyleClass().add("glass-panel");
        setSpacing(8);
        setPadding(new Insets(4));

        Label title = new Label("SYSTEM LOG");
        title.getStyleClass().add("panel-title");

        listView.getStyleClass().add("event-log");
        VBox.setVgrow(listView, Priority.ALWAYS);

        getChildren().addAll(title, listView);

        Timeline refreshTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> refresh()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        refresh();
    }

    private void refresh() {
        List<String> log = engine.getEventLog();
        if (log.size() != listView.getItems().size()
                || (!log.isEmpty() && !log.get(0).equals(listView.getItems().isEmpty() ? null : listView.getItems().get(0)))) {
            listView.getItems().setAll(log);
        }
    }
}
