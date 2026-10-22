package dev.sinepsinem.features.modules.player;

import dev.sinepsinem.event.impl.network.PacketEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

public class VelocityModule extends Module {
    public VelocityModule() {
        super("Velocity", "Removes velocity from explosions and entities", Category.PLAYER);
    }

    @Subscribe
    private void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck())
            return;

        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && packet.id() == mc.player.getId()
                || event.getPacket() instanceof ClientboundExplodePacket) {
            event.cancel();
        }
    }
}
