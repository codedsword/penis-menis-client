package me.alpha432.oyvey.util.imgui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.jspecify.annotations.Nullable;

/**
 * One ImGui draw command, submitted as a regular GUI element so it goes through vanilla's
 * GUI renderer (and therefore works on every graphics backend).
 * <p>
 * The GUI pipelines draw quads, so every ImGui triangle is emitted as a degenerate quad
 * (a, b, c, c); the second triangle of that quad has zero area and is never rasterized.
 */
public record ImGuiDrawElement(RenderPipeline pipeline, TextureSetup textureSetup,
                               float[] vertices, int[] colors, float scale,
                               ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
    // vertices are packed as x, y, u, v per index
    public static final int STRIDE = 4;

    @Override
    public void buildVertices(VertexConsumer consumer) {
        int count = colors.length;
        for (int i = 0; i + 2 < count; i += 3) {
            emit(consumer, i);
            emit(consumer, i + 1);
            emit(consumer, i + 2);
            emit(consumer, i + 2);
        }
    }

    private void emit(VertexConsumer consumer, int index) {
        int o = index * STRIDE;
        consumer.addVertex(vertices[o] / scale, vertices[o + 1] / scale, 0.0f)
                .setUv(vertices[o + 2], vertices[o + 3])
                .setColor(colors[index]);
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return scissorArea;
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return bounds;
    }
}
