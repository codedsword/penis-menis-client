package dev.sinepsinem.features.modules.client;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.event.impl.ClientEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.commands.Command;
import dev.sinepsinem.features.gui.ClickGuiScreen;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

import static dev.sinepsinem.features.commands.MessageSignatures.GENERAL;

public class ClickGuiModule extends Module {
    private static ClickGuiModule INSTANCE;

    public enum Preset {
        // Ambient's own themes, from its screenshots
        AMBIENT(0xFF0086, 0xFF001C),
        SUNSET(0xFF4B1F, 0xFFD21F),
        OCEAN(0x2F80FF, 0x8C52FF),
        MINT(0x1FFFA5, 0x1FB8FF),
        CUSTOM(0, 0);

        final int accent, secondary;

        Preset(int accent, int secondary) {
            this.accent = accent;
            this.secondary = secondary;
        }
    }

    public final Setting<String> prefix = str("Prefix", ".");
    public final Setting<Preset> preset = mode("Theme", Preset.AMBIENT);
    public final Setting<Color> accent = color("Accent", 255, 0, 134, 255);
    public final Setting<Color> secondary = color("Secondary", 255, 0, 28, 255);
    public final Setting<Boolean> darken = bool("DarkenBackground", true);
    public final Setting<Boolean> descriptions = bool("Descriptions", true);

    // where every category panel sits, in framebuffer pixels; negative until the panel was first laid out
    private final Map<Category, Setting<Vector2f>> panels = new EnumMap<>(Category.class);

    public ClickGuiModule() {
        super("ClickGui", "Opens the ClickGui", Module.Category.CLIENT);
        setBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
        accent.setVisibility(v -> preset.getValue() == Preset.CUSTOM);
        secondary.setVisibility(v -> preset.getValue() == Preset.CUSTOM);
        for (Category category : Category.values()) {
            panels.put(category, vec2f("Panel" + category.getName(), -1, -1));
        }
        INSTANCE = this;
    }

    public Setting<Vector2f> panel(Category category) {
        return panels.get(category);
    }

    @Subscribe
    public void onSettingChange(ClientEvent event) {
        if (event.getType() == ClientEvent.Type.SETTING_UPDATE && event.getSetting().getFeature().equals(this)) {
            if (event.getSetting().equals(this.prefix)) {
                SinepSinem.commandManager.setCommandPrefix(this.prefix.getPlannedValue());
                Command.sendMessage("Prefix set to {global} %s", GENERAL, SinepSinem.commandManager.getCommandPrefix());
            }
        }
    }

    /**
     * @return the theme's accent as 0xRRGGBB, from the preset unless it is custom
     */
    public int accentRgb() {
        return preset.getValue() == Preset.CUSTOM ? accent.getValue().getRGB() & 0xFFFFFF : preset.getValue().accent;
    }

    public int secondaryRgb() {
        return preset.getValue() == Preset.CUSTOM ? secondary.getValue().getRGB() & 0xFFFFFF : preset.getValue().secondary;
    }

    @Override
    public void onEnable() {
        // can be enabled by the config before the game is up
        if (mc.gui == null) {
            disable();
            return;
        }
        if (!(mc.gui.screen() instanceof ClickGuiScreen)) {
            ClickGuiScreen.open(mc.gui.screen());
        }
    }

    @Override
    public void onLoad() {
        SinepSinem.commandManager.setCommandPrefix(this.prefix.getValue());
    }

    @Override
    public void onTick() {
        if (!(ClickGuiModule.mc.gui.screen() instanceof ClickGuiScreen)) {
            this.disable();
        }
    }

    public static ClickGuiModule getInstance() {
        return INSTANCE;
    }
}
