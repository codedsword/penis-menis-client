package dev.sinepsinem.mixin.render;

import dev.sinepsinem.features.modules.render.ESPModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void espGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ESPModule.shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }
}
