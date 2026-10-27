package dev.sinepsinem.features.modules.client;

import dev.sinepsinem.features.gui.hud.CustomChat;
import dev.sinepsinem.features.gui.hud.CustomHotbar;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.render.blur.KawaseBlur;
import org.jspecify.annotations.Nullable;

import java.awt.Color;

/**
 * The look of the client in one place: the custom hotbar and chat (both {@link dev.sinepsinem.features.gui.hud.HudElement}s),
 * SF Pro for all text, the Kawase blur and glow every glass panel uses, and the click gui particles.
 * Turning it off brings back the vanilla hotbar, chat and font.
 */
public class InterfaceModule extends Module {
    private static InterfaceModule INSTANCE;

    public final Setting<Boolean> hotbar = bool("Hotbar", true);
    public final Setting<Boolean> chat = bool("Chat", true);
    public final Setting<Boolean> sfPro = bool("SFProEverywhere", true);
    public final Setting<Float> smoothness = num("Smoothness", 14.0f, 2.0f, 40.0f);
    public final Setting<Float> rounding = num("Rounding", 5.0f, 0.0f, 11.0f);

    public final Setting<Boolean> blur = bool("Blur", true);
    public final Setting<Integer> blurPasses = num("BlurPasses", 4, 1, KawaseBlur.MAX_PASSES);
    public final Setting<Float> blurOffset = num("BlurOffset", 3.0f, 0.5f, 8.0f);
    public final Setting<Color> tint = color("Tint", 14, 14, 16, 120);

    public final Setting<Boolean> glow = bool("Glow", true);
    public final Setting<Color> glowColor = color("GlowColor", 230, 98, 163, 120);
    public final Setting<Float> glowRadius = num("GlowRadius", 9.0f, 1.0f, 24.0f);

    public final Setting<Boolean> particles = bool("GuiParticles", true);
    public final Setting<Integer> particleCount = num("ParticleCount", 70, 10, 250);

    private boolean appliedFont;

    public InterfaceModule() {
        super("Interface", "Blurred custom hotbar and chat, SF Pro font, glow", Category.CLIENT);
        blurPasses.setVisibility(v -> blur.getValue());
        blurOffset.setVisibility(v -> blur.getValue());
        glowColor.setVisibility(v -> glow.getValue());
        glowRadius.setVisibility(v -> glow.getValue());
        particleCount.setVisibility(v -> particles.getValue());
        INSTANCE = this;
    }

    /**
     * @return the module while it is enabled, null otherwise
     */
    public static @Nullable InterfaceModule get() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }

    public static boolean sfProEverywhere() {
        InterfaceModule module = get();
        return module != null && module.sfPro.getValue();
    }

    public float speed() {
        return smoothness.getValue();
    }

    @Override
    public void onEnable() {
        CustomHotbar.INSTANCE.reset();
        CustomChat.INSTANCE.reset();
    }

    @Override
    public void onTick() {
        // chat lines are wrapped with the metrics of the font that was active when they arrived
        boolean font = sfProEverywhere();
        if (font != appliedFont) {
            appliedFont = font;
            mc.gui.hud.getChat().rescaleChat();
        }
    }

    @Override
    public void onDisable() {
        if (appliedFont && mc.gui != null) {
            appliedFont = false;
            mc.gui.hud.getChat().rescaleChat();
        }
    }
}
