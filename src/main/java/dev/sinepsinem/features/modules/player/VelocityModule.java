package dev.sinepsinem.features.modules.player;

import dev.sinepsinem.event.impl.entity.player.MovementInputEvent;
import dev.sinepsinem.event.impl.network.PacketEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.player.Input;

/**
 * Jump reset: jumping on the tick knockback arrives cancels part of it, using nothing but the jump
 * key. Ignoring velocity packets is rejected by movement simulation.
 */
public class VelocityModule extends Module {
    // set from the network thread
    private volatile boolean jump;

    public VelocityModule() {
        super("Velocity", "Reduces knockback with jump resets", Category.PLAYER);
    }

    @Subscribe
    private void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;
        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && packet.id() == mc.player.getId()
                && packet.movement().horizontalDistanceSqr() > 1.0E-4) {
            jump = true;
        }
    }

    @Subscribe
    public void onInput(MovementInputEvent event) {
        if (!jump) return;
        jump = false;
        if (!mc.player.onGround()) return;
        Input keys = event.getInput().keyPresses;
        event.getInput().keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
                true, keys.shift(), keys.sprint());
    }

    @Override
    public String getDisplayInfo() {
        return "JumpReset";
    }
}
