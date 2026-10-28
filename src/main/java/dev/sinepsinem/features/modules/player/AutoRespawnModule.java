package dev.sinepsinem.features.modules.player;

import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import net.minecraft.client.gui.screens.DeathScreen;

/**
 * Respawns as soon as you die, Vanilla right away, Delay after a short wait.
 */
public class AutoRespawnModule extends Module {
    public enum Mode {
        VANILLA, DELAY
    }

    private final Setting<Mode> mode = mode("Mode", Mode.VANILLA);
    private final Setting<Integer> delay = num("Delay", 20, 1, 100);

    private int deadTicks;

    public AutoRespawnModule() {
        super("AutoRespawn", "Respawns automatically", Category.PLAYER);
        delay.setVisibility(v -> mode.getValue() == Mode.DELAY);
    }

    @Override
    public void onTick() {
        if (!(mc.gui.screen() instanceof DeathScreen) || mc.player.isAlive()) {
            deadTicks = 0;
            return;
        }
        if (mode.getValue() == Mode.DELAY && ++deadTicks < delay.getValue()) return;
        mc.player.respawn();
        mc.gui.setScreen(null);
        deadTicks = 0;
    }
}
