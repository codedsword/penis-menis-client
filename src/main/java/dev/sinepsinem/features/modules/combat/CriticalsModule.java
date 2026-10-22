package dev.sinepsinem.features.modules.combat;

import dev.sinepsinem.event.impl.entity.player.MovementInputEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import net.minecraft.world.entity.player.Input;

/**
 * Real critical hits: jumps when KillAura has a target and lets it hit only while falling, which
 * is the vanilla crit condition. Faked positions (packet crits) are rejected by movement simulation.
 */
public class CriticalsModule extends Module {
    private static CriticalsModule INSTANCE;

    public CriticalsModule() {
        super("Criticals", "Jumps so KillAura lands critical hits", Category.COMBAT);
        INSTANCE = this;
    }

    /**
     * @return whether KillAura may attack now, false while waiting to fall for a crit
     */
    public static boolean allowsAttack() {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !INSTANCE.canCrit()) return true;
        return !mc.player.onGround() && mc.player.fallDistance > 0 && mc.player.getDeltaMovement().y < 0;
    }

    // conditions under which a jump can turn into a crit at all, see Player#canCriticalAttack
    private boolean canCrit() {
        return !mc.player.isInWater() && !mc.player.onClimbable() && !mc.player.isPassenger()
                && !mc.player.getAbilities().flying && !mc.player.isFallFlying();
    }

    @Subscribe
    public void onInput(MovementInputEvent event) {
        if (KillAuraModule.getTarget() == null || !mc.player.onGround() || !canCrit()) return;
        Input keys = event.getInput().keyPresses;
        event.getInput().keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
                true, keys.shift(), keys.sprint());
    }

    @Override
    public String getDisplayInfo() {
        return "Jump";
    }
}
