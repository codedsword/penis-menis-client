package dev.sinepsinem.features.modules.movement;

import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;

/**
 * Vanilla slows you to 60% and stops your sprint when a hit deals knockback; this keeps both.
 */
public class KeepSprintModule extends Module {
    private static KeepSprintModule INSTANCE;

    private final Setting<Float> motion = num("Motion", 1.0f, 0.6f, 1.0f);
    private final Setting<Boolean> sprint = bool("KeepSprint", true);

    public KeepSprintModule() {
        super("KeepSprint", "No slowdown when you hit", Category.MOVEMENT);
        INSTANCE = this;
    }

    public static boolean active() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    /**
     * @return the horizontal motion multiplier after a knockback hit, 0.6 in vanilla
     */
    public static double motion() {
        return active() ? INSTANCE.motion.getValue() : 0.6;
    }

    public static boolean keepsSprint() {
        return active() && INSTANCE.sprint.getValue();
    }

    @Override
    public String getDisplayInfo() {
        return Math.round(motion.getValue() * 100) + "%";
    }
}
