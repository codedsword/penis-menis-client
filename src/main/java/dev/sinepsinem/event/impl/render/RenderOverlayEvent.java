package dev.sinepsinem.event.impl.render;

import dev.sinepsinem.event.Event;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Posted before the vanilla hud is extracted, for world space overlays (particles, trajectory
 * labels) that belong under the hotbar and chat rather than on top of them.
 */
public class RenderOverlayEvent extends Event {
    private final GuiGraphicsExtractor context;
    private final float delta;

    public RenderOverlayEvent(GuiGraphicsExtractor context, float delta) {
        this.context = context;
        this.delta = delta;
    }

    public GuiGraphicsExtractor getContext() {
        return context;
    }

    public float getDelta() {
        return delta;
    }
}
