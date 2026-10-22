package me.alpha432.oyvey.util;

import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector2f;

public class MovementUtil implements Util {
    public static final double SPRINT_SPEED = 0.2873;

    public static Vector2f calcMoveVector(float yaw, Vector2f input, double speed) {
        Vector2f direction = new Vector2f(input);

        if (direction.lengthSquared() < 1.0E-8f) {
            return direction.zero();
        }

        direction.normalize();

        float rad = (float) Math.toRadians(yaw);
        float sin = (float) Math.sin(rad);
        float cos = (float) Math.cos(rad);

        float x = (direction.x * cos - direction.y * sin) * (float) speed;
        float z = (direction.y * cos + direction.x * sin) * (float) speed;

        return direction.set(x, z);
    }

    public static Vector2f calcMoveVector(float yaw, ClientInput input, double speed) {
        return calcMoveVector(yaw, new Vector2f(input.getMoveVector().x, input.getMoveVector().y), speed);
    }

    public static Vector2f calcMoveVector(float yaw, double speed) {
        return calcMoveVector(yaw, mc.player.input, speed);
    }

    /**
     * Rewrites {@code input} so that moving with {@code movementYaw} goes (as close as the 8 key
     * directions allow) in the direction the keys pointed relative to {@code cameraYaw}.
     * Both the move vector and the key presses are replaced, the latter are sent to the server.
     */
    public static void correctInput(ClientInput input, float cameraYaw, float movementYaw) {
        Input keys = input.keyPresses;
        float strafe = impulse(keys.left(), keys.right());
        float forward = impulse(keys.forward(), keys.backward());
        if (strafe == 0 && forward == 0) return;

        double[] wanted = worldDirection(strafe, forward, cameraYaw);
        float bestStrafe = 0, bestForward = 0;
        double best = -Double.MAX_VALUE;
        for (int f = -1; f <= 1; f++) {
            for (int s = -1; s <= 1; s++) {
                if (f == 0 && s == 0) continue;
                double[] dir = worldDirection(s, f, movementYaw);
                double dot = dir[0] * wanted[0] + dir[1] * wanted[1];
                if (dot > best) {
                    best = dot;
                    bestStrafe = s;
                    bestForward = f;
                }
            }
        }

        input.keyPresses = new Input(bestForward > 0, bestForward < 0, bestStrafe > 0, bestStrafe < 0,
                keys.jump(), keys.shift(), keys.sprint());
        input.moveVector = new Vec2(bestStrafe, bestForward).normalized();
    }

    private static float impulse(boolean positive, boolean negative) {
        return positive == negative ? 0 : positive ? 1 : -1;
    }

    // same maths as Entity#getInputVector, normalized
    private static double[] worldDirection(float strafe, float forward, float yaw) {
        double sin = Math.sin(Math.toRadians(yaw));
        double cos = Math.cos(Math.toRadians(yaw));
        double x = strafe * cos - forward * sin;
        double z = forward * cos + strafe * sin;
        double length = Math.sqrt(x * x + z * z);
        return new double[]{x / length, z / length};
    }

    public static double getSpeedFactor(boolean swiftness, boolean slowness) {
        double factor = 1.0;

        MobEffectInstance speedEffect = mc.player.getEffect(MobEffects.SPEED);
        if (swiftness && speedEffect != null) {
            factor *= 1.0 + 0.2 * (speedEffect.getAmplifier() + 1);
        }

        MobEffectInstance slowEffect = mc.player.getEffect(MobEffects.SLOWNESS);
        if (slowness && slowEffect != null) {
            factor *= Math.max(0.0, 1.0 - 0.15 * (slowEffect.getAmplifier() + 1));
        }

        return factor;
    }

    public static double getJumpBoost() {
        return mc.player.getJumpBoostPower();
    }

    public static double getJumpSpeed() {
        return mc.player.getAttributeValue(Attributes.JUMP_STRENGTH) * getBlockJumpFactor(mc.player) + getJumpBoost();
    }

    public static double getJumpHeight() {
        return mc.player.getAttributeValue(Attributes.JUMP_STRENGTH) + getJumpBoost();
    }

    private static float getBlockJumpFactor(LivingEntity entity) {
        BlockState inside = entity.level().getBlockState(entity.blockPosition());
        float factor = inside.getBlock().getJumpFactor();
        if (factor != 1.0f) return factor;
        return entity.level().getBlockState(entity.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();
    }
}
