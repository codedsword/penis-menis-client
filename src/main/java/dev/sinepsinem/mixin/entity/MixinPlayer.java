package dev.sinepsinem.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sinepsinem.features.modules.movement.KeepSprintModule;
import dev.sinepsinem.features.modules.movement.NoPushModule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static dev.sinepsinem.util.traits.Util.isLocalPlayer;

@Mixin(Player.class)
public class MixinPlayer {
    @ModifyReturnValue(method = "isPushedByFluid", at = @At("RETURN"))
    private boolean noLiquidPush(boolean original) {
        return original && !(NoPushModule.liquids() && isLocalPlayer(this));
    }

    // a knockback hit slows the attacker to 60%
    @WrapOperation(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 keepSprintMotion(Vec3 movement, double x, double y, double z, Operation<Vec3> original) {
        if (KeepSprintModule.active() && isLocalPlayer(this)) {
            double motion = KeepSprintModule.motion();
            return original.call(movement, motion, y, motion);
        }
        return original.call(movement, x, y, z);
    }

    // ...and stops the sprint
    @WrapWithCondition(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
    private boolean keepSprint(Player player, boolean sprinting) {
        return !(KeepSprintModule.keepsSprint() && isLocalPlayer(this));
    }
}
