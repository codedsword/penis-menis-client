package dev.sinepsinem.event.impl.render;

import dev.sinepsinem.event.Event;

/**
 * Posted while the level is being extracted for rendering. 3D drawing is done by emitting
 * gizmos (see {@link dev.sinepsinem.util.render.RenderUtil}), which vanilla renders on
 * both the OpenGL and Vulkan backends.
 */
public class Render3DEvent extends Event {
    private final float delta;

    public Render3DEvent(float delta) {
        this.delta = delta;
    }

    public float getDelta() {
        return delta;
    }
}
