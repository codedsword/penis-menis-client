package dev.sinepsinem.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.sinepsinem.features.modules.movement.NoPushModule;
import dev.sinepsinem.features.modules.render.AnimationsModule;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static dev.sinepsinem.util.traits.Util.isLocalPlayer;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    @ModifyReturnValue(method = "getCurrentSwingDuration", at = @At("RETURN"))
    private int swingSpeed(int original) {
        AnimationsModule animations = AnimationsModule.get();
        return animations != null && isLocalPlayer(this) ? animations.swingDuration(original) : original;
    }

    // entities colliding with us skip pushing anything that is not pushable
    @ModifyReturnValue(method = "isPushable", at = @At("RETURN"))
    private boolean noPush(boolean original) {
        return original && !(NoPushModule.entities() && isLocalPlayer(this));
    }
}
