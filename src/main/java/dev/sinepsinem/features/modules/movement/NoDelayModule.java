package dev.sinepsinem.features.modules.movement;

import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.mixin.entity.LivingEntityAccessor;

/**
 * Removes the 10 tick wait between jumps while holding space, and the cooldown after a missed hit.
 */
public class NoDelayModule extends Module {
    private final Setting<Boolean> jump = bool("Jump", true);
    private final Setting<Boolean> miss = bool("Miss", false);

    public NoDelayModule() {
        super("NoDelay", "No jump delay and no miss cooldown", Category.MOVEMENT);
    }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (jump.getValue()) ((LivingEntityAccessor) mc.player).sinepsinem$setNoJumpDelay(0);
        if (miss.getValue() && mc.missTime > 0 && mc.missTime < 10000) mc.missTime = 0;
    }
}
