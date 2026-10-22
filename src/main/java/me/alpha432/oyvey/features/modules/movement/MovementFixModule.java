package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;

/**
 * Makes movement agree with silent rotations, so the server's movement prediction matches.
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
        super("MovementFix", "Corrects movement for silent rotations", Category.MOVEMENT);
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
        return me.alpha432.oyvey.features.settings.EnumConverter.getProperName(mode.getValue());
    }

    public enum Mode {
        CLASSIC,
        PENIS
    }
}
