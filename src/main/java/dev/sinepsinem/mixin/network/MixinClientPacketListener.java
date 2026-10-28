package dev.sinepsinem.mixin.network;

import dev.sinepsinem.event.impl.network.ChatEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import dev.sinepsinem.features.modules.player.NoRotateModule;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.sinepsinem.util.traits.Util.EVENT_BUS;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void sendChat(String content, CallbackInfo ci) {
        if (EVENT_BUS.post(new ChatEvent(content))) {
            ci.cancel();
        }
    }

    @Unique
    private float sinepsinem$yaw, sinepsinem$pitch;

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread", shift = At.Shift.AFTER))
    private void noRotateSave(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        sinepsinem$yaw = mc.player.getYRot();
        sinepsinem$pitch = mc.player.getXRot();
    }

    // the server got its rotation confirmed with the reply, the camera goes back to where it was
    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void noRotateRestore(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!NoRotateModule.active() || mc.player == null) return;
        mc.player.setYRot(sinepsinem$yaw);
        mc.player.setXRot(sinepsinem$pitch);
        mc.player.yRotO = sinepsinem$yaw;
        mc.player.xRotO = sinepsinem$pitch;
    }
}
