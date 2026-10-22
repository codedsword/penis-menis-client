package me.alpha432.oyvey.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Custom first person swing animations, item position and swing speed.
 */
public class AnimationsModule extends Module {
    private static AnimationsModule INSTANCE;

    private final Setting<Swing> swing = mode("Swing", Swing.SMOOTH);
    private final Setting<Integer> swingDuration = num("SwingDuration", 6, 1, 20);
    private final Setting<Boolean> customDuration = bool("CustomDuration", false);
    private final Setting<Float> x = num("X", 0.0f, -1.0f, 1.0f);
    private final Setting<Float> y = num("Y", 0.0f, -1.0f, 1.0f);
    private final Setting<Float> z = num("Z", 0.0f, -1.0f, 1.0f);
    private final Setting<Float> scale = num("Scale", 1.0f, 0.3f, 1.5f);

    public AnimationsModule() {
        super("Animations", "Custom hand and swing animations", Category.RENDER);
        swingDuration.setVisibility(v -> customDuration.getValue());
        INSTANCE = this;
    }

    public static AnimationsModule get() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }

    public int swingDuration(int vanilla) {
        return customDuration.getValue() ? swingDuration.getValue() : vanilla;
    }

    public void applyOffset(PoseStack pose, HumanoidArm arm) {
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(invert * x.getValue(), y.getValue(), z.getValue());
        float s = scale.getValue();
        pose.scale(s, s, s);
    }

    /**
     * @return false to let vanilla animate this swing
     */
    public boolean applySwing(PoseStack pose, HumanoidArm arm, float attack) {
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        float sqrtSin = Mth.sin(Mth.sqrt(attack) * Mth.PI);
        float sin = Mth.sin(attack * Mth.PI);

        switch (swing.getValue()) {
            case VANILLA -> {
                return false;
            }
            case SMOOTH -> {
                // vanilla's rotation without the jerky translation
                pose.mulPose(Axis.YP.rotationDegrees(invert * (45.0f + sin * -20.0f)));
                pose.mulPose(Axis.ZP.rotationDegrees(invert * sqrtSin * -20.0f));
                pose.mulPose(Axis.XP.rotationDegrees(sqrtSin * -70.0f));
                pose.mulPose(Axis.YP.rotationDegrees(invert * -45.0f));
            }
            case SLIDE -> {
                pose.translate(invert * 0.1f * sqrtSin, -0.15f * sqrtSin, 0);
                pose.mulPose(Axis.YP.rotationDegrees(invert * 45.0f));
                pose.mulPose(Axis.XP.rotationDegrees(sqrtSin * -40.0f));
                pose.mulPose(Axis.ZP.rotationDegrees(invert * sqrtSin * -25.0f));
                pose.mulPose(Axis.YP.rotationDegrees(invert * -45.0f));
            }
            case STAB -> {
                pose.translate(0, 0.05f * sqrtSin, -0.45f * sin);
                pose.mulPose(Axis.XP.rotationDegrees(sqrtSin * -15.0f));
            }
            case SPIN -> {
                pose.translate(0, 0, -0.1f * sin);
                pose.mulPose(Axis.YP.rotationDegrees(invert * attack * 360.0f));
            }
            case CHOP -> {
                pose.translate(0, 0.1f * sin, 0);
                pose.mulPose(Axis.XP.rotationDegrees(sqrtSin * -90.0f));
            }
        }
        return true;
    }

    @Override
    public String getDisplayInfo() {
        return me.alpha432.oyvey.features.settings.EnumConverter.getProperName(swing.getValue());
    }

    public enum Swing {
        VANILLA,
        SMOOTH,
        SLIDE,
        STAB,
        SPIN,
        CHOP
    }
}
