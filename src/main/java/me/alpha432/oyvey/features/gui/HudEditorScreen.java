package me.alpha432.oyvey.features.gui;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.HudModule;
import me.alpha432.oyvey.util.imgui.ImGuiImpl;
import me.alpha432.oyvey.util.imgui.ImGuiScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Hud elements drag themselves (see {@link HudModule}), this screen only lists them.
 */
public class HudEditorScreen extends ImGuiScreen {
    private static HudEditorScreen INSTANCE;

    public HudModule currentDragging;
    public boolean anyHover;

    private HudEditorScreen() {
        super(Component.literal("oyvey-hudeditor"), null);
    }

    public static HudEditorScreen getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new HudEditorScreen();
        }
        return INSTANCE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        anyHover = false;
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected void extractOverlay(GuiGraphicsExtractor graphics) {
    }

    @Override
    protected void renderImGui() {
        ImGui.setNextWindowPos(20, 20, ImGuiCond.FirstUseEver);
        ImGui.pushFont(ImGuiImpl.regular);
        if (ImGui.begin("Hud", ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoCollapse)) {
            OyVey.moduleManager.stream()
                    .filter(m -> m.getCategory() == Module.Category.HUD && !m.hidden)
                    .forEach(m -> {
                        if (ImGui.checkbox(m.getName(), m.isEnabled())) m.toggle();
                    });
        }
        ImGui.end();
        ImGui.popFont();
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        super.mouseClicked(event, doubleClick);
        return false;
    }
}
