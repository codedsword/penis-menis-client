package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.Theme;
import imgui.ImDrawList;
import imgui.ImFont;
import org.joml.Vector2f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ambient's array list: enabled modules sorted by width, the name in the theme gradient flowing
 * down the list, the mode in light gray after it, a bar along the screen edge and optionally a
 * dark strip behind every line. Lines slide in and out.
 */
public class ArrayListHudModule extends ImGuiHudModule {
    private final Setting<Boolean> background = bool("Background", true);
    private final Setting<Boolean> bar = bool("Bar", true);
    private final Setting<Boolean> info = bool("Info", true);
    private final Setting<Boolean> lowercase = bool("Lowercase", false);
    private final Setting<Boolean> small = bool("Small", false);
    private final Setting<Float> speed = num("WaveSpeed", 1.0f, 0.1f, 5.0f);

    private final Map<Module, Float> animations = new HashMap<>();
    private long lastFrame = System.nanoTime();

    public ArrayListHudModule() {
        super("ArrayList", "Lists enabled modules", 80, 10);
        pos.setValue(new Vector2f(0.88f, 0.004f));
    }

    @Override
    protected float[] draw(ImDrawList draw, float x, float y) {
        long now = System.nanoTime();
        float dt = Math.min((now - lastFrame) / 1.0E9f, 0.1f);
        lastFrame = now;

        for (Module module : SinepSinem.moduleManager.getModules()) {
            boolean visible = module.isEnabled() && module.isDrawn() && !module.hidden
                    && module.getCategory() != Module.Category.HUD && module.getCategory() != Module.Category.CLIENT;
            float current = animations.getOrDefault(module, 0f);
            animations.put(module, current + ((visible ? 1f : 0f) - current) * Math.min(1f, dt * 12f));
        }

        ImFont font = small.getValue() ? ImGuiImpl.small : ImGuiImpl.regular;
        List<Module> shown = animations.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.01f)
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparingDouble((Module m) -> width(font, name(m) + suffix(m))).reversed())
                .toList();

        float screenWidth = mc.getWindow().getWidth();
        boolean right = x + getWidth() * scale / 2 > screenWidth / 2;
        float anchor = right ? Math.min(x + getWidth() * scale, screenWidth - 2) : x;
        float lineHeight = font.getFontSize() + 4, padding = 5, maxWidth = 60;
        float cy = y;

        for (int i = 0; i < shown.size(); i++) {
            Module module = shown.get(i);
            float anim = animations.get(module);
            float ease = 1 - (1 - anim) * (1 - anim) * (1 - anim);
            String name = name(module), suffix = suffix(module);
            float nameWidth = width(font, name), textWidth = nameWidth + width(font, suffix);
            float rowWidth = textWidth + padding * 2 + (bar.getValue() ? 2 : 0);
            maxWidth = Math.max(maxWidth, rowWidth);

            float slide = (1 - ease) * (rowWidth + 10);
            float rx = right ? anchor - rowWidth + slide : anchor - slide;
            float h = lineHeight * ease;
            int color = Theme.withAlpha(Theme.wave(i * 0.35f * speed.getValue() + timeOffset()), anim);

            if (background.getValue()) {
                draw.addRectFilled(rx, cy, rx + rowWidth, cy + h, Theme.rgba(0x000000, Math.round(105 * anim)));
            }
            if (bar.getValue()) {
                float bx = right ? rx + rowWidth - 2 : rx;
                draw.addRectFilled(bx, cy, bx + 2, cy + h, color);
            }
            float tx = rx + padding + (bar.getValue() && !right ? 2 : 0), ty = cy + (h - font.getFontSize()) / 2;
            text(draw, font, tx, ty, color, name);
            if (!suffix.isEmpty()) text(draw, font, tx + nameWidth, ty, Theme.withAlpha(Theme.rgb(0xD0D0D0), anim), suffix);
            cy += h;
        }

        if (right && Math.abs(maxWidth / scale - getWidth()) > 0.01f) {
            // keep the right edge in place while the list grows or shrinks
            float newX = (anchor - maxWidth) / scale;
            pos.setValueNoEvent(new Vector2f(newX / mc.getWindow().getGuiScaledWidth(), pos.getValue().y()));
        }
        return new float[]{maxWidth, Math.max(lineHeight, cy - y)};
    }

    private float timeOffset() {
        return (float) (System.currentTimeMillis() / 1000.0 * (speed.getValue() - 1));
    }

    private String name(Module module) {
        String name = module.getDisplayName();
        return lowercase.getValue() ? name.toLowerCase(Locale.ROOT) : name;
    }

    private String suffix(Module module) {
        String extra = info.getValue() ? module.getDisplayInfo() : null;
        if (extra == null || extra.isEmpty()) return "";
        return " " + (lowercase.getValue() ? extra.toLowerCase(Locale.ROOT) : extra);
    }
}
