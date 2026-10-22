package me.alpha432.oyvey.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.alpha432.oyvey.features.modules.render.AnimationsModule;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {
    @Inject(method = "applyItemArmTransform", at = @At("TAIL"))
    private void itemOffset(PoseStack poseStack, HumanoidArm arm, float inverseArmHeight, CallbackInfo ci) {
        AnimationsModule animations = AnimationsModule.get();
        if (animations != null) animations.applyOffset(poseStack, arm);
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void customSwing(float attack, PoseStack poseStack, int invert, HumanoidArm arm, CallbackInfo ci) {
        AnimationsModule animations = AnimationsModule.get();
        if (animations != null && animations.applySwing(poseStack, arm, attack)) {
            ci.cancel();
        }
    }
}
