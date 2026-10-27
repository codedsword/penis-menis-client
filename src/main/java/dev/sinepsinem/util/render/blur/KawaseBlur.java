package dev.sinepsinem.util.render.blur;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.util.traits.Util;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dual Kawase blur of the frame behind the gui. Each pass halves the resolution on the way down
 * and doubles it on the way back up, which gets a wide, smooth blur out of very few texture taps.
 * <p>
 * Panels ask for the blurred texture while the gui is being extracted ({@link #request}); the blur
 * itself runs at the start of the gui render (MixinGuiRenderer), after the world and before any
 * gui element is drawn, so the result never contains the gui itself. Everything goes through
 * blaze3d render passes, so it works on both the OpenGL and the Vulkan backend.
 */
public final class KawaseBlur implements Util {
    public static final int MAX_PASSES = 6;

    private static final BindGroupLayout LAYOUT = BindGroupLayout.builder()
            .withSampler("InSampler")
            .withUniform("BlurInfo", UniformType.UNIFORM_BUFFER)
            .build();
    private static final RenderPipeline DOWN = pipeline("kawase_down");
    private static final RenderPipeline UP = pipeline("kawase_up");
    // vec2 HalfTexel + float Offset, padded to a vec4
    private static final int UBO_SIZE = 16;

    // targets.get(0) is half the framebuffer, every next one half of the previous
    private static final List<TextureTarget> targets = new ArrayList<>();
    private static final List<MappableRingBuffer> uniforms = new ArrayList<>();
    private static int width, height;

    private static boolean requested;
    private static int passes = 4;
    private static float offset = 3.0f;
    private static boolean failed;

    private KawaseBlur() {
    }

    private static RenderPipeline pipeline(String name) {
        return RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(SinepSinem.MOD_ID, "pipeline/" + name))
                .withVertexShader("core/screenquad")
                .withFragmentShader(Identifier.fromNamespaceAndPath(SinepSinem.MOD_ID, "post/" + name))
                .withBindGroupLayout(LAYOUT)
                .build();
    }

    /**
     * Schedules a blur of this frame and returns the texture it will be written to. Only call this
     * while the gui is extracted; the view stays valid until the gui of the next frame is extracted.
     *
     * @param passes  1 to {@link #MAX_PASSES}, every pass doubles the blur radius
     * @param offset  tap distance in texels, larger is blurrier but starts to show artifacts above ~5
     * @return null when the blur is unavailable (no window yet, shader failed)
     */
    public static @Nullable GpuTextureView request(int passes, float offset) {
        if (failed) return null;
        KawaseBlur.passes = Math.clamp(passes, 1, MAX_PASSES);
        KawaseBlur.offset = Math.max(0.0f, offset);
        try {
            ensureTargets(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        } catch (Exception e) {
            failed = true;
            SinepSinem.LOGGER.error("Kawase blur is unavailable", e);
            return null;
        }
        if (targets.isEmpty()) return null;
        requested = true;
        return targets.getFirst().getColorTextureView();
    }

    // created while extracting, so a texture handed out this frame is never closed before it is drawn
    private static void ensureTargets(int fbWidth, int fbHeight) {
        if (fbWidth == width && fbHeight == height && targets.size() == MAX_PASSES) return;
        close();
        if (fbWidth <= 1 || fbHeight <= 1) return;
        width = fbWidth;
        height = fbHeight;
        for (int i = 0; i < MAX_PASSES; i++) {
            int w = Math.max(1, fbWidth >> (i + 1));
            int h = Math.max(1, fbHeight >> (i + 1));
            targets.add(new TextureTarget("sinepsinem kawase " + i, w, h, false, GpuFormat.RGBA8_UNORM));
        }
        // one per pass, a pass may not overwrite the uniforms of a pass the gpu has not run yet
        for (int i = 0; i < MAX_PASSES * 2; i++) {
            int index = i;
            uniforms.add(new MappableRingBuffer(() -> "sinepsinem kawase uniforms " + index,
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, UBO_SIZE));
        }
    }

    /**
     * Runs the requested blur on the main render target. Called right before the gui is drawn.
     */
    public static void process() {
        if (!requested || failed || targets.isEmpty()) return;
        requested = false;

        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        GpuTextureView input = main.getColorTextureView();
        if (input == null) return;

        try {
            int ubo = 0;
            for (int i = 0; i < passes; i++) {
                TextureTarget out = targets.get(i);
                pass(DOWN, input, out, uniforms.get(ubo++));
                input = out.getColorTextureView();
            }
            for (int i = passes - 2; i >= 0; i--) {
                pass(UP, targets.get(i + 1).getColorTextureView(), targets.get(i), uniforms.get(ubo++));
            }
        } catch (Exception e) {
            // a broken shader must not take the whole gui down with it
            failed = true;
            SinepSinem.LOGGER.error("Kawase blur failed, disabling it", e);
        }
    }

    private static void pass(RenderPipeline pipeline, GpuTextureView input, RenderTarget output, MappableRingBuffer ubo) {
        GpuBuffer buffer = ubo.currentBuffer();
        try (GpuBufferSlice.MappedView view = buffer.map(false, true)) {
            Std140Builder.intoBuffer(view.data())
                    .putVec2(0.5f / output.width, 0.5f / output.height)
                    .putFloat(offset);
        }

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "sinepsinem kawase blur", output.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("BlurInfo", buffer);
            pass.bindTexture("InSampler", input, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.draw(3, 1, 0, 0);
        }
        ubo.rotate();
    }

    private static void close() {
        targets.forEach(RenderTarget::destroyBuffers);
        targets.clear();
        uniforms.forEach(MappableRingBuffer::close);
        uniforms.clear();
        width = height = 0;
    }
}
