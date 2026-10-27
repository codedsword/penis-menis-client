package dev.sinepsinem.util.imgui;

import dev.sinepsinem.features.modules.client.InterfaceModule;
import imgui.ImDrawList;
import imgui.ImGui;

/**
 * Drawing helpers shared by the click gui and the hud elements. Coordinates are ImGui pixels,
 * colors are ImGui's packed ABGR (see {@link Theme}).
 */
public final class Draw {
    private Draw() {
    }

    /**
     * The Kawase blurred frame clipped to a rounded rectangle.
     */
    public static void blur(ImDrawList draw, float x1, float y1, float x2, float y2, float rounding, float alpha) {
        float width = ImGui.getIO().getDisplaySizeX(), height = ImGui.getIO().getDisplaySizeY();
        // render targets are stored bottom up
        draw.addImageRounded(ImGuiImpl.TEX_BLUR, x1, y1, x2, y2,
                x1 / width, 1 - y1 / height, x2 / width, 1 - y2 / height,
                Theme.withAlpha(Theme.TEXT, alpha), rounding);
    }

    /**
     * A soft light around a rounded rectangle, nine-sliced out of the radial glow texture.
     *
     * @param texture {@link ImGuiImpl#TEX_GLOW} to add light, {@link ImGuiImpl#TEX_SHADOW} to darken
     * @param fill    whether the inside is covered too (shadows), glows only light the outside
     */
    public static void glow(ImDrawList draw, long texture, float x1, float y1, float x2, float y2,
                            float rounding, float radius, int color, boolean fill) {
        if (radius <= 0 || (color >>> 24) == 0) return;
        // corners are centered on the centers of the rounded corners so the glow follows the rounding
        float inset = Math.min(rounding, Math.min(x2 - x1, y2 - y1) / 2);
        float ix1 = x1 + inset, iy1 = y1 + inset, ix2 = x2 - inset, iy2 = y2 - inset;
        float r = radius + inset;
        float ox1 = ix1 - r, oy1 = iy1 - r, ox2 = ix2 + r, oy2 = iy2 + r;

        draw.addImage(texture, ox1, oy1, ix1, iy1, 0, 0, 0.5f, 0.5f, color);
        draw.addImage(texture, ix2, oy1, ox2, iy1, 0.5f, 0, 1, 0.5f, color);
        draw.addImage(texture, ox1, iy2, ix1, oy2, 0, 0.5f, 0.5f, 1, color);
        draw.addImage(texture, ix2, iy2, ox2, oy2, 0.5f, 0.5f, 1, 1, color);
        if (fill) {
            draw.addImage(texture, ix1, oy1, ix2, iy1, 0.5f, 0, 0.5f, 0.5f, color);
            draw.addImage(texture, ix1, iy2, ix2, oy2, 0.5f, 0.5f, 0.5f, 1, color);
            draw.addImage(texture, ox1, iy1, ix1, iy2, 0, 0.5f, 0.5f, 0.5f, color);
            draw.addImage(texture, ix2, iy1, ox2, iy2, 0.5f, 0.5f, 1, 0.5f, color);
            draw.addImage(texture, ix1, iy1, ix2, iy2, 0.5f, 0.5f, 0.5f, 0.5f, color);
        } else {
            // the edges only cover the outside, starting at the falloff the corners have at the edge
            float edge = 0.5f - 0.5f * inset / r;
            draw.addImage(texture, ix1, oy1, ix2, y1, 0.5f, 0, 0.5f, edge, color);
            draw.addImage(texture, ix1, y2, ix2, oy2, 0.5f, 1 - edge, 0.5f, 1, color);
            draw.addImage(texture, ox1, iy1, x1, iy2, 0, 0.5f, edge, 0.5f, color);
            draw.addImage(texture, x2, iy1, ox2, iy2, 1 - edge, 0.5f, 1, 0.5f, color);
        }
    }

    /**
     * A glowing round light, used for particles.
     */
    public static void light(ImDrawList draw, float x, float y, float radius, int color) {
        draw.addImage(ImGuiImpl.TEX_GLOW, x - radius, y - radius, x + radius, y + radius, 0, 0, 1, 1, color);
    }

    /**
     * The glass panel every custom element sits on: drop shadow, accent glow, blurred backdrop,
     * tint and a hairline border, styled by the Interface module.
     *
     * @param scale ImGui pixels per gui pixel, glow and shadow sizes are in gui pixels
     */
    public static void panel(ImDrawList draw, float x1, float y1, float x2, float y2, float rounding, float alpha, float scale) {
        InterfaceModule style = InterfaceModule.get();
        if (alpha <= 0.003f) return;

        glow(draw, ImGuiImpl.TEX_SHADOW, x1, y1 + 2 * scale, x2, y2 + 2 * scale, rounding, 8 * scale,
                Theme.rgba(0x000000, Math.round(110 * alpha)), true);
        if (style == null || style.glow.getValue()) {
            int glowColor = style == null ? Theme.rgba(0xE662A3, 70) : Theme.fromArgb(style.glowColor.getValue().getRGB());
            float radius = (style == null ? 10 : style.glowRadius.getValue()) * scale;
            glow(draw, ImGuiImpl.TEX_GLOW, x1, y1, x2, y2, rounding, radius, Theme.withAlpha(glowColor, alpha), false);
        }
        blur(draw, x1, y1, x2, y2, rounding, alpha);
        int tint = style == null ? Theme.rgba(0x101010, 120) : Theme.fromArgb(style.tint.getValue().getRGB());
        draw.addRectFilled(x1, y1, x2, y2, Theme.withAlpha(tint, alpha), rounding);
        draw.addRect(x1, y1, x2, y2, Theme.rgba(0xFFFFFF, Math.round(22 * alpha)), rounding, 0, 1.0f);
    }
}
