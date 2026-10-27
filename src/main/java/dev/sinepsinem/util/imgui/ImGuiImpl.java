package dev.sinepsinem.util.imgui;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.features.modules.client.InterfaceModule;
import dev.sinepsinem.util.render.blur.KawaseBlur;
import dev.sinepsinem.util.text.Fonts;
import dev.sinepsinem.util.traits.Util;
import imgui.ImDrawData;
import imgui.ImFont;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec4;
import imgui.flag.ImGuiBackendFlags;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiFreeTypeBuilderFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseCursor;
import imgui.internal.ImGuiContext;
import imgui.type.ImInt;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Dear ImGui integration that does not touch OpenGL directly: textures are regular
 * {@link DynamicTexture}s and draw data is submitted as GUI elements. ImGui works in
 * framebuffer pixels, its output is scaled down by the GUI scale when it is submitted.
 * <p>
 * Every {@link Layer} is its own ImGui context (with its own clock), all of them share one font
 * atlas. Screens draw into {@link #SCREEN}; hud elements get a layer each, so drawing the hotbar
 * and the chat in the same frame does not run one context twice per frame.
 */
public final class ImGuiImpl implements Util {
    // texture ids understood by the draw lists of every layer
    public static final long TEX_FONT = 1;
    /** the Kawase blurred frame, sampled with screen space uvs (see {@link Draw#blur}) */
    public static final long TEX_BLUR = 2;
    /** soft radial glow, blended additively */
    public static final long TEX_GLOW = 3;
    /** the same radial falloff blended normally, for drop shadows */
    public static final long TEX_SHADOW = 4;

    private static final Identifier FONT_TEXTURE = Identifier.fromNamespaceAndPath(SinepSinem.MOD_ID, "imgui_font_atlas");
    private static final Identifier GLOW_TEXTURE = Identifier.fromNamespaceAndPath(SinepSinem.MOD_ID, "imgui_glow");
    private static final RenderPipeline GLOW_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(SinepSinem.MOD_ID, "pipeline/imgui_glow"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .build();

    public static final float FONT_SIZE = 17.0f;

    public static final Layer SCREEN = new Layer("screen", true);

    private static boolean initialized;
    private static Throwable failure;
    private static ImFontAtlas atlas;
    private static DynamicTexture fontTexture;
    private static DynamicTexture glowTexture;

    /** SF Pro Medium at the body size; everything is Medium, only the sizes differ */
    public static ImFont regular;
    public static ImFont small;
    public static ImFont title;

    private ImGuiImpl() {
    }

    /**
     * One ImGui context. Only {@link #SCREEN} receives mouse and keyboard input.
     */
    public static final class Layer {
        private final String name;
        private final boolean input;
        private @Nullable ImGuiContext context;
        private long lastFrame = System.nanoTime();

        public Layer(String name, boolean input) {
            this.name = name;
            this.input = input;
        }

        private void makeCurrent() {
            if (context == null) {
                context = this == SCREEN ? ImGui.createContext() : ImGui.createContext(atlas);
                ImGui.setCurrentContext(context);
                configure(input);
                SinepSinem.LOGGER.debug("Created ImGui layer {}", name);
            } else {
                ImGui.setCurrentContext(context);
            }
        }
    }

    /**
     * @return whether ImGui can be used, false when its native library failed to load
     */
    public static boolean ensureInit() {
        if (initialized || failure != null) return initialized;

        try {
            ImGuiNatives.prepare();
            SCREEN.makeCurrent();
        } catch (Throwable t) {
            failure = t;
            SinepSinem.LOGGER.error("Dear ImGui is not available on {} {}", System.getProperty("os.name"), System.getProperty("os.arch"), t);
            return false;
        }
        initialized = true;

        atlas = ImGui.getIO().getFonts();
        loadFonts(atlas);
        createGlowTexture();
        return true;
    }

    private static void configure(boolean input) {
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        io.setLogFilename(null);
        if (input) {
            io.setConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
            io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors);
            io.setSetClipboardTextFn(new imgui.callback.ImStrConsumer() {
                @Override
                public void accept(String text) {
                    mc.keyboardHandler.setClipboard(text);
                }
            });
            io.setGetClipboardTextFn(new imgui.callback.ImStrSupplier() {
                @Override
                public String get() {
                    return mc.keyboardHandler.getClipboard();
                }
            });
        } else {
            io.setConfigFlags(ImGuiConfigFlags.NoMouse | ImGuiConfigFlags.NoMouseCursorChange);
        }
        Theme.apply();
    }

    private static void loadFonts(ImFontAtlas atlas) {
        byte[] medium = readFont();

        ImFontConfig config = new ImFontConfig();
        config.setPixelSnapH(false);
        // ImGui would otherwise free our byte[] copy
        config.setFontDataOwnedByAtlas(false);
        if (useFreeType(atlas)) {
            // light hinting keeps glyph shapes intact, Bitmap allows embedded bitmap strikes
            config.setFontBuilderFlags(ImGuiFreeTypeBuilderFlags.LightHinting | ImGuiFreeTypeBuilderFlags.Bitmap);
            config.setRasterizerMultiply(1.1f);
        } else {
            config.setOversampleH(3);
            config.setOversampleV(2);
        }

        short[] ranges = atlas.getGlyphRangesCyrillic();
        if (medium != null) {
            regular = atlas.addFontFromMemoryTTF(medium, FONT_SIZE, config, ranges);
            small = atlas.addFontFromMemoryTTF(medium, FONT_SIZE - 3.0f, config, ranges);
            title = atlas.addFontFromMemoryTTF(medium, FONT_SIZE + 7.0f, config, ranges);
        } else {
            regular = small = title = atlas.addFontDefault();
        }
        config.destroy();

        ImInt width = new ImInt();
        ImInt height = new ImInt();
        ByteBuffer pixels = atlas.getTexDataAsRGBA32(width, height).order(ByteOrder.LITTLE_ENDIAN);

        NativeImage image = new NativeImage(width.get(), height.get(), false);
        for (int y = 0; y < height.get(); y++) {
            for (int x = 0; x < width.get(); x++) {
                // RGBA bytes read little endian are exactly NativeImage's ABGR layout
                image.setPixelABGR(x, y, pixels.getInt((y * width.get() + x) * 4));
            }
        }

        fontTexture = new DynamicTexture(() -> "sinepsinem imgui font atlas", image);
        mc.getTextureManager().register(FONT_TEXTURE, fontTexture);
        atlas.setTexID(TEX_FONT);
        atlas.clearTexData();
    }

    // white, alpha falls off from the center to the edge of the circle
    private static void createGlowTexture() {
        int size = 64;
        NativeImage image = new NativeImage(size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f) / size * 2 - 1, dy = (y + 0.5f) / size * 2 - 1;
                float falloff = Math.max(0, 1 - (float) Math.sqrt(dx * dx + dy * dy));
                // smoothstep squared, no visible ring where the falloff reaches zero
                float alpha = falloff * falloff * (3 - 2 * falloff);
                image.setPixelABGR(x, y, Math.round(alpha * alpha * 255) << 24 | 0xFFFFFF);
            }
        }
        glowTexture = new DynamicTexture(() -> "sinepsinem imgui glow", image);
        mc.getTextureManager().register(GLOW_TEXTURE, glowTexture);
    }

    /**
     * Rasterizes the atlas with FreeType instead of stb_truetype, which gives noticeably smoother
     * small text. Falls back to stb when the natives were built without FreeType.
     */
    private static boolean useFreeType(ImFontAtlas atlas) {
        try {
            atlas.setFreeTypeRenderer(true);
            SinepSinem.LOGGER.info("ImGui fonts are rasterized with FreeType");
            return true;
        } catch (Throwable t) {
            SinepSinem.LOGGER.warn("FreeType is not available for ImGui, using stb_truetype", t);
            return false;
        }
    }

    // SF Pro Medium from .minecraft/sinepsinem/fonts, otherwise the bundled Inter Medium
    private static byte @Nullable [] readFont() {
        try {
            Path override = Fonts.sfProOverride();
            if (override != null) {
                SinepSinem.LOGGER.info("Using {} for ImGui", override.getFileName());
                return Files.readAllBytes(override);
            }
            try (InputStream in = ImGuiImpl.class.getResourceAsStream("/assets/sinepsinem/font/" + Fonts.BUNDLED_FILE.getPath())) {
                return in == null ? null : in.readAllBytes();
            }
        } catch (Exception e) {
            SinepSinem.LOGGER.error("Failed to load the ImGui font", e);
            return null;
        }
    }

    /**
     * Runs one frame of the screen layer and submits the result into {@code graphics}.
     */
    public static void frame(GuiGraphicsExtractor graphics, Runnable ui) {
        if (!ensureInit()) {
            // never crash the game over the gui, say what is wrong instead
            String reason = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            graphics.centeredText(mc.font, "The gui could not start on this device (" + System.getProperty("os.arch") + ")",
                    graphics.guiWidth() / 2, graphics.guiHeight() / 2 - 10, 0xFFE662A3);
            graphics.centeredText(mc.font, reason.length() > 90 ? reason.substring(0, 90) + "..." : reason,
                    graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 4, 0xFFA3A3A3);
            return;
        }
        frame(SCREEN, graphics, ui);
    }

    /**
     * Runs one frame of {@code layer}. Hud layers have no windows of their own, they draw into
     * {@link ImGui#getBackgroundDrawList()}. Does nothing when ImGui failed to start.
     */
    public static void frame(Layer layer, GuiGraphicsExtractor graphics, Runnable ui) {
        if (!ensureInit()) return;
        layer.makeCurrent();
        ImGuiIO io = ImGui.getIO();

        int fbWidth = mc.getWindow().getWidth();
        int fbHeight = mc.getWindow().getHeight();
        float scale = mc.getWindow().getGuiScale();
        io.setDisplaySize(fbWidth, fbHeight);
        io.setDisplayFramebufferScale(1.0f, 1.0f);

        long now = System.nanoTime();
        io.setDeltaTime(Math.clamp((now - layer.lastFrame) / 1.0E9f, 1.0E-4f, 0.25f));
        layer.lastFrame = now;

        if (layer.input) {
            io.addMousePosEvent(
                    (float) (mc.mouseHandler.getScaledXPos(mc.getWindow()) * scale),
                    (float) (mc.mouseHandler.getScaledYPos(mc.getWindow()) * scale));
        } else {
            io.addMousePosEvent(-Float.MAX_VALUE, -Float.MAX_VALUE);
        }

        ImGui.newFrame();
        ui.run();
        ImGui.render();

        submit(graphics, ImGui.getDrawData(), scale);
        if (layer.input) updateCursor(graphics);
        SCREEN.makeCurrent();
    }

    private static void submit(GuiGraphicsExtractor graphics, ImDrawData data, float scale) {
        if (data == null || data.getCmdListsCount() == 0) return;

        TextureSetup font = TextureSetup.singleTexture(fontTexture.getTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        TextureSetup glow = TextureSetup.singleTexture(glowTexture.getTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        TextureSetup blur = null;
        boolean blurResolved = false;

        // scissors must stay inside the render target, clip rects reaching past the screen edge do not
        ScreenRectangle screen = new ScreenRectangle(0, 0, graphics.guiWidth(), graphics.guiHeight());
        ImVec4 clip = new ImVec4();
        int vertexSize = ImDrawData.sizeOfImDrawVert();
        int indexSize = ImDrawData.sizeOfImDrawIdx();
        float displayX = data.getDisplayPosX();
        float displayY = data.getDisplayPosY();

        for (int list = 0; list < data.getCmdListsCount(); list++) {
            // imgui-java hands out both through the same shared buffer, so take copies
            ByteBuffer vtx = copy(data.getCmdListVtxBufferData(list));
            ByteBuffer idx = copy(data.getCmdListIdxBufferData(list));

            for (int cmd = 0; cmd < data.getCmdListCmdBufferSize(list); cmd++) {
                int elemCount = data.getCmdListCmdBufferElemCount(list, cmd);
                if (elemCount == 0) continue;

                long textureId = data.getCmdListCmdBufferTextureId(list, cmd);
                RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
                TextureSetup texture = font;
                if (textureId == TEX_BLUR) {
                    if (!blurResolved) {
                        // only blur the frame when something on screen actually shows it
                        blur = blurTexture();
                        blurResolved = true;
                    }
                    if (blur == null) continue;
                    texture = blur;
                } else if (textureId == TEX_GLOW) {
                    pipeline = GLOW_PIPELINE;
                    texture = glow;
                } else if (textureId == TEX_SHADOW) {
                    texture = glow;
                }

                data.getCmdListCmdBufferClipRect(clip, list, cmd);
                float cx1 = clip.x - displayX, cy1 = clip.y - displayY;
                float cx2 = clip.z - displayX, cy2 = clip.w - displayY;
                if (cx2 <= cx1 || cy2 <= cy1) continue;

                int idxOffset = data.getCmdListCmdBufferIdxOffset(list, cmd);
                int vtxOffset = data.getCmdListCmdBufferVtxOffset(list, cmd);

                float[] vertices = new float[elemCount * ImGuiDrawElement.STRIDE];
                int[] colors = new int[elemCount];
                for (int i = 0; i < elemCount; i++) {
                    int index = (indexSize == 2
                            ? Short.toUnsignedInt(idx.getShort((idxOffset + i) * 2))
                            : idx.getInt((idxOffset + i) * 4)) + vtxOffset;
                    int base = index * vertexSize;
                    int o = i * ImGuiDrawElement.STRIDE;
                    vertices[o] = vtx.getFloat(base) - displayX;
                    vertices[o + 1] = vtx.getFloat(base + 4) - displayY;
                    vertices[o + 2] = vtx.getFloat(base + 8);
                    vertices[o + 3] = vtx.getFloat(base + 12);
                    // ImGui packs colors as ABGR
                    colors[i] = ARGB.fromABGR(vtx.getInt(base + 16));
                }

                int x = (int) Math.floor(cx1 / scale);
                int y = (int) Math.floor(cy1 / scale);
                ScreenRectangle rect = new ScreenRectangle(x, y,
                        (int) Math.ceil(cx2 / scale) - x, (int) Math.ceil(cy2 / scale) - y);
                ScreenRectangle scissor = graphics.scissorStack.peek();
                ScreenRectangle area = screen.intersection(rect);
                if (area != null && scissor != null) area = scissor.intersection(area);
                if (area == null || area.width() <= 0 || area.height() <= 0) continue;

                graphics.guiRenderState.addGuiElement(new ImGuiDrawElement(
                        pipeline, texture, vertices, colors, scale, area, area));
            }
        }
    }

    private static @Nullable TextureSetup blurTexture() {
        InterfaceModule style = InterfaceModule.get();
        if (style != null && !style.blur.getValue()) return null;
        int passes = style == null ? 4 : style.blurPasses.getValue();
        float offset = style == null ? 3.0f : style.blurOffset.getValue();
        GpuTextureView view = KawaseBlur.request(passes, offset);
        return view == null ? null : TextureSetup.singleTexture(view,
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
    }

    private static ByteBuffer copy(ByteBuffer source) {
        ByteBuffer copy = ByteBuffer.allocate(source.remaining()).order(ByteOrder.nativeOrder());
        copy.put(source.duplicate());
        return copy.flip();
    }

    private static void updateCursor(GuiGraphicsExtractor graphics) {
        switch (ImGui.getMouseCursor()) {
            case ImGuiMouseCursor.TextInput -> graphics.requestCursor(CursorTypes.IBEAM);
            case ImGuiMouseCursor.Hand -> graphics.requestCursor(CursorTypes.POINTING_HAND);
            case ImGuiMouseCursor.ResizeEW -> graphics.requestCursor(CursorTypes.RESIZE_EW);
            case ImGuiMouseCursor.ResizeNS -> graphics.requestCursor(CursorTypes.RESIZE_NS);
            case ImGuiMouseCursor.ResizeAll -> graphics.requestCursor(CursorTypes.RESIZE_ALL);
            default -> {
            }
        }
    }

    // input, fed from ImGuiScreen into the screen layer

    public static void mouseButton(int button, boolean down) {
        if (!initialized || button < 0 || button > 4) return;
        SCREEN.makeCurrent();
        ImGui.getIO().addMouseButtonEvent(button, down);
    }

    public static void mouseScroll(double horizontal, double vertical) {
        if (!initialized) return;
        SCREEN.makeCurrent();
        ImGui.getIO().addMouseWheelEvent((float) horizontal, (float) vertical);
    }

    public static void key(int glfwKey, int modifiers, boolean down) {
        if (!initialized) return;
        SCREEN.makeCurrent();
        ImGuiIO io = ImGui.getIO();
        io.addKeyEvent(ImGuiKey.ModCtrl, (modifiers & GLFW_MOD_CONTROL) != 0);
        io.addKeyEvent(ImGuiKey.ModShift, (modifiers & GLFW_MOD_SHIFT) != 0);
        io.addKeyEvent(ImGuiKey.ModAlt, (modifiers & GLFW_MOD_ALT) != 0);
        io.addKeyEvent(ImGuiKey.ModSuper, (modifiers & GLFW_MOD_SUPER) != 0);
        int key = mapKey(glfwKey);
        if (key != ImGuiKey.None) io.addKeyEvent(key, down);
    }

    public static void character(int codepoint) {
        if (!initialized) return;
        SCREEN.makeCurrent();
        ImGui.getIO().addInputCharacter(codepoint);
    }

    public static boolean wantsTextInput() {
        if (!initialized) return false;
        SCREEN.makeCurrent();
        return ImGui.getIO().getWantTextInput();
    }

    private static int mapKey(int key) {
        return switch (key) {
            case GLFW_KEY_TAB -> ImGuiKey.Tab;
            case GLFW_KEY_LEFT -> ImGuiKey.LeftArrow;
            case GLFW_KEY_RIGHT -> ImGuiKey.RightArrow;
            case GLFW_KEY_UP -> ImGuiKey.UpArrow;
            case GLFW_KEY_DOWN -> ImGuiKey.DownArrow;
            case GLFW_KEY_PAGE_UP -> ImGuiKey.PageUp;
            case GLFW_KEY_PAGE_DOWN -> ImGuiKey.PageDown;
            case GLFW_KEY_HOME -> ImGuiKey.Home;
            case GLFW_KEY_END -> ImGuiKey.End;
            case GLFW_KEY_INSERT -> ImGuiKey.Insert;
            case GLFW_KEY_DELETE -> ImGuiKey.Delete;
            case GLFW_KEY_BACKSPACE -> ImGuiKey.Backspace;
            case GLFW_KEY_SPACE -> ImGuiKey.Space;
            case GLFW_KEY_ENTER -> ImGuiKey.Enter;
            case GLFW_KEY_KP_ENTER -> ImGuiKey.KeypadEnter;
            case GLFW_KEY_ESCAPE -> ImGuiKey.Escape;
            case GLFW_KEY_LEFT_CONTROL -> ImGuiKey.LeftCtrl;
            case GLFW_KEY_LEFT_SHIFT -> ImGuiKey.LeftShift;
            case GLFW_KEY_A -> ImGuiKey.A;
            case GLFW_KEY_C -> ImGuiKey.C;
            case GLFW_KEY_V -> ImGuiKey.V;
            case GLFW_KEY_X -> ImGuiKey.X;
            case GLFW_KEY_Y -> ImGuiKey.Y;
            case GLFW_KEY_Z -> ImGuiKey.Z;
            default -> ImGuiKey.None;
        };
    }
}
