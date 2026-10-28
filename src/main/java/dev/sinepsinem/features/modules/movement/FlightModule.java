package dev.sinepsinem.features.modules.movement;

import dev.sinepsinem.event.impl.entity.player.MoveEvent;
import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.MovementUtil;
import net.minecraft.world.entity.MoverType;
import org.joml.Vector2f;

/**
 * Vanilla turns on creative flight, Motion sets your velocity straight from your keys.
 */
public class FlightModule extends Module {
    public enum Mode {
        VANILLA, MOTION
    }

    private final Setting<Mode> mode = mode("Mode", Mode.MOTION);
    private final Setting<Float> speed = num("Speed", 1.0f, 0.1f, 5.0f);
    private final Setting<Float> vertical = num("Vertical", 0.6f, 0.1f, 3.0f);
    private final Setting<Boolean> antiKick = bool("AntiKick", true);

    private int ticks;

    public FlightModule() {
        super("Flight", "Lets you fly", Category.MOVEMENT);
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().mayfly = false;
        }
        mc.player.getAbilities().setFlyingSpeed(0.05f);
    }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        ticks++;
        if (mode.getValue() == Mode.VANILLA) {
            mc.player.getAbilities().mayfly = true;
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlyingSpeed(0.05f * speed.getValue());
        }
    }

    @Subscribe
    public void onMove(MoveEvent event) {
        if (mode.getValue() != Mode.MOTION || event.getType() != MoverType.SELF) return;
        double y = 0;
        if (mc.options.keyJump.isDown()) y += vertical.getValue();
        if (mc.options.keyShift.isDown()) y -= vertical.getValue();
        // drop a little every second so vanilla's flying kick never sees you hovering
        if (antiKick.getValue() && y == 0 && ticks % 40 == 0) y = -0.04;
        Vector2f horizontal = MovementUtil.calcMoveVector(mc.player.getYRot(), speed.getValue());
        event.setMovement(horizontal.x, y, horizontal.y);
        mc.player.setDeltaMovement(horizontal.x, y, horizontal.y);
        mc.player.fallDistance = 0;
    }
}
