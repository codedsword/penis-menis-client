package me.alpha432.oyvey.util.imgui;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;

/**
 * Palette taken from legitclient.com: near-black neutrals with a pink accent.
 */
public final class Theme {
    public static final int ACCENT = rgb(0xE662A3);
    public static final int ACCENT_HOVER = rgb(0xCE5893);
    public static final int BACKGROUND = rgb(0x101010);
    public static final int SURFACE = rgb(0x171717);      // neutral-900
    public static final int SURFACE_HOVER = rgb(0x1F1F1F);
    public static final int BORDER = rgb(0x262626);       // neutral-800
    public static final int MUTED = rgb(0x404040);        // neutral-700
    public static final int TEXT = rgb(0xFFFFFF);
    public static final int TEXT_DIM = rgb(0xA3A3A3);     // neutral-400
    public static final int TEXT_FAINT = rgb(0x737373);   // neutral-500

    public static final float ROUNDING = 8.0f;
    public static final float ROUNDING_LARGE = 12.0f;

    private Theme() {
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

    static void apply() {
        ImGuiStyle style = ImGui.getStyle();
        style.setWindowRounding(ROUNDING_LARGE);
        style.setChildRounding(ROUNDING);
        style.setFrameRounding(6.0f);
        style.setPopupRounding(ROUNDING);
        style.setScrollbarRounding(ROUNDING);
        style.setGrabRounding(6.0f);
        style.setTabRounding(6.0f);
        style.setWindowBorderSize(1.0f);
        style.setChildBorderSize(1.0f);
        style.setFrameBorderSize(0.0f);
        style.setPopupBorderSize(1.0f);
        style.setWindowPadding(14.0f, 14.0f);
        style.setFramePadding(8.0f, 5.0f);
        style.setItemSpacing(8.0f, 7.0f);
        style.setItemInnerSpacing(6.0f, 4.0f);
        style.setScrollbarSize(8.0f);
        style.setGrabMinSize(8.0f);
        style.setWindowTitleAlign(0.5f, 0.5f);

        color(style, ImGuiCol.Text, TEXT);
        color(style, ImGuiCol.TextDisabled, TEXT_FAINT);
        color(style, ImGuiCol.WindowBg, BACKGROUND);
        color(style, ImGuiCol.ChildBg, SURFACE);
        color(style, ImGuiCol.PopupBg, SURFACE);
        color(style, ImGuiCol.Border, BORDER);
        color(style, ImGuiCol.BorderShadow, 0);
        color(style, ImGuiCol.FrameBg, BORDER);
        color(style, ImGuiCol.FrameBgHovered, MUTED);
        color(style, ImGuiCol.FrameBgActive, MUTED);
        color(style, ImGuiCol.TitleBg, BACKGROUND);
        color(style, ImGuiCol.TitleBgActive, BACKGROUND);
        color(style, ImGuiCol.TitleBgCollapsed, BACKGROUND);
        color(style, ImGuiCol.ScrollbarBg, 0);
        color(style, ImGuiCol.ScrollbarGrab, BORDER);
        color(style, ImGuiCol.ScrollbarGrabHovered, MUTED);
        color(style, ImGuiCol.ScrollbarGrabActive, ACCENT);
        color(style, ImGuiCol.CheckMark, ACCENT);
        color(style, ImGuiCol.SliderGrab, ACCENT);
        color(style, ImGuiCol.SliderGrabActive, ACCENT_HOVER);
        color(style, ImGuiCol.Button, BORDER);
        color(style, ImGuiCol.ButtonHovered, MUTED);
        color(style, ImGuiCol.ButtonActive, ACCENT);
        color(style, ImGuiCol.Header, BORDER);
        color(style, ImGuiCol.HeaderHovered, MUTED);
        color(style, ImGuiCol.HeaderActive, ACCENT);
        color(style, ImGuiCol.Separator, BORDER);
        color(style, ImGuiCol.TextSelectedBg, rgba(0xE662A3, 90));
        color(style, ImGuiCol.NavHighlight, ACCENT);
    }

    private static void color(ImGuiStyle style, int index, int abgr) {
        style.setColor(index,
                (abgr & 0xFF) / 255f, ((abgr >> 8) & 0xFF) / 255f,
                ((abgr >> 16) & 0xFF) / 255f, ((abgr >>> 24) & 0xFF) / 255f);
    }
}
