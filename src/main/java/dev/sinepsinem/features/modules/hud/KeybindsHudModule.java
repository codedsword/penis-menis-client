package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.modules.client.ClickGuiModule;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.Theme;
import imgui.ImDrawList;
import org.joml.Vector2f;

import java.util.List;

/**
 * Ambient's "Keybinds": every bound module with its key, the key lit while the module is on.
 */
public class KeybindsHudModule extends ImGuiHudModule {
    private static final float WIDTH = 190, ROW = 22, PAD = 10;

    public KeybindsHudModule() {
        super("Keybinds", "Lists bound modules and their keys", 95, 40);
        pos.setValue(new Vector2f(0.004f, 0.27f));
    }

    @Override
    protected float[] draw(ImDrawList draw, float x, float y) {
        List<Module> bound = SinepSinem.moduleManager.stream()
                .filter(m -> !m.getBind().isEmpty() && !m.hidden && !(m instanceof ClickGuiModule))
                .toList();
        float height = TITLE + Math.max(1, bound.size()) * ROW + 6;
        float top = panel(draw, x, y, WIDTH, height, "Keybinds");
        if (bound.isEmpty()) {
            text(draw, ImGuiImpl.small, x + PAD, top + 4, Theme.TEXT_FAINT, "Nothing bound");
        }
        for (int i = 0; i < bound.size(); i++) {
            Module module = bound.get(i);
            float ry = top + 2 + i * ROW;
            text(draw, ImGuiImpl.regular, x + PAD, ry + 2, Theme.TEXT, module.getDisplayName());
            String key = module.getBind().toString();
            int color = module.isEnabled() ? Theme.accent() : Theme.TEXT_FAINT;
            text(draw, ImGuiImpl.regular, x + WIDTH - PAD - width(ImGuiImpl.regular, key), ry + 2, color, key);
        }
        return new float[]{WIDTH, height};
    }
}
