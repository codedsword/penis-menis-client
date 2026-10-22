package me.alpha432.oyvey.util;

import me.alpha432.oyvey.util.models.Angles;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RotationUtil implements Util {
    private RotationUtil() {
    }

    /**
     * Moves from {@code from} towards {@code to} by at most the given speeds, the result is
     * snapped to the mouse sensitivity grid so it is a rotation a real mouse could produce.
     */
    public static Angles smooth(Angles from, Angles to, float yawSpeed, float pitchSpeed) {
        float yawDelta = Mth.wrapDegrees(to.yRot() - from.yRot());
        float pitchDelta = to.xRot() - from.xRot();
        yawDelta = Mth.clamp(yawDelta, -yawSpeed, yawSpeed);
        pitchDelta = Mth.clamp(pitchDelta, -pitchSpeed, pitchSpeed);

        float gcd = gcd();
        yawDelta -= yawDelta % gcd;
        pitchDelta -= pitchDelta % gcd;
        return new Angles(from.yRot() + yawDelta, Mth.clamp(from.xRot() + pitchDelta, -90f, 90f));
    }

    public static float gcd() {
        double sensitivity = mc.options.sensitivity().get() * 0.6 + 0.2;
        return (float) (sensitivity * sensitivity * sensitivity * 8.0 * 0.15);
    }

    public static Vec3 lookVector(Angles angles) {
        float pitch = angles.xRot() * Mth.DEG_TO_RAD;
        float yaw = -angles.yRot() * Mth.DEG_TO_RAD;
        float cosYaw = Mth.cos(yaw), sinYaw = Mth.sin(yaw);
        float cosPitch = Mth.cos(pitch), sinPitch = Mth.sin(pitch);
        return new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }

    /**
     * @return whether looking with {@code angles} from the eyes hits {@code entity} within range
     */
    public static boolean isLookingAt(Angles angles, Entity entity, double range) {
        Vec3 eyes = mc.player.getEyePosition();
        AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
        if (box.contains(eyes)) return true;
        return box.clip(eyes, eyes.add(lookVector(angles).scale(range))).isPresent();
    }

    /**
     * Angles to the point of the hitbox closest to the current server rotation, which keeps
     * rotations small and natural.
     */
    public static Angles toEntity(Entity entity, Angles current) {
        Vec3 eyes = mc.player.getEyePosition();
        AABB box = entity.getBoundingBox().deflate(0.05);
        Vec3 look = eyes.add(lookVector(current).scale(eyes.distanceTo(box.getCenter())));
        return Angles.pos(MathUtil.clamp(look, box));
    }
}
