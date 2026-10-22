package dev.sinepsinem.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sinepsinem.features.modules.render.AnimationsModule;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// spears and other stab items use their own first person attack animation instead of swingArm
@Mixin(SpearAnimations.class)
public class MixinSpearAnimations {
    @Inject(method = "firstPersonAttack", at = @At("HEAD"), cancellable = true)
    private static void customStab(float attack, PoseStack poseStack, int invert, HumanoidArm arm, CallbackInfo ci) {
        AnimationsModule animations = AnimationsModule.get();
        if (animations != null && animations.applySwing(poseStack, arm, attack)) {
            ci.cancel();
        }
    }
}
