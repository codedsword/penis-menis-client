package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.event.impl.render.Render2DEvent;
import dev.sinepsinem.features.modules.client.HudModule;
import dev.sinepsinem.features.modules.client.InterfaceModule;
import dev.sinepsinem.util.imgui.Draw;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.Theme;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.flag.ImDrawFlags;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

/**
 * A hud element in Ambient's "modern" style, drawn with ImGui in its own layer. Text and panels
 * are ImGui; heads and items, which only vanilla can draw, go in {@link #drawVanilla} on top.
 * <p>
 * ImGui works in framebuffer pixels while the hud editor works in gui pixels, {@link #scale}
 * converts between the two.
 */
public abstract class ImGuiHudModule extends HudModule {
    public static final float TITLE = 26;
    public static final float ROUNDING = 8;

    private final ImGuiImpl.Layer layer;
    protected float scale = 1;

    protected ImGuiHudModule(String name, String description, float width, float height) {
        super(name, description, width, height);
        layer = new ImGuiImpl.Layer("hud_" + name, false);
    }

    /**
     * Draws the element with its top left corner at x, y (framebuffer pixels).
     *
     * @return the size drawn, width and height in framebuffer pixels
     */
    protected abstract float[] draw(ImDrawList draw, float x, float y);

    /**
     * Drawn after the ImGui part, in gui pixels.
     */
    protected void drawVanilla(GuiGraphicsExtractor graphics, float x, float y) {
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);
        if (nullCheck()) return;
        scale = mc.getWindow().getGuiScale();
        float x = getX() * scale, y = getY() * scale;
        float[] size = {getWidth() * scale, getHeight() * scale};
        ImGuiImpl.frame(layer, e.getContext(), () -> {
            ImGui.pushFont(ImGuiImpl.regular);
            float[] drawn = draw(ImGui.getBackgroundDrawList(), x, y);
            ImGui.popFont();
            size[0] = drawn[0];
            size[1] = drawn[1];
        });
        setWidth(Math.max(4, size[0] / scale));
        setHeight(Math.max(4, size[1] / scale));
        drawVanilla(e.getContext(), getX(), getY());
    }

    /**
     * Ambient's hud panel: dark, translucent (frosted when the Interface blur is on), rounded,
     * with an optional centered title.
     *
     * @return where the content starts below the title
     */
    protected static float panel(ImDrawList draw, float x, float y, float w, float h, @Nullable String title) {
        InterfaceModule style = InterfaceModule.get();
        if (style != null && style.blur.getValue()) {
            Draw.blur(draw, x, y, x + w, y + h, ROUNDING, 1.0f);
            draw.addRectFilled(x, y, x + w, y + h, Theme.rgba(0x101010, 170), ROUNDING);
        } else {
            draw.addRectFilled(x, y, x + w, y + h, Theme.rgba(0x101010, 200), ROUNDING);
        }
        if (title == null) return y;
        draw.addRectFilled(x, y, x + w, y + TITLE, Theme.rgba(0x0A0A0A, 150), ROUNDING, ImDrawFlags.RoundCornersTop);
        float tw = width(ImGuiImpl.regular, title);
        draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE, x + (w - tw) / 2, y + (TITLE - ImGuiImpl.FONT_SIZE) / 2, Theme.TEXT, title);
        return y + TITLE;
    }

    /**
     * A thin rounded bar filled with the theme gradient.
     */
    protected static void bar(ImDrawList draw, float x, float y, float w, float h, float fraction) {
        draw.addRectFilled(x, y, x + w, y + h, Theme.rgba(0x000000, 120), h / 2);
        float fill = w * Math.clamp(fraction, 0, 1);
        if (fill < 1) return;
        int a = Theme.accent(), b = Theme.lerp(Theme.accent(), Theme.secondary(), fraction);
        // ImGui gradients cannot be rounded, round caps go on the ends of a gradient rect
        draw.addCircleFilled(x + h / 2, y + h / 2, h / 2, a, 12);
        draw.addRectFilledMultiColor(x + h / 2, y, x + Math.max(h / 2, fill - h / 2), y + h, a, b, b, a);
        draw.addCircleFilled(x + Math.max(h / 2, fill - h / 2), y + h / 2, h / 2, b, 12);
    }

    protected static float width(ImFont font, String text) {
        ImGui.pushFont(font);
        float width = ImGui.calcTextSize(text).x;
        ImGui.popFont();
        return width;
    }

    protected static void text(ImDrawList draw, ImFont font, float x, float y, int color, String text) {
        draw.addText(font, Math.round(font.getFontSize()), x, y, color, text);
    }
}
