package dev.sinepsinem.features.modules.player;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.features.modules.Module;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class NoFallModule extends Module {
    public NoFallModule() {
        super("NoFall", "Removes fall damage", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (!mc.player.onGround() && SinepSinem.positionManager.getFallDistance() > 3) {
            boolean bl = mc.player.horizontalCollision;
            ServerboundMovePlayerPacket.PosRot packet = new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY() + 0.000000001, mc.player.getZ(),
                    mc.player.getYRot(), mc.player.getXRot(), false, bl);
            mc.player.connection.send(packet);
        }
    }
}
