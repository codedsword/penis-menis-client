package me.alpha432.oyvey.features.gui;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImGuiColorEditFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiMouseCursor;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Bind;
import me.alpha432.oyvey.features.settings.EnumConverter;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.BuildConfig;
import me.alpha432.oyvey.util.imgui.ImGuiImpl;
import me.alpha432.oyvey.util.imgui.ImGuiScreen;
import me.alpha432.oyvey.util.imgui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ImGui click gui styled after legitclient.com: sidebar with categories on the left and
 * a list of module cards with inline settings on the right.
 */
public class ClickGuiScreen extends ImGuiScreen {
    private static final float WIDTH = 780, HEIGHT = 520, SIDEBAR = 180;
    private static final float CARD_HEIGHT = 58;

    private static ClickGuiScreen INSTANCE;

    private final Map<Object, Float> animations = new HashMap<>();
    private final Map<Setting<?>, ImString> textBuffers = new HashMap<>();
    private final ImString search = new ImString(64);

    private Module.Category category = Module.Category.COMBAT;
    private @Nullable Module expanded;
    private @Nullable Setting<Bind> listening;
    private float openAnimation;
    private float width = WIDTH, height = HEIGHT;

    private ClickGuiScreen() {
        super(Component.literal("OyVey"), null);
    }

    public static ClickGuiScreen getInstance() {
        if (INSTANCE == null) INSTANCE = new ClickGuiScreen();
        return INSTANCE;
    }

    public static void open(@Nullable Screen previous) {
        ClickGuiScreen gui = getInstance();
        gui.parent = previous;
        gui.openAnimation = 0;
        gui.listening = null;
        Minecraft.getInstance().gui.setScreen(gui);
    }

    @Override
    protected void renderImGui() {
        float dt = ImGui.getIO().getDeltaTime();
        openAnimation = approach(openAnimation, 1, dt * 6);
        float ease = easeOutCubic(openAnimation);

        float displayW = ImGui.getIO().getDisplaySizeX();
        float displayH = ImGui.getIO().getDisplaySizeY();
        width = Math.min(WIDTH, displayW - 40);
        height = Math.min(HEIGHT, displayH - 40);
        ImGui.setNextWindowPos((displayW - width) / 2, (displayH - height) / 2 + (1 - ease) * 30, ImGuiCond.Appearing);
        ImGui.setNextWindowSize(width, height, ImGuiCond.Always);

        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, ease);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
        ImGui.pushFont(ImGuiImpl.regular);
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse;
        if (ImGui.begin("##oyvey_clickgui", flags)) {
            sidebar();
            ImGui.sameLine(0, 0);
            content();
        }
        ImGui.end();
        ImGui.popFont();
        ImGui.popStyleVar(2);
    }

    private void sidebar() {
        ImGui.pushStyleColor(ImGuiCol.ChildBg, Theme.SURFACE);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 14, 18);
        ImGui.pushStyleVar(ImGuiStyleVar.ChildRounding, Theme.ROUNDING_LARGE);
        ImGui.beginChild("##sidebar", SIDEBAR, 0, false, ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.AlwaysUseWindowPadding);

        ImDrawList draw = ImGui.getWindowDrawList();
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();

        // logo, the pink mark from legitclient.com followed by the client name
        draw.addRectFilled(x, y + 4, x + 22, y + 26, Theme.ACCENT, 6);
        draw.addText(ImGuiImpl.title, 24, x + 30, y, Theme.TEXT, BuildConfig.NAME);
        ImGui.dummy(0, 40);

        label("MODULES");
        for (Module.Category c : OyVey.moduleManager.getCategories()) {
            if (c == Module.Category.HUD) continue;
            categoryButton(c);
        }

        ImGui.setCursorPosY(height - 54);
        ImGui.pushFont(ImGuiImpl.small);
        ImGui.textColored(Theme.TEXT_FAINT, "v" + BuildConfig.VERSION + " · " + BuildConfig.HASH);
        ImGui.textColored(Theme.TEXT_FAINT, OyVey.moduleManager.getModules().size() + " modules");
        ImGui.popFont();

        ImGui.endChild();
        ImGui.popStyleVar(2);
        ImGui.popStyleColor();
    }

    private void categoryButton(Module.Category c) {
        float width = ImGui.getContentRegionAvailX();
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        boolean clicked = ImGui.invisibleButton("##cat_" + c.name(), width, 34);
        boolean hovered = ImGui.isItemHovered();
        if (clicked) {
            category = c;
            expanded = null;
        }
        if (hovered) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);

        boolean selected = category == c && search.get().isEmpty();
        float hover = animate("cat_hover_" + c, hovered || selected ? 1 : 0, 12);
        float select = animate("cat_select_" + c, selected ? 1 : 0, 12);

        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addRectFilled(x, y, x + width, y + 34, Theme.withAlpha(Theme.BORDER, hover), Theme.ROUNDING);
        // accent pill slides in on the selected category
        draw.addRectFilled(x, y + 9, x + 3 * select, y + 25, Theme.ACCENT, 2);
        int textColor = Theme.lerp(Theme.TEXT_DIM, Theme.TEXT, hover);
        draw.addText(ImGuiImpl.semiBold, 17, x + 12 + 4 * select, y + 8, textColor, c.getName());
        int count = OyVey.moduleManager.getModulesByCategory(c).size();
        String countText = String.valueOf(count);
        float countWidth = ImGui.calcTextSize(countText).x;
        draw.addText(ImGuiImpl.small, 14, x + width - countWidth - 10, y + 10, Theme.TEXT_FAINT, countText);
    }

    private void content() {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 20, 18);
        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0);
        ImGui.beginChild("##content", 0, 0, false, ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.AlwaysUseWindowPadding);

        // header: title + search
        String query = search.get().trim().toLowerCase(Locale.ROOT);
        ImGui.pushFont(ImGuiImpl.title);
        ImGui.text(query.isEmpty() ? category.getName() : "Search");
        ImGui.popFont();
        ImGui.sameLine();
        float searchWidth = 220;
        ImGui.setCursorPosX(ImGui.getWindowWidth() - searchWidth - 20);
        ImGui.setCursorPosY(ImGui.getCursorPosY() + 2);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, Theme.ROUNDING);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 10, 6);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, Theme.SURFACE);
        ImGui.setNextItemWidth(searchWidth);
        ImGui.inputTextWithHint("##search", "Search modules...", search);
        ImGui.popStyleColor();
        ImGui.popStyleVar(2);

        ImGui.dummy(0, 4);
        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
        ImGui.beginChild("##modules", 0, 0, false);

        List<Module> modules = query.isEmpty()
                ? OyVey.moduleManager.getModulesByCategory(category)
                : OyVey.moduleManager.stream()
                .filter(m -> m.getName().toLowerCase(Locale.ROOT).contains(query)
                        || m.getDescription().toLowerCase(Locale.ROOT).contains(query))
                .toList();
        for (Module module : modules) {
            if (module.hidden) continue;
            moduleCard(module);
        }
        ImGui.dummy(0, 6);

        ImGui.endChild();
        ImGui.popStyleVar();
        ImGui.popStyleColor();

        ImGui.endChild();
        ImGui.popStyleColor();
        ImGui.popStyleVar();
    }

    private void moduleCard(Module module) {
        float width = ImGui.getContentRegionAvailX() - 10;
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        boolean open = expanded == module;

        float settingsHeight = open ? settingsHeight(module) : 0;
        float openAnim = animate("open_" + module.getName(), open ? 1 : 0, 10);
        float height = CARD_HEIGHT + settingsHeight * easeOutCubic(openAnim);

        ImDrawList draw = ImGui.getWindowDrawList();
        boolean headerHovered = ImGui.isMouseHoveringRect(x, y, x + width, y + CARD_HEIGHT) && ImGui.isWindowHovered();
        float hover = animate("hover_" + module.getName(), headerHovered ? 1 : 0, 12);
        float enabled = animate("enabled_" + module.getName(), module.isEnabled() ? 1 : 0, 12);

        int background = Theme.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hover);
        int border = Theme.lerp(Theme.BORDER, Theme.withAlpha(Theme.ACCENT, 0.55f), enabled);
        draw.addRectFilled(x, y, x + width, y + height, background, Theme.ROUNDING_LARGE);
        draw.addRect(x, y, x + width, y + height, border, Theme.ROUNDING_LARGE, 0, 1.0f);

        draw.addText(ImGuiImpl.semiBold, 17, x + 16, y + 11, Theme.lerp(Theme.TEXT_DIM, Theme.TEXT, Math.max(enabled, hover)), module.getName());
        draw.addText(ImGuiImpl.small, 14, x + 16, y + 32, Theme.TEXT_FAINT, module.getDescription());

        // header click: left toggles, right expands settings
        ImGui.setCursorScreenPos(x, y);
        ImGui.invisibleButton("##card_" + module.getName(), width - 82, CARD_HEIGHT);
        if (ImGui.isItemHovered()) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
        if (ImGui.isItemClicked(0)) module.toggle();
        if (ImGui.isItemClicked(1)) expanded = open ? null : module;

        // settings chevron
        ImGui.setCursorScreenPos(x + width - 82, y);
        if (ImGui.invisibleButton("##expand_" + module.getName(), 26, CARD_HEIGHT)) {
            expanded = open ? null : module;
        }
        boolean chevronHovered = ImGui.isItemHovered();
        float cx = x + width - 69, cy = y + CARD_HEIGHT / 2;
        int chevronColor = chevronHovered ? Theme.TEXT : Theme.TEXT_FAINT;
        float rot = easeOutCubic(openAnim);
        draw.addLine(cx - 4, cy - 2 + rot * 3, cx, cy + 2 - rot * 3, chevronColor, 1.6f);
        draw.addLine(cx, cy + 2 - rot * 3, cx + 4, cy - 2 + rot * 3, chevronColor, 1.6f);

        // toggle switch
        ImGui.setCursorScreenPos(x + width - 56, y);
        if (ImGui.invisibleButton("##toggle_" + module.getName(), 56, CARD_HEIGHT)) module.toggle();
        toggleSwitch(draw, x + width - 52, y + CARD_HEIGHT / 2 - 9, enabled);

        ImGui.setCursorScreenPos(x, y + CARD_HEIGHT);
        if (open || openAnim > 0.01f) {
            float visible = settingsHeight(module) * easeOutCubic(openAnim);
            ImGui.pushClipRect(x, y + CARD_HEIGHT, x + width, y + CARD_HEIGHT + visible, true);
            draw.addLine(x + 16, y + CARD_HEIGHT, x + width - 16, y + CARD_HEIGHT, Theme.BORDER);
            ImGui.setCursorScreenPos(x + 16, y + CARD_HEIGHT + 10);
            ImGui.beginGroup();
            ImGui.pushItemWidth(width - 32);
            for (Setting<?> setting : module.getSettings()) {
                if (!setting.isVisible() || setting == module.enabled || setting.isVec2fSetting()) continue;
                setting(module, setting, width - 32);
            }
            ImGui.popItemWidth();
            ImGui.endGroup();
            ImGui.popClipRect();
            ImGui.setCursorScreenPos(x, y + height);
        }
        ImGui.dummy(width, 8);
    }

    private float settingsHeight(Module module) {
        float h = 20;
        for (Setting<?> setting : module.getSettings()) {
            if (!setting.isVisible() || setting == module.enabled || setting.isVec2fSetting()) continue;
            h += setting.isNumberSetting() ? 48 : 31;
        }
        return h;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setting(Module module, Setting setting, float width) {
        String id = "##" + module.getName() + "_" + setting.getName();
        Object value = setting.getValue();
        ImDrawList draw = ImGui.getWindowDrawList();
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();

        if (value instanceof Boolean bool) {
            ImGui.text(setting.getName());
            ImGui.setCursorScreenPos(x, y);
            if (ImGui.invisibleButton(id, width, 24)) setting.setValue(!bool);
            if (ImGui.isItemHovered()) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
            float anim = animate(setting, bool ? 1 : 0, 12);
            toggleSwitch(draw, x + width - 36, y + 3, anim);
            ImGui.dummy(0, 0);
        } else if (value instanceof Number number) {
            ImGui.text(setting.getName());
            String display = value instanceof Integer ? String.valueOf(number.intValue())
                    : String.format(Locale.ROOT, "%.2f", number.doubleValue());
            float valueWidth = ImGui.calcTextSize(display).x;
            draw.addText(x + width - valueWidth, y, Theme.TEXT_DIM, display);

            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0, 2);
            ImGui.pushStyleVar(ImGuiStyleVar.GrabMinSize, 12);
            ImGui.pushStyleVar(ImGuiStyleVar.GrabRounding, 6);
            ImGui.setNextItemWidth(width);
            if (value instanceof Integer i) {
                int[] v = {i};
                if (ImGui.sliderInt(id, v, ((Number) setting.getMin()).intValue(), ((Number) setting.getMax()).intValue(), "")) {
                    setting.setValue(v[0]);
                }
            } else {
                float[] v = {number.floatValue()};
                if (ImGui.sliderFloat(id, v, ((Number) setting.getMin()).floatValue(), ((Number) setting.getMax()).floatValue(), "")) {
                    float rounded = Math.round(v[0] * 100f) / 100f;
                    setting.setValue(value instanceof Double ? (Object) (double) rounded : (Object) rounded);
                }
            }
            if (ImGui.isItemHovered()) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
            ImGui.popStyleVar(3);
            ImGui.dummy(0, 2);
        } else if (value instanceof Enum<?> e) {
            ImGui.alignTextToFramePadding();
            ImGui.text(setting.getName());
            ImGui.sameLine();
            float comboWidth = Math.min(160, width / 2);
            ImGui.setCursorScreenPos(x + width - comboWidth, y);
            ImGui.setNextItemWidth(comboWidth);
            ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 6);
            if (ImGui.beginCombo(id, EnumConverter.getProperName(e))) {
                for (Enum<?> constant : e.getDeclaringClass().getEnumConstants()) {
                    if (ImGui.selectable(EnumConverter.getProperName(constant), constant == e)) {
                        setting.setValue(constant);
                    }
                }
                ImGui.endCombo();
            }
            ImGui.popStyleVar();
        } else if (value instanceof Bind bind) {
            ImGui.alignTextToFramePadding();
            ImGui.text(setting.getName());
            ImGui.sameLine();
            String text = listening == setting ? "Press a key..." : bind.toString();
            float buttonWidth = Math.max(80, ImGui.calcTextSize(text).x + 20);
            ImGui.setCursorScreenPos(x + width - buttonWidth, y);
            ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 6);
            if (listening == setting) ImGui.pushStyleColor(ImGuiCol.Button, Theme.ACCENT);
            if (ImGui.button(text + id, buttonWidth, 0)) {
                listening = listening == setting ? null : setting;
            }
            if (listening == setting) ImGui.popStyleColor();
            ImGui.popStyleVar();
        } else if (value instanceof Color color) {
            ImGui.alignTextToFramePadding();
            ImGui.text(setting.getName());
            ImGui.sameLine();
            ImGui.setCursorScreenPos(x + width - 22, y);
            float[] rgba = color.getRGBComponents(null);
            if (ImGui.colorEdit4(id, rgba, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.NoLabel
                    | ImGuiColorEditFlags.AlphaBar | ImGuiColorEditFlags.AlphaPreviewHalf)) {
                setting.setValue(new Color(rgba[0], rgba[1], rgba[2], rgba[3]));
            }
        } else if (value instanceof String str) {
            ImGui.alignTextToFramePadding();
            ImGui.text(setting.getName());
            ImGui.sameLine();
            float inputWidth = Math.min(200, width / 2);
            ImGui.setCursorScreenPos(x + width - inputWidth, y);
            ImString buffer = textBuffers.computeIfAbsent(setting, s -> new ImString(str, 64));
            if (!ImGui.isAnyItemActive() && !buffer.get().equals(str)) buffer.set(str);
            ImGui.setNextItemWidth(inputWidth);
            ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 6);
            if (ImGui.inputText(id, buffer, imgui.flag.ImGuiInputTextFlags.EnterReturnsTrue)) {
                if (setting == module.displayName) module.setDisplayName(buffer.get());
                else setting.setValue(buffer.get());
            }
            ImGui.popStyleVar();
        }

        ImGui.setCursorScreenPos(x, y + (value instanceof Number ? 48 : 31));
    }

    private static void toggleSwitch(ImDrawList draw, float x, float y, float t) {
        float w = 36, h = 18;
        int track = Theme.lerp(Theme.MUTED, Theme.ACCENT, t);
        draw.addRectFilled(x, y, x + w, y + h, track, h / 2);
        float knob = x + h / 2 + (w - h) * easeOutCubic(t);
        draw.addCircleFilled(knob, y + h / 2, h / 2 - 3, Theme.TEXT, 20);
    }

    private static void label(String text) {
        ImGui.pushFont(ImGuiImpl.small);
        ImGui.textColored(Theme.ACCENT, spaced(text));
        ImGui.popFont();
        ImGui.dummy(0, 2);
    }

    // poor man's tracking-widest
    private static String spaced(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) sb.append(c).append(' ');
        return sb.toString().trim();
    }

    private float animate(Object key, float target, float speed) {
        float current = animations.getOrDefault(key, target);
        float next = approach(current, target, ImGui.getIO().getDeltaTime() * speed);
        animations.put(key, next);
        return next;
    }

    private static float approach(float current, float target, float step) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) return target;
        return current + diff * Math.min(1, step);
    }

    private static float easeOutCubic(float t) {
        float f = 1 - t;
        return 1 - f * f * f;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening != null) {
            int key = event.key();
            listening.setValue(key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE
                    ? Bind.none() : Bind.keyboard(key));
            listening = null;
            return true;
        }
        if (!ImGuiImpl.wantsTextInput() && OyVey.moduleManager.getModuleByName("ClickGui") instanceof Module gui
                && gui.getBind().getKey() == event.key()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // mouse binds: middle and side buttons
        if (listening != null && event.button() > 1) {
            listening.setValue(Bind.mouse(event.button()));
            listening = null;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        listening = null;
        super.onClose();
        OyVey.configManager.save();
    }
}
