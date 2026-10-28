package dev.sinepsinem.mixin.network;

import dev.sinepsinem.features.modules.misc.ResourcePackSpoofModule;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class MixinClientCommonPacketListenerImpl {
    @Shadow @Final protected Connection connection;

    // answer like a client that downloaded and applied the pack, without downloading it
    @Inject(method = "handleResourcePackPush", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread", shift = At.Shift.AFTER), cancellable = true)
    private void spoofResourcePack(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        if (!ResourcePackSpoofModule.active()) return;
        for (ServerboundResourcePackPacket.Action action : new ServerboundResourcePackPacket.Action[]{
                ServerboundResourcePackPacket.Action.ACCEPTED,
                ServerboundResourcePackPacket.Action.DOWNLOADED,
                ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED}) {
            connection.send(new ServerboundResourcePackPacket(packet.id(), action));
        }
        ci.cancel();
    }
}
