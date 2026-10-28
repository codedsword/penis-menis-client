package dev.sinepsinem.manager;

import dev.sinepsinem.util.imgui.Theme;

import java.awt.Color;

/**
 * The client color everywhere outside ImGui (chat prefix, hud editor), the theme's accent.
 */
public class ColorManager {
    public void init() {
    }

    public Color getColor() {
        return Theme.toColor(Theme.accent());
    }

    public int getColorAsInt() {
        return Theme.toArgb(Theme.accent());
    }

    public int getColorAsIntFullAlpha() {
        return getColorAsInt() | 0xFF000000;
    }

    /**
     * @param t 0 for the accent, 1 for the end of the theme gradient
     */
    public int getGradientColor(float t) {
        return Theme.toArgb(Theme.gradient(t));
    }
}
