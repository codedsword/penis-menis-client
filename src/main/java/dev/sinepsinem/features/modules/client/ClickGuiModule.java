package dev.sinepsinem.features.modules.client;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.event.impl.ClientEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.commands.Command;
import dev.sinepsinem.features.gui.ClickGuiScreen;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static dev.sinepsinem.features.commands.MessageSignatures.GENERAL;

public class ClickGuiModule extends Module {
    private static ClickGuiModule INSTANCE;

    public final Setting<String> prefix = str("Prefix", ".");
    public final Setting<Color> color = color("Color", 0, 0, 255, 180);
    public final Setting<Color> topColor = color("TopColor", 0, 0, 150, 240);
    public final Setting<Boolean> rainbow = bool("Rainbow", false);
    public final Setting<Integer> rainbowHue = num("Delay", 240, 0, 600);
    public final Setting<Float> rainbowBrightness = num("Brightness", 150.0f, 1.0f, 255.0f);
    public final Setting<Float> rainbowSaturation = num("Saturation", 150.0f, 1.0f, 255.0f);

    public ClickGuiModule() {
        super("ClickGui", "Opens the ClickGui", Module.Category.CLIENT);
        setBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
        rainbowHue.setVisibility(v -> rainbow.getValue());
        rainbowBrightness.setVisibility(v -> rainbow.getValue());
        rainbowSaturation.setVisibility(v -> rainbow.getValue());
        INSTANCE = this;
    }

    @Subscribe
    public void onSettingChange(ClientEvent event) {
        if (event.getType() == ClientEvent.Type.SETTING_UPDATE && event.getSetting().getFeature().equals(this)) {
            if (event.getSetting().equals(this.prefix)) {
                SinepSinem.commandManager.setCommandPrefix(this.prefix.getPlannedValue());
                Command.sendMessage("Prefix set to {global} %s", GENERAL, SinepSinem.commandManager.getCommandPrefix());
            }
            if (event.getSetting().equals(this.color)) {
                SinepSinem.colorManager.setColor(this.color.getPlannedValue());
            }
        }
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
        SinepSinem.colorManager.setColor(this.color.getValue());
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