package me.alpha432.oyvey.mixin.network;

import me.alpha432.oyvey.event.impl.entity.player.AttackEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.alpha432.oyvey.util.traits.Util.EVENT_BUS;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void attackPre(Player player, Entity entity, CallbackInfo ci) {
        if (EVENT_BUS.post(new AttackEvent.Pre(entity))) {
            ci.cancel();
        }
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void attackPost(Player player, Entity entity, CallbackInfo ci) {
        EVENT_BUS.post(new AttackEvent.Post(entity));
    }
}
