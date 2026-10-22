package dev.sinepsinem.mixin.entity;

import dev.sinepsinem.event.impl.entity.player.TravelEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.sinepsinem.util.traits.Util.EVENT_BUS;
import static dev.sinepsinem.util.traits.Util.isLocalPlayer;

@Mixin(AbstractClientPlayer.class)
public class MixinAbstractClientPlayer {
    @Inject(method = "tick", at = @At("HEAD"))
    private void travelPreHook(CallbackInfo ci) {
        if (isLocalPlayer(this)) {
            EVENT_BUS.post(new TravelEvent.Pre());
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void travelPostHook(CallbackInfo ci) {
        if (isLocalPlayer(this)) {
            EVENT_BUS.post(new TravelEvent.Post());
        }
    }
}
