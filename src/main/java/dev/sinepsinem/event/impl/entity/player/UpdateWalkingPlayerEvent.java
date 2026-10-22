package dev.sinepsinem.event.impl.entity.player;

import dev.sinepsinem.event.Event;
import dev.sinepsinem.event.Stage;

public class UpdateWalkingPlayerEvent extends Event {
    private final Stage stage;

    public UpdateWalkingPlayerEvent(Stage stage) {
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }
}
