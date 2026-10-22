package dev.sinepsinem.features.modules.movement;

import dev.sinepsinem.event.impl.entity.player.MovementInputEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import net.minecraft.world.entity.player.Input;

/**
 * Holds the sprint key for you, vanilla still decides whether you may sprint.
 */
public class SprintModule extends Module {
    public SprintModule() {
        super("Sprint", "Automatically sprints", Category.MOVEMENT);
    }

    @Subscribe
    public void onInput(MovementInputEvent event) {
        Input keys = event.getInput().keyPresses;
        if (keys.sprint()) return;
        event.getInput().keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
                keys.jump(), keys.shift(), true);
    }
}
