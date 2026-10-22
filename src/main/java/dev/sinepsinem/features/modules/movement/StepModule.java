package dev.sinepsinem.features.modules.movement;

import dev.sinepsinem.features.modules.Module;

/**
 * Walks up blocks by jumping, through vanilla's auto jump. Raising the step height is rejected by
 * movement simulation.
 */
public class StepModule extends Module {
    private boolean previous;

    public StepModule() {
        super("Step", "Jumps up blocks automatically", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        previous = mc.options.autoJump().get();
        mc.options.autoJump().set(true);
    }

    @Override
    public void onDisable() {
        mc.options.autoJump().set(previous);
    }
}
