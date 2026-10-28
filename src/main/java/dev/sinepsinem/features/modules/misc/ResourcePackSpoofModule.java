package dev.sinepsinem.features.modules.misc;

import dev.sinepsinem.features.modules.Module;

/**
 * Tells the server its resource pack was downloaded and loaded without downloading it.
 */
public class ResourcePackSpoofModule extends Module {
    private static ResourcePackSpoofModule INSTANCE;

    public ResourcePackSpoofModule() {
        super("ResourcePackSpoof", "Skips server resource packs", Category.MISC);
        INSTANCE = this;
    }

    public static boolean active() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }
}
