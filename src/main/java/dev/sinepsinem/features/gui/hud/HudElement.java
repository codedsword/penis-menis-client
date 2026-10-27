package dev.sinepsinem.features.gui.hud;

import dev.sinepsinem.features.modules.client.InterfaceModule;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.traits.Util;
import imgui.ImDrawList;
import imgui.ImGui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Consumer;

/**
 * A vanilla hud element we replace (the hotbar, the chat). The mixins only ask an element whether
 * it {@link #isActive() is active} and hand it the vanilla state; the element draws its glass
 * chrome through its own ImGui layer and leaves items and text to vanilla, so item models,
 * tooltips and clickable chat links keep working.
 */
public interface HudElement extends Util {
    /**
     * @return whether the element replaces its vanilla counterpart right now
     */
    boolean isActive();

    /**
     * @return the ImGui context this element draws into, one per element
     */
    ImGuiImpl.Layer layer();

    /**
     * Forget animation state, e.g. after the element was turned off for a while.
     */
    void reset();

    /**
     * Draws this element's chrome. The consumer gets the draw list and the gui scale; coordinates
     * are ImGui pixels, so gui coordinates must be multiplied by the scale.
     */
    default void chrome(GuiGraphicsExtractor graphics, Consumer<Chrome> ui) {
        float scale = mc.getWindow().getGuiScale();
        ImGuiImpl.frame(layer(), graphics, () -> ui.accept(new Chrome(ImGui.getBackgroundDrawList(), scale)));
    }

    /**
     * @return how far an animated value moves towards its target this frame, from 0 to 1
     */
    static float smoothing(float dt) {
        InterfaceModule style = InterfaceModule.get();
        float speed = style == null ? 14.0f : style.speed();
        return 1.0f - (float) Math.exp(-dt * speed);
    }

    static float rounding() {
        InterfaceModule style = InterfaceModule.get();
        return style == null ? 5.0f : style.rounding.getValue();
    }

    record Chrome(ImDrawList draw, float scale) {
    }
}
