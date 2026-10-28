package dev.sinepsinem.util.imgui;

import dev.sinepsinem.features.modules.client.ClickGuiModule;
import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;

import java.awt.Color;

/**
 * Ambient's palette, sampled from its client screenshots: flat near-black panels with a single
 * accent (a hot pink by default) that also runs as a gradient into a second color for the
 * array list, bars and highlights. Colors are ImGui's packed ABGR.
 */
public final class Theme {
    public static final int PANEL = rgb(0x0F0F0F);
    public static final int HEADER = rgb(0x0A0A0A);
    /** dropdowns, setting groups, text fields */
    public static final int FIELD = rgb(0x1B1B1B);
    public static final int FIELD_HOVER = rgb(0x262626);
    public static final int TRACK = rgb(0x070707);
    public static final int TOGGLE_OFF = rgb(0x404040);
    public static final int KNOB_OFF = rgb(0x9F9F9F);
    public static final int BORDER = rgb(0x262626);
    public static final int TEXT = rgb(0xFFFFFF);
    public static final int TEXT_DIM = rgb(0xA3A3A3);
    public static final int TEXT_FAINT = rgb(0x6B6B6B);

    // the older screens (alt manager) were drawn with these names
    public static final int BACKGROUND = PANEL;
    public static final int SURFACE = rgb(0x151515);
    public static final int SURFACE_HOVER = FIELD;
    public static final int MUTED = TOGGLE_OFF;

    public static final float ROUNDING = 6.0f;
    public static final float ROUNDING_LARGE = 8.0f;

    private static final int DEFAULT_ACCENT = 0xFF0086;
    private static final int DEFAULT_SECONDARY = 0xFF001C;

    private Theme() {
    }

    /**
     * @return the accent color, Ambient pink unless changed in the ClickGui module
     */
    public static int accent() {
        ClickGuiModule gui = ClickGuiModule.getInstance();
        return rgb(gui == null ? DEFAULT_ACCENT : gui.accentRgb());
    }

    /**
     * @return where the accent gradient ends
     */
    public static int secondary() {
        ClickGuiModule gui = ClickGuiModule.getInstance();
        return rgb(gui == null ? DEFAULT_SECONDARY : gui.secondaryRgb());
    }

    /**
     * @param t 0 for the accent, 1 for the secondary color
     */
    public static int gradient(float t) {
        return lerp(accent(), secondary(), t);
    }

    /**
     * The gradient flowing over time, {@code offset} shifts it per line or per pixel.
     */
    public static int wave(float offset) {
        double time = System.currentTimeMillis() / 1000.0;
        float t = (float) (Math.sin(time * 2.0 + offset) * 0.5 + 0.5);
        return gradient(t);
    }

    /**
     * The accent darkened, Ambient draws toggle tracks (x0.62) and slider fills (x0.42) this way.
     */
    public static int accentShade(float factor) {
        return shade(accent(), factor);
    }

    public static int shade(int abgr, float factor) {
        int r = Math.round((abgr & 0xFF) * factor);
        int g = Math.round(((abgr >> 8) & 0xFF) * factor);
        int b = Math.round(((abgr >> 16) & 0xFF) * factor);
        return (abgr & 0xFF000000) | b << 16 | g << 8 | r;
    }

    /**
     * @return 0xRRGGBB as ImGui's packed ABGR with full alpha
     */
    public static int rgb(int rgb) {
        return rgba(rgb, 255);
    }

    public static int rgba(int rgb, int alpha) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return (alpha & 0xFF) << 24 | b << 16 | g << 8 | r;
    }

    public static int withAlpha(int abgr, float alpha) {
        int a = Math.round(((abgr >>> 24) & 0xFF) * Math.max(0, Math.min(1, alpha)));
        return (abgr & 0x00FFFFFF) | a << 24;
    }

    public static int lerp(int from, int to, float t) {
        t = Math.max(0, Math.min(1, t));
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int a = (from >>> shift) & 0xFF, b = (to >>> shift) & 0xFF;
            result |= Math.round(a + (b - a) * t) << shift;
        }
        return result;
    }

    public static int fromArgb(int argb) {
        return rgba(argb & 0xFFFFFF, (argb >>> 24) & 0xFF);
    }

    public static int toArgb(int abgr) {
        int r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF, a = (abgr >>> 24) & 0xFF;
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static Color toColor(int abgr) {
        return new Color(toArgb(abgr), true);
    }

    static void apply() {
        ImGuiStyle style = ImGui.getStyle();
        style.setWindowRounding(ROUNDING_LARGE);
        style.setChildRounding(ROUNDING);
        style.setFrameRounding(ROUNDING);
        style.setPopupRounding(ROUNDING);
        style.setScrollbarRounding(ROUNDING);
        style.setGrabRounding(ROUNDING);
        style.setTabRounding(ROUNDING);
        style.setWindowBorderSize(0.0f);
        style.setChildBorderSize(0.0f);
        style.setFrameBorderSize(0.0f);
        style.setPopupBorderSize(1.0f);
        style.setWindowPadding(14.0f, 14.0f);
        style.setFramePadding(8.0f, 5.0f);
        style.setItemSpacing(8.0f, 7.0f);
        style.setItemInnerSpacing(6.0f, 4.0f);
        style.setScrollbarSize(6.0f);
        style.setGrabMinSize(8.0f);
        style.setWindowTitleAlign(0.5f, 0.5f);

        color(style, ImGuiCol.Text, TEXT);
        color(style, ImGuiCol.TextDisabled, TEXT_FAINT);
        color(style, ImGuiCol.WindowBg, PANEL);
        color(style, ImGuiCol.ChildBg, 0);
        color(style, ImGuiCol.PopupBg, PANEL);
        color(style, ImGuiCol.Border, BORDER);
        color(style, ImGuiCol.BorderShadow, 0);
        color(style, ImGuiCol.FrameBg, FIELD);
        color(style, ImGuiCol.FrameBgHovered, FIELD_HOVER);
        color(style, ImGuiCol.FrameBgActive, FIELD_HOVER);
        color(style, ImGuiCol.TitleBg, HEADER);
        color(style, ImGuiCol.TitleBgActive, HEADER);
        color(style, ImGuiCol.TitleBgCollapsed, HEADER);
        color(style, ImGuiCol.ScrollbarBg, 0);
        color(style, ImGuiCol.ScrollbarGrab, BORDER);
        color(style, ImGuiCol.ScrollbarGrabHovered, TOGGLE_OFF);
        color(style, ImGuiCol.Button, FIELD);
        color(style, ImGuiCol.ButtonHovered, FIELD_HOVER);
        color(style, ImGuiCol.Header, FIELD);
        color(style, ImGuiCol.HeaderHovered, FIELD_HOVER);
        color(style, ImGuiCol.Separator, BORDER);
        refresh();
    }

    /**
     * Re-applies the accent dependent style colors, the accent can change at any time.
     */
    public static void refresh() {
        ImGuiStyle style = ImGui.getStyle();
        int accent = accent();
        color(style, ImGuiCol.ScrollbarGrabActive, accent);
        color(style, ImGuiCol.CheckMark, accent);
        color(style, ImGuiCol.SliderGrab, accent);
        color(style, ImGuiCol.SliderGrabActive, accent);
        color(style, ImGuiCol.ButtonActive, accentShade(0.62f));
        color(style, ImGuiCol.HeaderActive, accentShade(0.62f));
        color(style, ImGuiCol.TextSelectedBg, withAlpha(accent, 0.35f));
        color(style, ImGuiCol.NavHighlight, accent);
    }

    private static void color(ImGuiStyle style, int index, int abgr) {
        style.setColor(index,
                (abgr & 0xFF) / 255f, ((abgr >> 8) & 0xFF) / 255f,
                ((abgr >> 16) & 0xFF) / 255f, ((abgr >>> 24) & 0xFF) / 255f);
    }
}
