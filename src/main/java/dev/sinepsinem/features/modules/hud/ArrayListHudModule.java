package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.event.impl.render.Render2DEvent;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.modules.client.HudModule;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.text.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Vector2f;

import java.awt.Color;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enabled modules sorted by width, sliding in and out, in SF Pro.
 */
public class ArrayListHudModule extends HudModule {
    private final Setting<Color> startColor = color("StartColor", 230, 98, 163, 255);
    private final Setting<Color> endColor = color("EndColor", 140, 82, 255, 255);
    private final Setting<Float> speed = num("WaveSpeed", 1.0f, 0.1f, 5.0f);
    private final Setting<Boolean> background = bool("Background", true);
    private final Setting<Boolean> bar = bool("Bar", true);
    private final Setting<Boolean> info = bool("Info", true);

    private final Map<Module, Float> animations = new HashMap<>();
    private long lastFrame = System.nanoTime();

    public ArrayListHudModule() {
        super("ArrayList", "Lists enabled modules", 80, 10);
        pos.setValue(new Vector2f(0.88f, 0.01f));
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);
        GuiGraphicsExtractor graphics = e.getContext();

        long now = System.nanoTime();
        float dt = Math.min((now - lastFrame) / 1.0E9f, 0.1f);
        lastFrame = now;

        for (Module module : SinepSinem.moduleManager.getModules()) {
            boolean visible = module.isEnabled() && module.isDrawn() && !module.hidden && module.getCategory() != Category.HUD
                    && module.getCategory() != Category.CLIENT;
            float current = animations.getOrDefault(module, 0f);
            float target = visible ? 1f : 0f;
            animations.put(module, current + (target - current) * Math.min(1f, dt * 12f));
        }

        List<Module> shown = animations.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.01f)
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparingInt((Module m) -> mc.font.width(label(m))).reversed())
                .toList();

        float screenWidth = mc.getWindow().getGuiScaledWidth();
        boolean right = getX() + getWidth() / 2 > screenWidth / 2f;
        float anchorX = right ? Math.min(getX() + getWidth(), screenWidth - 2) : getX();
        float y = getY();
        int lineHeight = 12;
        int maxWidth = 40;

        for (int i = 0; i < shown.size(); i++) {
            Module module = shown.get(i);
            float anim = animations.get(module);
            Component text = label(module);
            int textWidth = mc.font.width(text);
            maxWidth = Math.max(maxWidth, textWidth + 8);

            float slide = (1 - easeOutCubic(anim)) * (textWidth + 10);
            float x = right ? anchorX - textWidth - 6 + slide : anchorX + 2 - slide;
            int color = wave(i);
            int alpha = Math.round(255 * anim);

            if (background.getValue()) {
                graphics.fill(Math.round(x - 3), Math.round(y), Math.round(x + textWidth + 3), Math.round(y + lineHeight * anim),
                        ARGB.color(Math.round(150 * anim), 0x101010));
            }
            if (bar.getValue()) {
                int barX = right ? Math.round(x + textWidth + 3) : Math.round(x - 4);
                graphics.fill(barX, Math.round(y), barX + 1, Math.round(y + lineHeight * anim), ARGB.color(alpha, color));
            }
            graphics.text(mc.font, text, Math.round(x), Math.round(y + 2), ARGB.color(alpha, color), false);
            y += lineHeight * anim;
        }

        if (right && maxWidth != getWidth()) {
            // keep the right edge in place while the list grows or shrinks
            pos.setValueNoEvent(new Vector2f((anchorX - maxWidth) / screenWidth, pos.getValue().y()));
        }
        setWidth(maxWidth);
        setHeight(Math.max(10, y - getY()));
    }

    private Component label(Module module) {
        MutableComponent text = Fonts.text(module.getDisplayName());
        String extra = module.getDisplayInfo();
        if (info.getValue() && extra != null) {
            text.append(Fonts.text(" " + extra).withColor(0xFF8A8A8A));
        }
        return text;
    }

    private int wave(int index) {
        double time = System.currentTimeMillis() / 1000.0 * speed.getValue();
        float t = (float) (Math.sin(time * 2 + index * 0.35) * 0.5 + 0.5);
        Color a = startColor.getValue(), b = endColor.getValue();
        return ARGB.color(
                Mth.lerpInt(t, a.getRed(), b.getRed()),
                Mth.lerpInt(t, a.getGreen(), b.getGreen()),
                Mth.lerpInt(t, a.getBlue(), b.getBlue()));
    }

    private static float easeOutCubic(float t) {
        float f = 1 - t;
        return 1 - f * f * f;
    }
}
