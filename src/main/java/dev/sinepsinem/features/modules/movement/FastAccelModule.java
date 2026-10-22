package dev.sinepsinem.features.modules.movement;

import dev.sinepsinem.event.impl.entity.player.MoveEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.MovementUtil;
import net.minecraft.world.entity.MoverType;
import org.joml.Vector2f;

public class FastAccelModule extends Module {
    private final Setting<Float> speed = num("Speed", 1f, 0.5f, 2f);
    private final Setting<Boolean> slowness = bool("Slowness", true);

    public FastAccelModule() {
        super("FastAccel", "Makes the player accelerate faster while walking", Category.MOVEMENT);
    }

    @Subscribe
    private void onMove(MoveEvent event) {
        if (nullCheck() || event.getType() != MoverType.SELF) return;
        if (mc.player.isInLiquid() || mc.player.isFallFlying()
                || mc.player.getAbilities().flying || mc.player.isSpectator()) return;

        double speed = MovementUtil.SPRINT_SPEED * this.speed.getValue() * MovementUtil.getSpeedFactor(true, slowness.getValue());

        if (event.getMovement().horizontalDistance() > speed) {
            speed = event.getMovement().horizontalDistance();
        }

        Vector2f motion = MovementUtil.calcMoveVector(mc.player.getYRot(), speed);
        event.setMovement(motion.x, event.getMovement().y, motion.y);
    }
}
