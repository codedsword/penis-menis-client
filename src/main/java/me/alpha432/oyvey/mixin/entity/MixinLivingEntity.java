package me.alpha432.oyvey.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.alpha432.oyvey.features.modules.render.AnimationsModule;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static me.alpha432.oyvey.util.traits.Util.isLocalPlayer;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    @ModifyReturnValue(method = "getCurrentSwingDuration", at = @At("RETURN"))
    private int swingSpeed(int original) {
        AnimationsModule animations = AnimationsModule.get();
        return animations != null && isLocalPlayer(this) ? animations.swingDuration(original) : original;
    }
}
