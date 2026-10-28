package dev.sinepsinem.features.modules.player;

import dev.sinepsinem.features.modules.Module;

/**
 * Server teleports (lag backs, anticheat setbacks) normally snap your camera to the rotation the
 * server sends; this keeps where you were looking. The server still gets its rotation confirmed.
 */
public class NoRotateModule extends Module {
    private static NoRotateModule INSTANCE;

    public NoRotateModule() {
        super("NoRotate", "Keeps your rotation when the server teleports you", Category.PLAYER);
        INSTANCE = this;
    }

    public static boolean active() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }
}
