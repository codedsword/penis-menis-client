package dev.sinepsinem.features.modules.movement;

import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;

/**
 * Stops other entities, water currents and blocks you stand inside of from pushing you around.
 */
public class NoPushModule extends Module {
    private static NoPushModule INSTANCE;

    private final Setting<Boolean> entities = bool("Entities", true);
    private final Setting<Boolean> liquids = bool("Liquids", true);
    private final Setting<Boolean> blocks = bool("Blocks", true);

    public NoPushModule() {
        super("NoPush", "Entities, liquids and blocks do not push you", Category.MOVEMENT);
        INSTANCE = this;
    }

    public static boolean entities() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.entities.getValue();
    }

    public static boolean liquids() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.liquids.getValue();
    }

    public static boolean blocks() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.blocks.getValue();
    }
}
