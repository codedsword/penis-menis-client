package dev.sinepsinem.util.models;

import dev.sinepsinem.util.MathUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import static dev.sinepsinem.util.traits.Util.mc;

public record Angles(float yRot, float xRot) {
    public Angles(float[] rot) {
        this(rot[0], rot[1]);
    }

    public static Angles pos(Vec3 pos) {
        return new Angles(MathUtil.calcAngle(mc.player.getEyePosition(), pos));
    }

    public static Angles entity(Entity entity) {
        return pos(MathUtil.clamp(mc.player.getEyePosition(), entity.getBoundingBox()));
    }
}
