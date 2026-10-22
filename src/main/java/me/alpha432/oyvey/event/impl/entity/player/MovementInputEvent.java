package me.alpha432.oyvey.event.impl.entity.player;

import me.alpha432.oyvey.event.Event;
import net.minecraft.client.player.ClientInput;

/**
 * Posted right after the local player's input has been polled, before it is turned into movement
 * and before the key presses are sent to the server.
 */
public class MovementInputEvent extends Event {
    private final ClientInput input;

    public MovementInputEvent(ClientInput input) {
        this.input = input;
    }

    public ClientInput getInput() {
        return input;
    }
}
