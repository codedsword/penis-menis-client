package dev.sinepsinem.features.modules.ghost;

import dev.sinepsinem.event.impl.entity.player.AttackEvent;
import dev.sinepsinem.event.impl.entity.player.MovementInputEvent;
import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

/**
 * Resets sprint right after a hit so the next hit deals sprint knockback again. W Tap lets go of
 * forward, S Tap briefly walks back, Sprint only drops the sprint key.
 */
public class SprintResetModule extends Module {
    public enum Mode {
        W_TAP, S_TAP, SPRINT
    }

    private final Setting<Mode> mode = mode("Mode", Mode.W_TAP);
    private final Setting<Integer> ticks = num("Ticks", 2, 1, 6);
    private final Setting<Boolean> onlyPlayers = bool("OnlyPlayers", false);

    private int resetTicks;

    public SprintResetModule() {
        super("SprintReset", "Resets sprint after every hit", Category.GHOST);
    }

    @Override
    public void onDisable() {
        resetTicks = 0;
    }

    @Subscribe
    public void onAttack(AttackEvent.Post event) {
        if (!(event.getTarget() instanceof LivingEntity) || !mc.player.isSprinting()) return;
        if (onlyPlayers.getValue() && !(event.getTarget() instanceof net.minecraft.world.entity.player.Player)) return;
        resetTicks = ticks.getValue();
    }

    @Subscribe
    public void onInput(MovementInputEvent event) {
        if (resetTicks <= 0) return;
        Input keys = event.getInput().keyPresses;
        switch (mode.getValue()) {
            case W_TAP -> {
                event.getInput().keyPresses = new Input(false, keys.backward(), keys.left(), keys.right(), keys.jump(), keys.shift(), false);
                event.getInput().moveVector = new Vec2(event.getInput().moveVector.x, Math.min(0, event.getInput().moveVector.y));
            }
            case S_TAP -> {
                event.getInput().keyPresses = new Input(false, true, keys.left(), keys.right(), keys.jump(), keys.shift(), false);
                event.getInput().moveVector = new Vec2(event.getInput().moveVector.x, -1);
            }
            case SPRINT -> event.getInput().keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
                    keys.jump(), keys.shift(), false);
        }
        mc.player.setSprinting(false);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (resetTicks > 0) resetTicks--;
    }
}
