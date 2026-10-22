package dev.sinepsinem.util.imgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Screen whose contents are drawn with Dear ImGui. Input received by the screen is
 * forwarded to ImGui, the previous screen (if any) is drawn underneath.
 */
public abstract class ImGuiScreen extends Screen {
    protected @Nullable Screen parent;

    protected ImGuiScreen(Component title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    protected abstract void renderImGui();

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (parent != null) {
            parent.extractBackground(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE, delta);
            parent.extractRenderState(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE, delta);
            graphics.nextStratum();
        }
        extractOverlay(graphics);
        ImGuiImpl.frame(graphics, this::renderImGui);
    }

    /**
     * Drawn above the parent screen but below ImGui.
     */
    protected void extractOverlay(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0x88000000);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        ImGuiImpl.mouseButton(event.button(), true);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        ImGuiImpl.mouseButton(event.button(), false);
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        ImGuiImpl.mouseScroll(scrollX, scrollY);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        ImGuiImpl.key(event.key(), event.modifiers(), true);
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && !ImGuiImpl.wantsTextInput()) {
            onClose();
        }
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        ImGuiImpl.key(event.key(), event.modifiers(), false);
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        ImGuiImpl.character(event.codepoint());
        return true;
    }

    @Override
    public void onClose() {
        // release anything ImGui still thinks is held
        for (int i = 0; i < 5; i++) ImGuiImpl.mouseButton(i, false);
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
