package dev.sinepsinem.features.modules.movement;

import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;

/**
 * Picks how movement follows silent rotations. Movement always uses the rotation that is sent to the
 * server (anything else fails simulation checks), this only decides what your keys do with it.
 * Classic is used while the module is off.
 * <ul>
 *     <li>Classic: the player is moved with the rotation's yaw, but the key presses are remapped
 *     so you still walk where the camera is looking.</li>
 *     <li>Penis: no input remapping, you move along the look vector of the silent rotation
 *     (pressing W walks towards whatever is being rotated at) while the camera stays put.</li>
 * </ul>
 */
public class MovementFixModule extends Module {
    private static MovementFixModule INSTANCE;

    private final Setting<Mode> mode = mode("Mode", Mode.CLASSIC);

    public MovementFixModule() {
        super("MovementFix", "Picks how you move during silent rotations", Category.MOVEMENT);
        INSTANCE = this;
    }

    public static MovementFixModule getInstance() {
        return INSTANCE;
    }

    public Mode getMode() {
        return mode.getValue();
    }

    @Override
    public String getDisplayInfo() {
        return dev.sinepsinem.features.settings.EnumConverter.getProperName(mode.getValue());
    }

    public enum Mode {
        CLASSIC,
        PENIS
    }
}
