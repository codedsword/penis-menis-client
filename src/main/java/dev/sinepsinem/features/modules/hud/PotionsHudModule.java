package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.Theme;
import imgui.ImDrawList;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Vector2f;

import java.util.Comparator;
import java.util.List;

/**
 * Active effects as text, "Speed II 1:46": the name in the effect's color, the time in white.
 */
public class PotionsHudModule extends ImGuiHudModule {
    private static final String[] NUMERALS = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

    public PotionsHudModule() {
        super("Potions", "Lists your active effects", 60, 10);
        pos.setValue(new Vector2f(0.004f, 0.05f));
    }

    @Override
    protected float[] draw(ImDrawList draw, float x, float y) {
        List<MobEffectInstance> effects = mc.player.getActiveEffects().stream()
                .sorted(Comparator.comparingInt(MobEffectInstance::getDuration))
                .toList();
        float lineHeight = ImGuiImpl.FONT_SIZE + 2, width = 20, cy = y;
        for (MobEffectInstance effect : effects) {
            int amplifier = effect.getAmplifier();
            String name = effect.getEffect().value().getDisplayName().getString()
                    + (amplifier < NUMERALS.length ? NUMERALS[amplifier] : " " + (amplifier + 1)) + " ";
            String time = effect.isInfiniteDuration() ? "∞" : StringUtil.formatTickDuration(effect.getDuration(), 20);
            int color = Theme.lerp(Theme.fromArgb(effect.getEffect().value().getColor() | 0xFF000000), Theme.TEXT, 0.15f);
            text(draw, ImGuiImpl.regular, x + 1, cy + 1, Theme.rgba(0x000000, 120), name + time);
            text(draw, ImGuiImpl.regular, x, cy, color, name);
            float nameWidth = width(ImGuiImpl.regular, name);
            text(draw, ImGuiImpl.regular, x + nameWidth, cy, Theme.TEXT, time);
            width = Math.max(width, nameWidth + width(ImGuiImpl.regular, time));
            cy += lineHeight;
        }
        return new float[]{width, Math.max(lineHeight, cy - y)};
    }
}
