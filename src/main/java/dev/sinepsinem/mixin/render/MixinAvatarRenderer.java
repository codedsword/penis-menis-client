package dev.sinepsinem.mixin.render;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.util.models.Angles;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.sinepsinem.features.modules.render.NameTagsModule;
import net.minecraft.world.entity.player.Player;

import static dev.sinepsinem.util.traits.Util.mc;

@Mixin(AvatarRenderer.class)
public class MixinAvatarRenderer {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private <T extends Avatar> void extractRenderStateHook(T avatar, AvatarRenderState state, float f, CallbackInfo ci) {
        if (mc.player != avatar) return;
        Angles lerped = SinepSinem.rotationManager.getLerpRenderSnapshot(f);
        if (lerped == null) return;
        state.xRot = lerped.xRot();
        state.yRot = 0;
        state.bodyRot = lerped.yRot();
    }

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
    private void hideReplacedNameTags(Avatar avatar, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        if (avatar instanceof Player player && NameTagsModule.replaces(player)) {
            cir.setReturnValue(false);
        }
    }
}
