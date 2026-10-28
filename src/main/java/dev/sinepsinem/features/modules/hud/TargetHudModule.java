package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.features.gui.HudEditorScreen;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.Theme;
import imgui.ImDrawList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Ambient's target hud: head (flashing red when hit), name, health in the accent, a health bar
 * in the theme gradient, distance, and whether you are winning the fight. Shows the kill aura
 * target or whoever you hit last; in the hud editor it previews you.
 */
public class TargetHudModule extends ImGuiHudModule {
    private static final float WIDTH = 230, HEIGHT = 64, HEAD = 46, PAD = 9;

    private @Nullable LivingEntity shown;
    private float health = 1, alpha;
    private long lastFrame = System.nanoTime();

    public TargetHudModule() {
        super("TargetHud", "Shows who you are fighting", 115, 32);
        pos.setValue(new Vector2f(0.46f, 0.72f));
    }

    private @Nullable LivingEntity target() {
        LivingEntity target = SinepSinem.sessionManager.target();
        if (target == null && mc.gui.screen() instanceof HudEditorScreen) return mc.player;
        return target;
    }

    @Override
    protected float[] draw(ImDrawList draw, float x, float y) {
        long now = System.nanoTime();
        float dt = Math.min((now - lastFrame) / 1.0E9f, 0.1f);
        lastFrame = now;

        LivingEntity target = target();
        if (target != null) shown = target;
        alpha += ((target != null ? 1 : 0) - alpha) * Math.min(1, dt * 10);
        if (shown == null || alpha < 0.01f) return new float[]{WIDTH, HEIGHT};

        LivingEntity entity = shown;
        float hp = entity.getHealth() + entity.getAbsorptionAmount();
        health += (Math.clamp(hp / entity.getMaxHealth(), 0, 1) - health) * Math.min(1, dt * 8);

        draw.addRectFilled(x, y, x + WIDTH, y + HEIGHT, Theme.rgba(0x101010, Math.round(200 * alpha)), ROUNDING);
        float tx = x + PAD + HEAD + 10, right = x + WIDTH - PAD;
        int white = Theme.withAlpha(Theme.TEXT, alpha), dim = Theme.withAlpha(Theme.TEXT_DIM, alpha);

        text(draw, ImGuiImpl.regular, tx, y + 9, white, entity.getName().getString());
        String hpText = hp == Math.floor(hp) ? String.valueOf((int) hp) : String.format(Locale.ROOT, "%.1f", hp);
        text(draw, ImGuiImpl.regular, right - width(ImGuiImpl.regular, hpText), y + 9, Theme.withAlpha(Theme.accent(), alpha), hpText);
        bar(draw, tx, y + 32, right - tx, 5, health);

        String distance = entity == mc.player ? "0m" : Math.round(entity.distanceTo(mc.player)) + "m";
        float self = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        String state = entity == mc.player ? "" : self > hp ? "Winning" : self < hp ? "Losing" : "Even";
        text(draw, ImGuiImpl.small, tx, y + 43, dim, distance);
        text(draw, ImGuiImpl.small, right - width(ImGuiImpl.small, state), y + 43, dim, state);
        return new float[]{WIDTH, HEIGHT};
    }

    @Override
    protected void drawVanilla(GuiGraphicsExtractor graphics, float x, float y) {
        if (shown == null || alpha < 0.5f) return;
        int size = Math.round(HEAD / scale);
        int hx = Math.round(x + PAD / scale), hy = Math.round(y + (HEIGHT - HEAD) / 2 / scale);
        // red flash while the target is hurt, like the vanilla damage tint
        int tint = shown.hurtTime > 0 ? ARGB.color(255, 255, 120, 120) : -1;
        if (shown instanceof AbstractClientPlayer player) {
            PlayerFaceExtractor.extractRenderState(graphics, player.getSkin(), hx, hy, size, tint);
        } else {
            graphics.fill(hx, hy, hx + size, hy + size, 0xFF2A2A2A);
            String letter = shown.getName().getString().substring(0, 1);
            graphics.centeredText(mc.font, letter, hx + size / 2, hy + (size - 8) / 2, -1);
        }
    }
}
