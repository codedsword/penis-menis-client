package dev.sinepsinem.features.modules.combat;

import dev.sinepsinem.event.impl.entity.player.MoveEvent;
import dev.sinepsinem.event.impl.render.Render3DEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.TargetUtil;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.List;

/**
 * Circles your target at a fixed radius while you move, aiming at where it will be a few ticks
 * from now. A or D picks the direction, running into a wall flips it. The radius is drawn as a
 * translucent ring around the target, like Ambient's.
 */
public class TargetStrafeModule extends Module {
    public enum Activation {
        ALWAYS, ON_JUMP, ON_MOVE
    }

    public enum Gradient {
        EDGE, SOLID
    }

    public enum Outline {
        FULL, STRIP_ONLY, NONE
    }

    private final Setting<Activation> activation = mode("Activation", Activation.ON_MOVE);
    private final Setting<Float> radius = num("Radius", 2.0f, 0.5f, 5.0f);
    private final Setting<Integer> predictTicks = num("PredictTicks", 6, 0, 20);
    private final Setting<Float> range = num("Range", 6.0f, 2.0f, 12.0f);

    private final Setting<Boolean> renderRadius = bool("RenderRadius", true).group("Render");
    private final Setting<Boolean> fill = bool("Fill", true).group("Render");
    private final Setting<Gradient> gradient = mode("GradientMode", Gradient.EDGE).group("Render");
    private final Setting<Outline> outline = mode("OutlineMode", Outline.FULL).group("Render");
    private final Setting<Float> outlineWidth = num("OutlineWidth", 0.04f, 0.01f, 0.2f).group("Render");
    private final Setting<Integer> stripAngle = num("StripAngle", 45, 10, 90).group("Render");
    private final Setting<Float> height = num("Height", 0.6f, 0.1f, 2.0f).group("Render");
    private final Setting<Color> color = color("RenderColor", 131, 148, 239, 144).group("Render");

    private static final int SEGMENTS = 64;

    private int direction = 1;

    public TargetStrafeModule() {
        super("TargetStrafe", "Circles around your target", Category.COMBAT);
    }

    private @Nullable LivingEntity target() {
        LivingEntity aura = KillAuraModule.getTarget();
        if (aura != null) return aura;
        List<LivingEntity> targets = TargetUtil.targets(range.getValue(), true, false, false, TargetUtil.Priority.DISTANCE);
        return targets.isEmpty() ? null : targets.getFirst();
    }

    private boolean active() {
        return switch (activation.getValue()) {
            case ALWAYS -> true;
            case ON_JUMP -> mc.options.keyJump.isDown();
            case ON_MOVE -> mc.player.input.getMoveVector().lengthSquared() > 1.0E-4f;
        };
    }

    @Subscribe
    public void onMove(MoveEvent event) {
        if (event.getType() != MoverType.SELF || mc.player.isFallFlying() || mc.player.isPassenger()) return;
        LivingEntity target = target();
        if (target == null || !active()) return;

        double speed = event.getMovement().horizontalDistance();
        if (speed < 0.05) speed = mc.player.isSprinting() ? 0.2806 : 0.2158;

        if (mc.options.keyLeft.isDown()) direction = 1;
        else if (mc.options.keyRight.isDown()) direction = -1;
        if (mc.player.horizontalCollision) direction = -direction;

        Vec3 center = target.position().add(target.getDeltaMovement().multiply(1, 0, 1).scale(predictTicks.getValue()));
        double dx = mc.player.getX() - center.x, dz = mc.player.getZ() - center.z;
        double angle = Math.atan2(dz, dx);
        double r = radius.getValue();
        // the next point on the circle, one step of our speed further around it
        double next = angle + direction * speed / Math.max(r, 0.5);
        double tx = center.x + Math.cos(next) * r - mc.player.getX();
        double tz = center.z + Math.sin(next) * r - mc.player.getZ();
        double length = Math.sqrt(tx * tx + tz * tz);
        if (length < 1.0E-4) return;
        double mx = tx / length * speed, mz = tz / length * speed;
        event.setMovement(mx, event.getMovement().y, mz);
        mc.player.setDeltaMovement(mx, mc.player.getDeltaMovement().y, mz);
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!renderRadius.getValue()) return;
        LivingEntity target = target();
        if (target == null) return;

        Vec3 base = target.getPosition(event.getDelta());
        double r = radius.getValue();
        Color c = color.getValue();
        int rgb = c.getRGB() & 0xFFFFFF;
        float alpha = c.getAlpha() / 255f;

        if (fill.getValue()) {
            // the wall of a cylinder in horizontal strips, fading upwards for the edge gradient
            int strips = gradient.getValue() == Gradient.EDGE ? 6 : 1;
            double stripHeight = height.getValue() / strips;
            for (int s = 0; s < strips; s++) {
                float fade = gradient.getValue() == Gradient.EDGE ? (float) Math.pow(1 - (double) s / strips, 2) : 1;
                int fillColor = ARGB.color(Math.round(255 * alpha * 0.55f * fade), rgb);
                double y0 = base.y + s * stripHeight, y1 = y0 + stripHeight;
                ring(base, r, y0, y1, fillColor, 0, 360);
            }
        }

        double width = outlineWidth.getValue();
        int lineColor = ARGB.color(Math.round(255 * Math.min(1, alpha * 1.6f)), rgb);
        switch (outline.getValue()) {
            case FULL -> ring(base, r, base.y, base.y + width, lineColor, 0, 360);
            case STRIP_ONLY -> {
                // dashes every strip angle, slowly turning
                double offset = System.currentTimeMillis() / 20.0 % 360;
                int step = stripAngle.getValue();
                for (int a = 0; a < 360; a += step * 2) {
                    ring(base, r, base.y, base.y + width * 3, lineColor, a + offset, a + offset + step);
                }
            }
            case NONE -> {
            }
        }
    }

    // a band of the cylinder wall between two angles (degrees)
    private static void ring(Vec3 center, double r, double y0, double y1, int argb, double from, double to) {
        int segments = Math.max(2, (int) Math.ceil(SEGMENTS * (to - from) / 360));
        GizmoStyle style = GizmoStyle.fill(argb);
        for (int i = 0; i < segments; i++) {
            double a0 = Math.toRadians(from + (to - from) * i / segments);
            double a1 = Math.toRadians(from + (to - from) * (i + 1) / segments);
            double x0 = center.x + Math.cos(a0) * r, z0 = center.z + Math.sin(a0) * r;
            double x1 = center.x + Math.cos(a1) * r, z1 = center.z + Math.sin(a1) * r;
            Gizmos.rect(new Vec3(x0, y0, z0), new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), new Vec3(x0, y1, z0), style);
        }
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(radius.getValue());
    }
}
