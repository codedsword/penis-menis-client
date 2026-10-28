package dev.sinepsinem.features.modules.movement;

import dev.sinepsinem.event.impl.entity.player.MoveEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.MovementUtil;
import net.minecraft.world.entity.MoverType;
import org.joml.Vector2f;

/**
 * Strafe bunny hops and keeps full speed in any direction while airborne, Ground only speeds up
 * walking.
 */
public class SpeedModule extends Module {
    public enum Mode {
        STRAFE, GROUND
    }

    private final Setting<Mode> mode = mode("Mode", Mode.STRAFE);
    private final Setting<Float> speed = num("Speed", 1.0f, 0.5f, 3.0f);
    private final Setting<Boolean> autoJump = bool("AutoJump", true);

    public SpeedModule() {
        super("Speed", "Moves you faster", Category.MOVEMENT);
    }

    @Subscribe
    public void onMove(MoveEvent event) {
        if (event.getType() != MoverType.SELF || mc.player.isInLiquid() || mc.player.isFallFlying()
                || mc.player.getAbilities().flying || mc.player.isPassenger() || mc.player.onClimbable()) return;
        if (mc.player.input.getMoveVector().lengthSquared() < 1.0E-4f) return;

        double base = MovementUtil.SPRINT_SPEED * MovementUtil.getSpeedFactor(true, true) * speed.getValue();
        double current = event.getMovement().horizontalDistance();
        double y = event.getMovement().y;
        switch (mode.getValue()) {
            case STRAFE -> {
                if (mc.player.onGround() && autoJump.getValue()) {
                    y = MovementUtil.getJumpSpeed();
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, y, mc.player.getDeltaMovement().z);
                    base *= 1.5;
                }
                set(event, Math.max(current, base), y);
            }
            case GROUND -> {
                if (mc.player.onGround()) set(event, Math.max(current, base), y);
            }
        }
    }

    private void set(MoveEvent event, double horizontal, double y) {
        Vector2f motion = MovementUtil.calcMoveVector(mc.player.getYRot(), horizontal);
        event.setMovement(motion.x, y, motion.y);
        mc.player.setDeltaMovement(motion.x, mc.player.getDeltaMovement().y, motion.y);
    }
}
