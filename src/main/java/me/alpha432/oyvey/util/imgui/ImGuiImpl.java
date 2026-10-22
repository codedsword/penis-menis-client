package me.alpha432.oyvey.util.imgui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import imgui.ImDrawData;
import imgui.ImFont;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec4;
import imgui.flag.ImGuiBackendFlags;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseCursor;
import imgui.type.ImInt;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.util.traits.Util;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Dear ImGui integration that does not touch OpenGL directly: the font atlas is a regular
 * {@link DynamicTexture} and draw data is submitted as GUI elements. ImGui works in
 * framebuffer pixels, its output is scaled down by the GUI scale when it is submitted.
 */
public final class ImGuiImpl implements Util {
    private static final Identifier FONT_TEXTURE = Identifier.fromNamespaceAndPath("oyvey", "imgui_font_atlas");
    private static final Path FONT_OVERRIDE_DIR = FabricLoader.getInstance().getGameDir().resolve("oyvey").resolve("fonts");

    public static final float FONT_SIZE = 17.0f;

    private static boolean initialized;
    private static long lastFrame;
    private static DynamicTexture fontTexture;

    public static ImFont regular;
    public static ImFont semiBold;
    public static ImFont small;
    public static ImFont title;

    private ImGuiImpl() {
    }

    public static void ensureInit() {
        if (initialized) return;
        initialized = true;

        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
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

        loadFonts(io.getFonts());
        Theme.apply();
        lastFrame = System.nanoTime();
    }

    private static void loadFonts(ImFontAtlas atlas) {
        byte[] regularBytes = readFont("ProductSans-Regular.ttf", "productsans-regular.ttf");
        byte[] boldBytes = readFont("ProductSans-Bold.ttf", "productsans-semibold.ttf");

        ImFontConfig config = new ImFontConfig();
        config.setOversampleH(3);
        config.setOversampleV(2);
        config.setPixelSnapH(false);
        // ImGui would otherwise free our byte[] copies
        config.setFontDataOwnedByAtlas(false);

        short[] ranges = atlas.getGlyphRangesCyrillic();
        if (regularBytes != null && boldBytes != null) {
            regular = atlas.addFontFromMemoryTTF(regularBytes, FONT_SIZE, config, ranges);
            semiBold = atlas.addFontFromMemoryTTF(boldBytes, FONT_SIZE, config, ranges);
            small = atlas.addFontFromMemoryTTF(regularBytes, FONT_SIZE - 3.0f, config, ranges);
            title = atlas.addFontFromMemoryTTF(boldBytes, FONT_SIZE + 7.0f, config, ranges);
        } else {
            regular = semiBold = small = title = atlas.addFontDefault();
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

        fontTexture = new DynamicTexture(() -> "oyvey imgui font atlas", image);
        mc.getTextureManager().register(FONT_TEXTURE, fontTexture);
        atlas.setTexID(1);
        atlas.clearTexData();
    }

    /**
     * Fonts can be swapped for the real Product Sans by dropping the .ttf files into
     * .minecraft/oyvey/fonts; the bundled fallback is Google Sans (SIL OFL 1.1).
     */
    private static byte[] readFont(String overrideName, String bundledName) {
        try {
            Path override = FONT_OVERRIDE_DIR.resolve(overrideName);
            if (Files.isRegularFile(override)) {
                return Files.readAllBytes(override);
            }
            try (InputStream in = ImGuiImpl.class.getResourceAsStream("/assets/oyvey/font/" + bundledName)) {
                return in == null ? null : in.readAllBytes();
            }
        } catch (Exception e) {
            OyVey.LOGGER.error("Failed to load font {}", bundledName, e);
            return null;
        }
    }

    /**
     * Runs one ImGui frame and submits the result into {@code graphics}.
     */
    public static void frame(GuiGraphicsExtractor graphics, Runnable ui) {
        ensureInit();
        ImGuiIO io = ImGui.getIO();

        int fbWidth = mc.getWindow().getWidth();
        int fbHeight = mc.getWindow().getHeight();
        float scale = mc.getWindow().getGuiScale();
        io.setDisplaySize(fbWidth, fbHeight);
        io.setDisplayFramebufferScale(1.0f, 1.0f);

        long now = System.nanoTime();
        io.setDeltaTime(Math.max((now - lastFrame) / 1.0E9f, 1.0E-4f));
        lastFrame = now;

        io.addMousePosEvent(
                (float) (mc.mouseHandler.getScaledXPos(mc.getWindow()) * scale),
                (float) (mc.mouseHandler.getScaledYPos(mc.getWindow()) * scale));

        ImGui.newFrame();
        ui.run();
        ImGui.render();

        submit(graphics, ImGui.getDrawData(), scale);
        updateCursor(graphics);
    }

    private static void submit(GuiGraphicsExtractor graphics, ImDrawData data, float scale) {
        if (data == null || data.getCmdListsCount() == 0) return;

        TextureSetup texture = TextureSetup.singleTexture(fontTexture.getTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
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
                ScreenRectangle area = scissor == null ? rect : scissor.intersection(rect);
                if (area == null) continue;

                graphics.guiRenderState.addGuiElement(new ImGuiDrawElement(
                        RenderPipelines.GUI_TEXTURED, texture, vertices, colors, scale, area, area));
            }
        }
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

    // input, fed from ImGuiScreen

    public static void mouseButton(int button, boolean down) {
        if (!initialized || button < 0 || button > 4) return;
        ImGui.getIO().addMouseButtonEvent(button, down);
    }

    public static void mouseScroll(double horizontal, double vertical) {
        if (!initialized) return;
        ImGui.getIO().addMouseWheelEvent((float) horizontal, (float) vertical);
    }

    public static void key(int glfwKey, int modifiers, boolean down) {
        if (!initialized) return;
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
        ImGui.getIO().addInputCharacter(codepoint);
    }

    public static boolean wantsTextInput() {
        return initialized && ImGui.getIO().getWantTextInput();
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
