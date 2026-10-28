package dev.sinepsinem.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.sinepsinem.features.modules.ghost.HitboxesModule;
import dev.sinepsinem.features.modules.render.ESPModule;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {
    // the glow outline takes its color from the team color
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void espGlowColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (ESPModule.shouldGlow(self)) {
            cir.setReturnValue(ESPModule.glowColor(self));
        }
    }

    // the crosshair picks entities with their box grown by this
    @ModifyReturnValue(method = "getPickRadius", at = @At("RETURN"))
    private float hitboxes(float original) {
        return original + HitboxesModule.expansion((Entity) (Object) this);
    }
}
