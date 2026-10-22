package me.alpha432.oyvey.event.impl.render;

import me.alpha432.oyvey.event.Event;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Render2DEvent extends Event {
    private final GuiGraphicsExtractor context;
    private final float delta;

    public Render2DEvent(GuiGraphicsExtractor context, float delta) {
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
