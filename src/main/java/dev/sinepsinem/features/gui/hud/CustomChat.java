package dev.sinepsinem.features.gui.hud;

import dev.sinepsinem.features.modules.client.InterfaceModule;
import dev.sinepsinem.util.imgui.Draw;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import imgui.ImDrawList;
import imgui.ImGui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Smooth glass chat. Vanilla still lays the chat out and draws the text (so links, hover text
 * and tag tooltips keep working), but everything it draws goes through
 * {@link ChatComponent.ChatGraphicsAccess}, and we put our own access in front of vanilla's:
 * <ul>
 *     <li>the per line black backgrounds become one blurred, rounded panel whose top eases to
 *     the oldest visible line,</li>
 *     <li>new lines slide in from the bottom and fade in while the older ones move up.</li>
 * </ul>
 */
public final class CustomChat implements HudElement {
    public static final CustomChat INSTANCE = new CustomChat();

    private final ImGuiImpl.Layer layer = new ImGuiImpl.Layer("chat", false);
    private final ImGuiImpl.Layer inputLayer = new ImGuiImpl.Layer("chat_input", false);

    // how far the newest lines are still pushed down, in chat units
    private float slide;
    private float panelTop, panelAlpha;
    private long lastFrame = System.nanoTime(), lastPanel;

    private CustomChat() {
    }

    @Override
    public boolean isActive() {
        InterfaceModule module = InterfaceModule.get();
        return module != null && module.chat.getValue();
    }

    @Override
    public ImGuiImpl.Layer layer() {
        return layer;
    }

    @Override
    public void reset() {
        slide = 0;
        lastPanel = 0;
    }

    /**
     * A line was added to the bottom of the chat while it is scrolled all the way down.
     */
    public void onLineAdded() {
        if (!isActive()) return;
        float entryHeight = entryHeight();
        slide = Math.min(slide + entryHeight, entryHeight * 3);
    }

    /**
     * @param graphics null while vanilla only collects clickable text, then only the slide applies
     */
    public ChatComponent.ChatGraphicsAccess wrap(ChatComponent.ChatGraphicsAccess access, @Nullable GuiGraphicsExtractor graphics, int screenHeight) {
        if (graphics != null) {
            long now = System.nanoTime();
            float dt = Math.min((now - lastFrame) / 1.0E9f, 0.1f);
            lastFrame = now;
            slide *= 1 - HudElement.smoothing(dt);
            if (slide < 0.05f) slide = 0;
        }
        float scale = mc.options.chatScale().get().floatValue();
        int chatBottom = Mth.floor((screenHeight - 40) / scale);
        double spacing = mc.options.chatLineSpacing().get();
        int entryBottomToMessageY = (int) Math.round(8.0 * (spacing + 1.0) - 4.0 * spacing);
        return new Access(access, graphics, chatBottom, entryHeight(), entryBottomToMessageY, slide);
    }

    /**
     * The chat screen's input box as a glass panel instead of a flat fill.
     */
    public void inputBox(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2) {
        float rounding = HudElement.rounding();
        ImGuiImpl.frame(inputLayer, graphics, () -> {
            float s = mc.getWindow().getGuiScale();
            ImDrawList draw = ImGui.getBackgroundDrawList();
            draw.pushClipRect((x1 - 20) * s, (y1 - 20) * s, (x2 + 20) * s, (y2 + 20) * s);
            Draw.panel(draw, x1 * s, y1 * s, x2 * s, y2 * s, rounding * s, 1.0f, s);
            draw.popClipRect();
        });
    }

    private void drawPanel(GuiGraphicsExtractor graphics, Vector2f min, Vector2f max, float alpha) {
        long now = System.nanoTime();
        float dt = Math.min((now - lastPanel) / 1.0E9f, 0.1f);
        float t = HudElement.smoothing(dt);
        // nothing was drawn for a while (chat faded out): start over instead of easing from stale values
        if (now - lastPanel > 150_000_000L) {
            panelTop = min.y;
            panelAlpha = 0;
        } else {
            panelTop += (min.y - panelTop) * t;
        }
        panelAlpha += (alpha - panelAlpha) * t;
        lastPanel = now;

        float top = panelTop, opacity = panelAlpha, rounding = HudElement.rounding();
        chrome(graphics, c -> {
            ImDrawList draw = c.draw();
            float s = c.scale();
            draw.pushClipRect((min.x - 20) * s, (Math.min(top, min.y) - 20) * s, (max.x + 20) * s, (max.y + 20) * s);
            Draw.panel(draw, min.x * s, top * s, max.x * s, max.y * s, rounding * s, opacity, s);
            draw.popClipRect();
        });
    }

    private static int entryHeight() {
        return (int) (9 * (mc.options.chatLineSpacing().get() + 1.0));
    }

    private final class Access implements ChatComponent.ChatGraphicsAccess {
        private final ChatComponent.ChatGraphicsAccess delegate;
        private final @Nullable GuiGraphicsExtractor graphics;
        private final int chatBottom, entryHeight, entryBottomToMessageY;
        private final float slide;

        private boolean flushed;
        private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        private float alpha;

        private Access(ChatComponent.ChatGraphicsAccess delegate, @Nullable GuiGraphicsExtractor graphics,
                       int chatBottom, int entryHeight, int entryBottomToMessageY, float slide) {
            this.delegate = delegate;
            this.graphics = graphics;
            this.chatBottom = chatBottom;
            this.entryHeight = entryHeight;
            this.entryBottomToMessageY = entryBottomToMessageY;
            this.slide = slide;
        }

        @Override
        public void updatePose(Consumer<Matrix3x2f> updater) {
            delegate.updatePose(updater);
        }

        @Override
        public void fill(int x0, int y0, int x1, int y1, int color) {
            // everything black drawn before the first line of text is a line background
            if (!flushed && (color & 0xFFFFFF) == 0) {
                minX = Math.min(minX, Math.min(x0, x1));
                minY = Math.min(minY, Math.min(y0, y1));
                maxX = Math.max(maxX, Math.max(x0, x1));
                maxY = Math.max(maxY, Math.max(y0, y1));
                alpha = Math.max(alpha, ARGB.alpha(color) / 255.0f);
                return;
            }
            delegate.fill(x0, y0, x1, y1, color);
        }

        @Override
        public boolean handleMessage(int textTop, float opacity, FormattedCharSequence message) {
            flush();
            int line = Math.round((chatBottom - entryBottomToMessageY - textTop) / (float) entryHeight);
            return delegate.handleMessage(textTop, opacity * fade(line), message);
        }

        @Override
        public void handleTag(int x0, int y0, int x1, int y1, float opacity, GuiMessageTag tag) {
            flush();
            int line = Math.round((chatBottom - y1) / (float) entryHeight);
            delegate.handleTag(x0, y0, x1, y1, opacity * fade(line), tag);
        }

        @Override
        public void handleTagIcon(int left, int bottom, boolean forceVisible, GuiMessageTag tag, GuiMessageTag.Icon icon) {
            flush();
            delegate.handleTagIcon(left, bottom, forceVisible, tag, icon);
        }

        // line 0 is the newest, it fades in while it slides up into place
        private float fade(int line) {
            if (slide <= 0 || line < 0) return 1;
            return Mth.clamp(1 - (slide - line * entryHeight) / entryHeight, 0, 1);
        }

        // the backgrounds are all in, draw the panel below the text that follows
        private void flush() {
            if (flushed) return;
            flushed = true;
            if (graphics != null && minX < maxX && minY < maxY) {
                float background = mc.options.textBackgroundOpacity().get().floatValue();
                float panelAlpha = background <= 0 ? 0 : Math.min(1, alpha / background);
                Matrix3x2f pose = graphics.pose();
                drawPanel(graphics,
                        pose.transformPosition(minX, minY, new Vector2f()),
                        pose.transformPosition(maxX, maxY, new Vector2f()), panelAlpha);
            }
            if (slide > 0) delegate.updatePose(pose -> pose.translate(0, slide));
        }
    }
}
