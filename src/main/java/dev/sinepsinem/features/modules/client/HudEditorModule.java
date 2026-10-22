package dev.sinepsinem.features.modules.client;

import dev.sinepsinem.features.gui.HudEditorScreen;
import dev.sinepsinem.features.modules.Module;

public class HudEditorModule extends Module {
    public HudEditorModule() {
        super("HudEditor", "Edit HUD element positions", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            disable();
            return;
        }
        mc.gui.setScreen(HudEditorScreen.getInstance());
        disable();
    }
}

