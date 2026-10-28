package dev.sinepsinem.features.gui;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.modules.client.ClickGuiModule;
import dev.sinepsinem.features.modules.client.InterfaceModule;
import dev.sinepsinem.features.settings.Bind;
import dev.sinepsinem.features.settings.EnumConverter;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.BuildConfig;
import dev.sinepsinem.util.imgui.GlowParticles;
import dev.sinepsinem.util.imgui.Icons;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.ImGuiScreen;
import dev.sinepsinem.util.imgui.Theme;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.flag.ImDrawFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiMouseCursor;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Ambient's click gui: one floating panel per category (draggable by its header, collapsible),
 * modules as rows that light up with an accent gradient while enabled, settings inline under a
 * module (right click or the chevron), and a search bar above the hotbar.
 * <p>
 * Panel pencil: bind mode, the rows show their keybinds and clicking one starts listening.
 * Middle click on a module hides it from the array list.
 */
public class ClickGuiScreen extends ImGuiScreen {
    private static final float WIDTH = 220, HEADER = 42, ROW = 36, INSET = 8, GAP = 14, MARGIN = 28;
    private static final float SWITCH_W = 34, SWITCH_H = 18;

    private static ClickGuiScreen INSTANCE;

    private final Map<Object, Float> animations = new HashMap<>();
    private final Map<Object, Float> heights = new HashMap<>();
    private final Map<Module.Category, Panel> panels = new EnumMap<>(Module.Category.class);
    private final Map<Setting<?>, ImString> textBuffers = new HashMap<>();
    private final Map<Setting<?>, float[]> hsv = new HashMap<>();
    // expanded modules, groups, dropdowns and color pickers
    private final Set<Object> open = new HashSet<>();
    private final ImString search = new ImString(64);
    private final GlowParticles particles = new GlowParticles();

    private @Nullable Setting<Bind> listening;
    private @Nullable Module hovered;
    private float hoverTime;
    private float openAnimation;
    private boolean onlyEnabled;
    private boolean focusSearch;
    private long savedAt;

    private @Nullable Panel dragging;
    private float dragX, dragY;

    private static final class Panel {
        final Module.Category category;
        boolean collapsed;
        boolean bindMode;
        float scroll, scrollTarget;

        Panel(Module.Category category) {
            this.category = category;
        }
    }

    private ClickGuiScreen() {
        super(Component.literal(BuildConfig.NAME), null);
        for (Module.Category category : Module.Category.values()) {
            panels.put(category, new Panel(category));
        }
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
        gui.particles.reseed();
        Minecraft.getInstance().gui.setScreen(gui);
    }

    @Override
    protected void extractOverlay(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        // the backdrop is drawn by ImGui, see backdrop()
    }

    @Override
    protected void renderImGui() {
        float dt = ImGui.getIO().getDeltaTime();
        openAnimation = approach(openAnimation, 1, dt * 7);
        float ease = easeOutCubic(openAnimation);
        Theme.refresh();

        float displayW = ImGui.getIO().getDisplaySizeX();
        float displayH = ImGui.getIO().getDisplaySizeY();
        backdrop(dt, displayW, displayH, ease);

        ImGui.pushFont(ImGuiImpl.regular);
        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, ease);
        layout(displayW, displayH);
        hovered = null;
        for (Panel panel : panels.values()) {
            if (!visible(panel.category)) continue;
            panel(panel, displayW, displayH, ease);
        }
        searchBar(displayW, displayH);
        tooltip(dt);
        ImGui.popStyleVar();
        ImGui.popFont();
    }

    private void backdrop(float dt, float displayW, float displayH, float ease) {
        ImDrawList draw = ImGui.getBackgroundDrawList();
        ClickGuiModule gui = ClickGuiModule.getInstance();
        if (gui.darken.getValue()) {
            draw.addRectFilled(0, 0, displayW, displayH, Theme.rgba(0x000000, Math.round(80 * ease)));
        }
        InterfaceModule style = InterfaceModule.get();
        if (style == null || style.particles.getValue()) {
            float scale = Minecraft.getInstance().getWindow().getGuiScale();
            int count = style == null ? 70 : style.particleCount.getValue();
            particles.update(dt, displayW, displayH, count, scale, ImGui.getMousePosX(), ImGui.getMousePosY());
            particles.draw(draw, Theme.accent(), ease, scale);
        }
    }

    // panels that were never placed go in a row along the top, wrapping below the tallest panel of
    // the row when the screen is narrow
    private void layout(float displayW, float displayH) {
        float x = MARGIN, y = MARGIN, rowHeight = 0;
        for (Panel panel : panels.values()) {
            if (!visible(panel.category)) continue;
            float height = HEADER + Math.max(1, SinepSinem.moduleManager.getModulesByCategory(panel.category).size()) * ROW + 6;
            if (x + WIDTH > displayW - MARGIN) {
                x = MARGIN;
                y += rowHeight + GAP;
                rowHeight = 0;
            }
            Setting<Vector2f> pos = ClickGuiModule.getInstance().panel(panel.category);
            if (pos.getValue().x < 0) pos.setValueNoEvent(new Vector2f(x, y));
            rowHeight = Math.max(rowHeight, height);
            x += WIDTH + GAP;
        }
    }

    private boolean visible(Module.Category category) {
        if (category == Module.Category.SCRIPTS) return true;
        return !modules(category).isEmpty() || (query().isEmpty() && !onlyEnabled
                && SinepSinem.moduleManager.getModulesByCategory(category).stream().anyMatch(m -> !m.hidden));
    }

    private String query() {
        return search.get().trim().toLowerCase(Locale.ROOT);
    }

    private List<Module> modules(Module.Category category) {
        String query = query();
        List<Module> result = new ArrayList<>();
        for (Module module : SinepSinem.moduleManager.getModulesByCategory(category)) {
            if (module.hidden) continue;
            if (onlyEnabled && !module.isEnabled()) continue;
            if (!query.isEmpty() && !module.getName().toLowerCase(Locale.ROOT).contains(query)
                    && !module.getDisplayName().toLowerCase(Locale.ROOT).contains(query)) continue;
            result.add(module);
        }
        return result;
    }

    // panels

    private void panel(Panel panel, float displayW, float displayH, float ease) {
        Setting<Vector2f> pos = ClickGuiModule.getInstance().panel(panel.category);
        float x = Math.clamp(pos.getValue().x, 0, Math.max(0, displayW - WIDTH));
        float y = Math.clamp(pos.getValue().y, 0, Math.max(0, displayH - HEADER));
        float drawY = y + (1 - ease) * 24;

        float maxBody = Math.max(80, displayH - drawY - HEADER - 90);
        // the body is measured while it is drawn, until then estimate it from the module count
        float content = heights.getOrDefault(panel, modules(panel.category).size() * ROW + 2f);
        float body = animate(panel, panel.collapsed ? 0 : Math.min(content, maxBody), 16);

        ImGui.setNextWindowPos(x, drawY, ImGuiCond.Always);
        ImGui.setNextWindowSize(WIDTH, HEADER + body + (body > 0.5f ? 6 : 0), ImGuiCond.Always);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, Theme.ROUNDING_LARGE);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, Theme.withAlpha(Theme.PANEL, 0.96f));
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse | ImGuiWindowFlags.NoSavedSettings;
        if (ImGui.begin("##panel_" + panel.category.name(), flags)) {
            header(panel, pos, x, drawY, body, displayW, displayH);
            // drawn (and so measured) whenever the panel is open, even while it is still growing from nothing
            if (body > 0.5f || !panel.collapsed) {
                ImGui.setCursorScreenPos(x, drawY + HEADER);
                ImGui.beginChild("##body", WIDTH, Math.max(1, body) + 4, false, ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
                smoothScroll(panel);
                float start = ImGui.getCursorScreenPosY();
                List<Module> modules = modules(panel.category);
                for (Module module : modules) {
                    moduleRow(panel, module);
                }
                if (panel.category == Module.Category.SCRIPTS && modules.isEmpty()) {
                    hint("No scripts loaded, .script folder");
                }
                ImGui.dummy(WIDTH, 2);
                heights.put(panel, ImGui.getCursorScreenPosY() - start);
                ImGui.endChild();
            }
        }
        ImGui.end();
        ImGui.popStyleColor();
        ImGui.popStyleVar(3);
    }

    private void header(Panel panel, Setting<Vector2f> pos, float x, float y, float body, float displayW, float displayH) {
        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addRectFilled(x, y, x + WIDTH, y + HEADER, Theme.HEADER, Theme.ROUNDING_LARGE,
                body > 0.5f ? ImDrawFlags.RoundCornersTop : ImDrawFlags.RoundCornersAll);

        ImFont font = ImGuiImpl.heading;
        float textY = y + (HEADER - ImGuiImpl.FONT_SIZE - 2) / 2;
        draw.addText(font, (int) ImGuiImpl.FONT_SIZE + 2, x + 12, textY, Theme.TEXT, icon(panel.category) + "  " + panel.category.getName());

        // chevron collapses, pencil toggles bind mode, the rest of the header drags
        ImGui.setCursorScreenPos(x + WIDTH - 30, y);
        if (ImGui.invisibleButton("##collapse", 30, HEADER)) panel.collapsed = !panel.collapsed;
        boolean chevronHovered = ImGui.isItemHovered();
        ImGui.setCursorScreenPos(x + WIDTH - 58, y);
        if (ImGui.invisibleButton("##binds", 28, HEADER)) panel.bindMode = !panel.bindMode;
        boolean pencilHovered = ImGui.isItemHovered();
        if (pencilHovered) ImGui.setTooltip(panel.bindMode ? "Done editing binds" : "Edit keybinds");

        float iconY = y + (HEADER - ImGuiImpl.FONT_SIZE) / 2;
        int pencil = panel.bindMode || pencilHovered ? Theme.accent() : Theme.shade(Theme.accent(), 0.8f);
        draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE, x + WIDTH - 50, iconY, pencil, Icons.PENCIL);
        draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE, x + WIDTH - 24, iconY,
                chevronHovered ? Theme.TEXT : Theme.TEXT_DIM, panel.collapsed ? Icons.CHEVRON_DOWN : Icons.CHEVRON_UP);

        ImGui.setCursorScreenPos(x, y);
        ImGui.invisibleButton("##drag", WIDTH - 58, HEADER);
        if (ImGui.isItemActivated()) {
            dragging = panel;
            dragX = ImGui.getMousePosX() - x;
            dragY = ImGui.getMousePosY() - y;
        }
        if (dragging == panel) {
            if (ImGui.isItemActive()) {
                ImGui.setMouseCursor(ImGuiMouseCursor.ResizeAll);
                float nx = Math.clamp(ImGui.getMousePosX() - dragX, 0, Math.max(0, displayW - WIDTH));
                float ny = Math.clamp(ImGui.getMousePosY() - dragY, 0, Math.max(0, displayH - HEADER));
                pos.setValueNoEvent(new Vector2f(nx, ny));
            } else {
                dragging = null;
            }
        }
    }

    private static String icon(Module.Category category) {
        return switch (category) {
            case COMBAT -> Icons.SWORD;
            case GHOST -> Icons.GHOST;
            case MOVEMENT -> Icons.ZAP;
            case RENDER -> Icons.EYE;
            case PLAYER -> Icons.USER;
            case MISC -> Icons.SETTINGS;
            case CLIENT -> Icons.MONITOR;
            case HUD -> Icons.LAYOUT;
            case SCRIPTS -> Icons.FILE_CODE;
        };
    }

    // ImGui's own wheel handling is off for the body, scrolling eases towards a target instead
    private void smoothScroll(Panel panel) {
        float wheel = ImGui.getIO().getMouseWheel();
        if (wheel != 0 && ImGui.isWindowHovered(ImGuiHoveredFlags.ChildWindows)) {
            panel.scrollTarget -= wheel * 60f;
        }
        panel.scrollTarget = Math.clamp(panel.scrollTarget, 0f, ImGui.getScrollMaxY());
        float current = ImGui.getScrollY();
        if (Math.abs(current - panel.scrollTarget) > 0.5f) {
            ImGui.setScrollY(current + (panel.scrollTarget - current) * Math.min(1f, ImGui.getIO().getDeltaTime() * 14f));
        }
    }

    private void hint(String text) {
        float x = ImGui.getCursorScreenPosX(), y = ImGui.getCursorScreenPosY();
        ImGui.getWindowDrawList().addText(ImGuiImpl.small, (int) ImGuiImpl.FONT_SIZE - 3, x + 12, y + 10, Theme.TEXT_FAINT, text);
        ImGui.dummy(WIDTH, ROW);
    }

    // modules

    private void moduleRow(Panel panel, Module module) {
        ImDrawList draw = ImGui.getWindowDrawList();
        float x = ImGui.getCursorScreenPosX(), y = ImGui.getCursorScreenPosY();
        List<Setting<?>> settings = settings(module);
        boolean expanded = open.contains(module) && !settings.isEmpty();

        ImGui.invisibleButton("##module_" + module.getName(), WIDTH, ROW);
        boolean rowHovered = ImGui.isItemHovered();
        if (rowHovered) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
            hovered = module;
        }
        if (ImGui.isItemClicked(0)) {
            if (panel.bindMode) listening = module.bind;
            else module.toggle();
        }
        if (ImGui.isItemClicked(1) && !settings.isEmpty()) toggleOpen(module);
        if (ImGui.isItemClicked(2)) module.setDrawn(!module.isDrawn());

        float hover = animate("hover_" + module.getName(), rowHovered ? 1 : 0, 14);
        float enabled = animate("enabled_" + module.getName(), module.isEnabled() ? 1 : 0, 12);
        if (hover > 0.01f) draw.addRectFilled(x, y, x + WIDTH, y + ROW, Theme.withAlpha(Theme.FIELD, 0.6f * hover));
        if (enabled > 0.01f) {
            // Ambient's enabled row: accent fading out to the right
            int left = Theme.withAlpha(Theme.accent(), 0.32f * enabled);
            int right = Theme.withAlpha(Theme.secondary(), 0.06f * enabled);
            draw.addRectFilledMultiColor(x, y, x + WIDTH, y + ROW, left, right, right, left);
            draw.addRectFilled(x, y + 8, x + 2, y + ROW - 8, Theme.withAlpha(Theme.accent(), enabled), 1);
        }

        int textColor = Theme.lerp(rgbGray(0xD4), Theme.TEXT, Math.max(enabled, hover));
        draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE, x + 12, y + (ROW - ImGuiImpl.FONT_SIZE) / 2, textColor, module.getDisplayName());

        float right = x + WIDTH - 12;
        if (panel.bindMode) {
            boolean waiting = listening == module.bind;
            String key = waiting ? "..." : module.getBind().isEmpty() ? "-" : module.getBind().toString();
            chip(draw, right, y + (ROW - 22) / 2, key, waiting);
        } else if (!settings.isEmpty()) {
            float rot = easeOutCubic(animate("open_" + module.getName(), expanded ? 1 : 0, 12));
            draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE, right - 14, y + (ROW - ImGuiImpl.FONT_SIZE) / 2,
                    rowHovered ? Theme.TEXT : Theme.TEXT_DIM, rot > 0.5f ? Icons.CHEVRON_DOWN : Icons.CHEVRON_RIGHT);
            right -= 20;
        }
        if (!module.isDrawn() && !panel.bindMode) {
            draw.addText(ImGuiImpl.small, (int) ImGuiImpl.FONT_SIZE - 3, right - 16, y + (ROW - ImGuiImpl.FONT_SIZE + 3) / 2,
                    Theme.TEXT_FAINT, Icons.EYE_OFF);
        }

        float openAnim = animate("open_" + module.getName(), expanded ? 1 : 0, 12);
        if (openAnim > 0.01f) {
            float full = heights.getOrDefault(module, 0f);
            float visible = full * easeOutCubic(openAnim);
            ImGui.pushClipRect(x, y + ROW, x + WIDTH, y + ROW + visible, true);
            ImGui.setCursorScreenPos(x, y + ROW);
            float start = ImGui.getCursorScreenPosY();
            settingsList(module, settings, x + INSET, WIDTH - INSET * 2);
            ImGui.dummy(WIDTH, 4);
            heights.put(module, ImGui.getCursorScreenPosY() - start);
            ImGui.popClipRect();
            ImGui.setCursorScreenPos(x, y + ROW + visible);
            ImGui.dummy(WIDTH, 0);
        }
    }

    private void toggleOpen(Object key) {
        if (!open.remove(key)) open.add(key);
    }

    // the settings Ambient shows: drawn, keybind and display name live elsewhere (middle click, bind mode, commands)
    private static List<Setting<?>> settings(Module module) {
        List<Setting<?>> result = new ArrayList<>();
        for (Setting<?> setting : module.getSettings()) {
            if (setting == module.enabled || setting == module.drawn || setting == module.bind || setting == module.displayName) continue;
            if (setting.isVec2fSetting() || !setting.isVisible()) continue;
            result.add(setting);
        }
        return result;
    }

    private void settingsList(Module module, List<Setting<?>> settings, float x, float width) {
        // settings of a group render together in one box, where the group first appears
        Map<String, List<Setting<?>>> groups = new LinkedHashMap<>();
        for (Setting<?> setting : settings) {
            if (setting.getGroup() != null) groups.computeIfAbsent(setting.getGroup(), g -> new ArrayList<>()).add(setting);
        }
        Set<String> done = new HashSet<>();
        for (Setting<?> setting : settings) {
            String group = setting.getGroup();
            if (group == null) {
                ImGui.setCursorScreenPos(x, ImGui.getCursorScreenPosY());
                setting(module, setting, x, width);
            } else if (done.add(group)) {
                group(module, group, groups.get(group), x, width);
            }
        }
    }

    private void group(Module module, String group, List<Setting<?>> settings, float x, float width) {
        ImDrawList draw = ImGui.getWindowDrawList();
        Object key = module.getName() + "/" + group;
        boolean expanded = open.contains(key);
        float y = ImGui.getCursorScreenPosY() + 2;
        float full = heights.getOrDefault(key, 0f);
        float anim = easeOutCubic(animate(key, expanded ? 1 : 0, 12));
        float boxHeight = 28 + full * anim;

        draw.addRectFilled(x, y, x + width, y + boxHeight, Theme.withAlpha(Theme.FIELD, 0.55f), Theme.ROUNDING);
        draw.addRectFilled(x, y, x + width, y + 28, Theme.FIELD, Theme.ROUNDING,
                anim > 0.01f ? ImDrawFlags.RoundCornersTop : ImDrawFlags.RoundCornersAll);
        ImGui.setCursorScreenPos(x, y);
        if (ImGui.invisibleButton("##group_" + key, width, 28)) toggleOpen(key);
        boolean hover = ImGui.isItemHovered();
        draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE - 1, x + 10, y + 5, hover ? Theme.TEXT : Theme.TEXT_DIM, Module.spaced(group));
        draw.addText(ImGuiImpl.small, (int) ImGuiImpl.FONT_SIZE - 3, x + width - 20, y + 7, Theme.TEXT_DIM,
                expanded ? Icons.CHEVRON_UP : Icons.CHEVRON_DOWN);

        ImGui.setCursorScreenPos(x, y + 28);
        if (anim > 0.01f) {
            ImGui.pushClipRect(x, y + 28, x + width, y + boxHeight, true);
            float start = ImGui.getCursorScreenPosY();
            for (Setting<?> setting : settings) {
                ImGui.setCursorScreenPos(x + 6, ImGui.getCursorScreenPosY());
                setting(module, setting, x + 6, width - 12);
            }
            ImGui.dummy(width, 4);
            heights.put(key, ImGui.getCursorScreenPosY() - start);
            ImGui.popClipRect();
        }
        ImGui.setCursorScreenPos(x, y + boxHeight + 4);
        ImGui.dummy(width, 0);
    }

    // settings

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setting(Module module, Setting setting, float x, float width) {
        Object value = setting.getValue();
        String id = module.getName() + "_" + setting.getName();
        float y = ImGui.getCursorScreenPosY();
        if (value instanceof Boolean bool) {
            toggle(setting, bool, id, x, y, width);
        } else if (value instanceof Number number) {
            slider(setting, number, id, x, y, width);
        } else if (value instanceof Enum<?> e) {
            dropdown(setting, e, id, x, y, width);
        } else if (value instanceof Color color) {
            color(setting, color, id, x, y, width);
        } else if (value instanceof Bind bind) {
            bind(setting, bind, id, x, y, width);
        } else if (value instanceof String str) {
            text(module, setting, str, id, x, y, width);
        }
    }

    private void label(Setting<?> setting, float x, float y, float width) {
        ImDrawList draw = ImGui.getWindowDrawList();
        String name = Module.spaced(setting.getName());
        draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE - 1, x + 4, y, Theme.TEXT, name);
        if (!setting.getDescription().isEmpty()) {
            float nameWidth = textWidth(ImGuiImpl.regular, name) * (ImGuiImpl.FONT_SIZE - 1) / ImGuiImpl.FONT_SIZE;
            float ix = x + 8 + nameWidth, iy = y + 3;
            draw.addText(ImGuiImpl.small, (int) ImGuiImpl.FONT_SIZE - 5, ix, iy, Theme.TEXT_FAINT, Icons.INFO);
            if (ImGui.isMouseHoveringRect(ix - 2, iy - 2, ix + 14, iy + 14) && ImGui.isWindowHovered(ImGuiHoveredFlags.ChildWindows)) {
                tooltipText(setting.getDescription());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void toggle(Setting setting, boolean on, String id, float x, float y, float width) {
        label(setting, x, y + 6, width);
        ImGui.setCursorScreenPos(x, y);
        if (ImGui.invisibleButton("##" + id, width, 30)) setting.setValue(!on);
        if (ImGui.isItemHovered()) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
        switchWidget(ImGui.getWindowDrawList(), x + width - SWITCH_W - 2, y + 6, animate(setting, on ? 1 : 0, 14));
    }

    private static void switchWidget(ImDrawList draw, float x, float y, float t) {
        int track = Theme.lerp(Theme.TOGGLE_OFF, Theme.accentShade(0.62f), t);
        draw.addRectFilled(x, y, x + SWITCH_W, y + SWITCH_H, track, SWITCH_H / 2);
        float knob = x + SWITCH_H / 2 + (SWITCH_W - SWITCH_H) * easeOutCubic(t);
        draw.addCircleFilled(knob, y + SWITCH_H / 2, SWITCH_H / 2 - 3, Theme.lerp(Theme.KNOB_OFF, Theme.TEXT, t), 20);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void slider(Setting setting, Number number, String id, float x, float y, float width) {
        ImDrawList draw = ImGui.getWindowDrawList();
        float min = ((Number) setting.getMin()).floatValue(), max = ((Number) setting.getMax()).floatValue();
        label(setting, x, y + 4, width);
        String display = format(number);
        draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE - 1, x + width - 4 - textWidth(ImGuiImpl.regular, display) * 16 / 17f,
                y + 4, Theme.TEXT, display);

        float trackX = x + 4, trackW = width - 8, trackY = y + 30;
        ImGui.setCursorScreenPos(x, y + 20);
        ImGui.invisibleButton("##" + id, width, 20);
        if (ImGui.isItemHovered() || ImGui.isItemActive()) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
        if (ImGui.isItemActive()) {
            float fraction = Math.clamp((ImGui.getMousePosX() - trackX) / trackW, 0, 1);
            float raw = min + (max - min) * fraction;
            Object next;
            if (number instanceof Integer) next = Math.round(raw);
            else {
                float step = max - min > 20 ? 0.5f : max - min > 2 ? 0.1f : 0.01f;
                float snapped = Math.round(raw / step) * step;
                snapped = Math.round(snapped * 100f) / 100f;
                next = number instanceof Double ? (Object) (double) snapped : (Object) snapped;
            }
            if (!next.equals(number)) setting.setValue(next);
        }
        float fraction = max > min ? Math.clamp((((Number) setting.getValue()).floatValue() - min) / (max - min), 0, 1) : 0;
        float shown = animate(setting, fraction, 18);
        draw.addRectFilled(trackX, trackY - 2, trackX + trackW, trackY + 2, Theme.TRACK, 2);
        draw.addRectFilled(trackX, trackY - 2, trackX + trackW * shown, trackY + 2, Theme.accentShade(0.42f), 2);
        float knob = ImGui.isItemActive() ? 6.5f : 5.5f;
        draw.addCircleFilled(trackX + trackW * shown, trackY, knob, Theme.accent(), 20);
        ImGui.setCursorScreenPos(x, y + 44);
        ImGui.dummy(width, 0);
    }

    private static String format(Number number) {
        if (number instanceof Integer i) return String.valueOf(i);
        String text = String.format(Locale.ROOT, "%.2f", number.doubleValue());
        // 1.40 -> 1.4, 2.00 -> 2, like Ambient
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void dropdown(Setting setting, Enum<?> current, String id, float x, float y, float width) {
        ImDrawList draw = ImGui.getWindowDrawList();
        boolean expanded = open.contains(setting);
        Enum<?>[] constants = current.getDeclaringClass().getEnumConstants();
        float optionHeight = 24;
        float anim = easeOutCubic(animate("dropdown_" + id, expanded ? 1 : 0, 14));
        float boxY = y + 2, boxHeight = 28 + constants.length * optionHeight * anim + (anim > 0.01f ? 4 : 0);

        ImGui.setCursorScreenPos(x, boxY);
        if (ImGui.invisibleButton("##" + id, width, 28)) toggleOpen(setting);
        boolean hover = ImGui.isItemHovered();
        if (hover) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
        draw.addRectFilled(x, boxY, x + width, boxY + boxHeight, hover && !expanded ? Theme.FIELD_HOVER : Theme.FIELD, Theme.ROUNDING);
        String name = Module.spaced(setting.getName());
        draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE - 1, x + 10, boxY + 5, Theme.TEXT, name);

        // the value is cut short when it does not fit next to the name, "Av..." like Ambient
        float nameWidth = textWidth(ImGuiImpl.regular, name) * 16 / 17f;
        String value = ellipsize(EnumConverter.getProperName(current), width - nameWidth - 44);
        float valueWidth = textWidth(ImGuiImpl.regular, value) * 16 / 17f;
        draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE - 1, x + width - 28 - valueWidth, boxY + 5, Theme.TEXT_DIM, value);
        draw.addText(ImGuiImpl.small, (int) ImGuiImpl.FONT_SIZE - 3, x + width - 20, boxY + 7, Theme.TEXT_DIM,
                expanded ? Icons.CHEVRON_UP : Icons.CHEVRON_DOWN);

        if (anim > 0.01f) {
            float listY = boxY + 30;
            ImGui.pushClipRect(x, listY, x + width, boxY + boxHeight, true);
            for (int i = 0; i < constants.length; i++) {
                Enum<?> constant = constants[i];
                float oy = listY + i * optionHeight;
                ImGui.setCursorScreenPos(x, oy);
                if (ImGui.invisibleButton("##" + id + "_" + constant.name(), width, optionHeight)) {
                    setting.setValue(constant);
                    open.remove(setting);
                }
                boolean optionHover = ImGui.isItemHovered();
                int color = constant == current ? Theme.accent() : optionHover ? Theme.TEXT : Theme.TEXT_DIM;
                draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE - 1, x + 12, oy + 3, color, EnumConverter.getProperName(constant));
            }
            ImGui.popClipRect();
        }
        ImGui.setCursorScreenPos(x, boxY + boxHeight + 4);
        ImGui.dummy(width, 0);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void color(Setting setting, Color color, String id, float x, float y, float width) {
        ImDrawList draw = ImGui.getWindowDrawList();
        label(setting, x, y + 6, width);
        float sx = x + width - 42, sy = y + 7;
        ImGui.setCursorScreenPos(x, y);
        if (ImGui.invisibleButton("##" + id, width, 30)) toggleOpen(setting);
        if (ImGui.isItemHovered()) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
        checker(draw, sx, sy, sx + 40, sy + 16, 4);
        draw.addRectFilled(sx, sy, sx + 40, sy + 16, Theme.fromArgb(color.getRGB()), 4);

        float anim = easeOutCubic(animate("picker_" + id, open.contains(setting) ? 1 : 0, 14));
        float full = 196;
        if (anim > 0.01f) {
            float py = y + 32;
            ImGui.pushClipRect(x, py, x + width, py + full * anim, true);
            picker(setting, color, id, x, py, width);
            ImGui.popClipRect();
        }
        ImGui.setCursorScreenPos(x, y + 32 + full * anim);
        ImGui.dummy(width, 0);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void picker(Setting setting, Color color, String id, float x, float y, float width) {
        ImDrawList draw = ImGui.getWindowDrawList();
        float[] state = hsv.computeIfAbsent(setting, s -> toHsv(color));
        // re-read when the color was changed from somewhere else (a command, the hex field)
        if (Color.HSBtoRGB(state[0], state[1], state[2]) != (color.getRGB() | 0xFF000000)) {
            float[] fresh = toHsv(color);
            if (fresh[1] > 0.001f && fresh[2] > 0.001f) state[0] = fresh[0];
            state[1] = fresh[1];
            state[2] = fresh[2];
        }
        float alpha = color.getAlpha() / 255f;

        // saturation (x) and value (y)
        float h = 120;
        int hue = Theme.fromArgb(Color.HSBtoRGB(state[0], 1, 1));
        draw.addRectFilledMultiColor(x, y, x + width, y + h, Theme.TEXT, hue, hue, Theme.TEXT);
        draw.addRectFilledMultiColor(x, y, x + width, y + h, 0, 0, Theme.rgb(0x000000), Theme.rgb(0x000000));
        ImGui.setCursorScreenPos(x, y);
        ImGui.invisibleButton("##sv_" + id, width, h);
        if (ImGui.isItemActive()) {
            state[1] = Math.clamp((ImGui.getMousePosX() - x) / width, 0, 1);
            state[2] = 1 - Math.clamp((ImGui.getMousePosY() - y) / h, 0, 1);
            setting.setValue(withAlpha(new Color(Color.HSBtoRGB(state[0], state[1], state[2])), alpha));
        }
        float kx = x + state[1] * width, ky = y + (1 - state[2]) * h;
        draw.addCircle(kx, ky, 5, Theme.rgb(0x000000), 16, 2.5f);
        draw.addCircle(kx, ky, 5, Theme.TEXT, 16, 1.5f);

        // hue
        float hy = y + h + 8, bar = 10;
        float segment = width / 6;
        for (int i = 0; i < 6; i++) {
            int a = Theme.fromArgb(Color.HSBtoRGB(i / 6f, 1, 1)), b = Theme.fromArgb(Color.HSBtoRGB((i + 1) / 6f, 1, 1));
            draw.addRectFilledMultiColor(x + i * segment, hy, x + (i + 1) * segment, hy + bar, a, b, b, a);
        }
        ImGui.setCursorScreenPos(x, hy - 2);
        ImGui.invisibleButton("##hue_" + id, width, bar + 4);
        if (ImGui.isItemActive()) {
            state[0] = Math.clamp((ImGui.getMousePosX() - x) / width, 0, 0.999f);
            setting.setValue(withAlpha(new Color(Color.HSBtoRGB(state[0], state[1], state[2])), alpha));
        }
        knob(draw, x + state[0] * width, hy + bar / 2, Theme.fromArgb(Color.HSBtoRGB(state[0], 1, 1)));

        // alpha
        float ay = hy + bar + 8;
        checker(draw, x, ay, x + width, ay + bar, 0);
        int opaque = Theme.fromArgb(color.getRGB() | 0xFF000000);
        draw.addRectFilledMultiColor(x, ay, x + width, ay + bar, Theme.withAlpha(opaque, 0), opaque, opaque, Theme.withAlpha(opaque, 0));
        ImGui.setCursorScreenPos(x, ay - 2);
        ImGui.invisibleButton("##alpha_" + id, width, bar + 4);
        if (ImGui.isItemActive()) {
            float a = Math.clamp((ImGui.getMousePosX() - x) / width, 0, 1);
            setting.setValue(withAlpha(color, a));
        }
        knob(draw, x + alpha * width, ay + bar / 2, opaque);

        // hex field, copy and paste
        float fy = ay + bar + 10, fh = 26;
        String hex = hex(color);
        ImString buffer = textBuffers.computeIfAbsent(setting, s -> new ImString(hex, 16));
        float fieldW = width - 2 * (fh + 4);
        draw.addRectFilled(x, fy, x + fieldW, fy + fh, Theme.FIELD, Theme.ROUNDING);
        draw.addText(ImGuiImpl.small, (int) ImGuiImpl.FONT_SIZE - 3, x + 8, fy + 7, Theme.TEXT_FAINT, Icons.PIPETTE);
        ImGui.setCursorScreenPos(x + 24, fy + 2);
        ImGui.setNextItemWidth(fieldW - 26);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0, 3);
        boolean editing = ImGui.inputText("##hex_" + id, buffer, ImGuiInputTextFlags.EnterReturnsTrue | ImGuiInputTextFlags.AutoSelectAll);
        boolean active = ImGui.isItemActive();
        ImGui.popStyleVar();
        ImGui.popStyleColor();
        if (editing) {
            Color parsed = parseHex(buffer.get());
            if (parsed != null) setting.setValue(parsed);
        }
        if (!active && !buffer.get().equalsIgnoreCase(hex)) buffer.set(hex);

        float bx = x + fieldW + 4;
        if (iconButton(draw, "##copy_" + id, bx, fy, fh, Icons.COPY)) mc().keyboardHandler.setClipboard(hex);
        if (iconButton(draw, "##paste_" + id, bx + fh + 4, fy, fh, Icons.PASTE)) {
            Color parsed = parseHex(mc().keyboardHandler.getClipboard().trim());
            if (parsed != null) setting.setValue(parsed);
        }
    }

    private boolean iconButton(ImDrawList draw, String id, float x, float y, float size, String icon) {
        ImGui.setCursorScreenPos(x, y);
        boolean clicked = ImGui.invisibleButton(id, size, size);
        boolean hover = ImGui.isItemHovered();
        if (hover) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
        draw.addRectFilled(x, y, x + size, y + size, hover ? Theme.FIELD_HOVER : Theme.FIELD, Theme.ROUNDING);
        draw.addText(ImGuiImpl.small, (int) ImGuiImpl.FONT_SIZE - 3, x + (size - 14) / 2, y + (size - 14) / 2,
                hover ? Theme.TEXT : Theme.TEXT_DIM, icon);
        return clicked;
    }

    private static void knob(ImDrawList draw, float x, float y, int fill) {
        draw.addCircleFilled(x, y, 6, Theme.TEXT, 16);
        draw.addCircleFilled(x, y, 4, fill, 16);
    }

    private static void checker(ImDrawList draw, float x1, float y1, float x2, float y2, float rounding) {
        draw.addRectFilled(x1, y1, x2, y2, Theme.rgb(0xBFBFBF), rounding);
        float cell = 5;
        int dark = Theme.rgb(0x7F7F7F);
        for (float cy = y1; cy < y2; cy += cell) {
            for (float cx = x1 + ((Math.round((cy - y1) / cell) & 1) == 0 ? 0 : cell); cx < x2; cx += cell * 2) {
                draw.addRectFilled(cx, cy, Math.min(cx + cell, x2), Math.min(cy + cell, y2), dark);
            }
        }
    }

    private static float[] toHsv(Color color) {
        return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    }

    private static Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(Math.clamp(alpha, 0, 1) * 255));
    }

    private static String hex(Color color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    private static @Nullable Color parseHex(String text) {
        String hex = text.startsWith("#") ? text.substring(1) : text;
        try {
            if (hex.length() == 6) return new Color(Integer.parseInt(hex, 16));
            if (hex.length() == 8) {
                long value = Long.parseLong(hex, 16);
                return new Color((int) (value >> 24) & 0xFF, (int) (value >> 16) & 0xFF, (int) (value >> 8) & 0xFF, (int) value & 0xFF);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void bind(Setting setting, Bind bind, String id, float x, float y, float width) {
        label(setting, x, y + 6, width);
        boolean waiting = listening == setting;
        ImGui.setCursorScreenPos(x, y);
        if (ImGui.invisibleButton("##" + id, width, 30)) listening = waiting ? null : setting;
        if (ImGui.isItemHovered()) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
        chip(ImGui.getWindowDrawList(), x + width - 2, y + 4, waiting ? "..." : bind.toString(), waiting);
        ImGui.setCursorScreenPos(x, y + 30);
        ImGui.dummy(width, 0);
    }

    // a small key box, right aligned at x
    private static void chip(ImDrawList draw, float right, float y, String text, boolean active) {
        float w = Math.max(26, textWidth(ImGuiImpl.small, text) + 14);
        draw.addRectFilled(right - w, y, right, y + 22, active ? Theme.accentShade(0.62f) : Theme.FIELD, Theme.ROUNDING);
        draw.addText(ImGuiImpl.small, (int) ImGuiImpl.FONT_SIZE - 3, right - w + 7, y + 4, Theme.TEXT, text);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void text(Module module, Setting setting, String value, String id, float x, float y, float width) {
        label(setting, x, y + 6, width);
        float fieldW = Math.min(110, width / 2);
        ImString buffer = textBuffers.computeIfAbsent(setting, s -> new ImString(value, 64));
        ImGui.setCursorScreenPos(x + width - fieldW, y + 2);
        ImGui.setNextItemWidth(fieldW);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, Theme.ROUNDING);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 8, 4);
        if (ImGui.inputText("##" + id, buffer, ImGuiInputTextFlags.EnterReturnsTrue)) setting.setValue(buffer.get());
        boolean active = ImGui.isItemActive();
        ImGui.popStyleVar(2);
        if (!active && !buffer.get().equals(value)) buffer.set(value);
        ImGui.setCursorScreenPos(x, y + 32);
        ImGui.dummy(width, 0);
    }

    // search bar above the hotbar: eye shows only enabled modules, cloud saves the config

    private void searchBar(float displayW, float displayH) {
        float scale = Minecraft.getInstance().getWindow().getGuiScale();
        float w = 300, h = 36;
        float x = (displayW - w) / 2, y = displayH - 62 * scale - h;
        ImGui.setNextWindowPos(x, y, ImGuiCond.Always);
        ImGui.setNextWindowSize(w, h, ImGuiCond.Always);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, Theme.ROUNDING_LARGE);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, Theme.withAlpha(Theme.PANEL, 0.96f));
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoSavedSettings;
        if (ImGui.begin("##search_bar", flags)) {
            ImDrawList draw = ImGui.getWindowDrawList();
            draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE, x + 12, y + (h - ImGuiImpl.FONT_SIZE) / 2, Theme.TEXT_DIM, Icons.SEARCH);
            ImGui.setCursorScreenPos(x + 38, y + (h - 24) / 2);
            ImGui.setNextItemWidth(w - 38 - 70);
            ImGui.pushStyleColor(ImGuiCol.FrameBg, 0);
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0, 4);
            if (focusSearch) {
                ImGui.setKeyboardFocusHere();
                focusSearch = false;
            }
            if (ImGui.inputTextWithHint("##search", "Search...", search)) {
                for (Panel panel : panels.values()) panel.scrollTarget = 0;
            }
            ImGui.popStyleVar();
            ImGui.popStyleColor();

            float iy = y + (h - ImGuiImpl.FONT_SIZE) / 2;
            ImGui.setCursorScreenPos(x + w - 62, y);
            if (ImGui.invisibleButton("##only_enabled", 28, h)) onlyEnabled = !onlyEnabled;
            boolean eyeHover = ImGui.isItemHovered();
            if (eyeHover) ImGui.setTooltip(onlyEnabled ? "Showing enabled modules" : "Show only enabled modules");
            draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE, x + w - 56, iy,
                    onlyEnabled || eyeHover ? Theme.accent() : Theme.shade(Theme.accent(), 0.75f), Icons.EYE);

            ImGui.setCursorScreenPos(x + w - 32, y);
            if (ImGui.invisibleButton("##save", 28, h)) {
                SinepSinem.configManager.save();
                savedAt = System.currentTimeMillis();
            }
            boolean saveHover = ImGui.isItemHovered();
            boolean saved = System.currentTimeMillis() - savedAt < 1500;
            if (saveHover) ImGui.setTooltip("Save config");
            draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE, x + w - 26, iy,
                    saved || saveHover ? Theme.accent() : Theme.shade(Theme.accent(), 0.75f), saved ? Icons.CHECK : Icons.CLOUD);
        }
        ImGui.end();
        ImGui.popStyleColor();
        ImGui.popStyleVar(2);
    }

    // module descriptions after hovering a row for a moment
    private void tooltip(float dt) {
        if (hovered == null || dragging != null || !ClickGuiModule.getInstance().descriptions.getValue()) {
            hoverTime = 0;
            return;
        }
        hoverTime += dt;
        if (hoverTime > 0.6f && !hovered.getDescription().isEmpty()) tooltipText(hovered.getDescription());
    }

    private static void tooltipText(String text) {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 10, 6);
        ImGui.pushStyleColor(ImGuiCol.PopupBg, Theme.withAlpha(Theme.HEADER, 0.97f));
        ImGui.pushStyleColor(ImGuiCol.Border, Theme.withAlpha(Theme.accent(), 0.5f));
        ImGui.beginTooltip();
        ImGui.pushFont(ImGuiImpl.small);
        ImGui.textColored(Theme.TEXT_DIM, text);
        ImGui.popFont();
        ImGui.endTooltip();
        ImGui.popStyleColor(2);
        ImGui.popStyleVar();
    }

    // helpers

    private static float textWidth(ImFont font, String text) {
        ImGui.pushFont(font);
        float width = ImGui.calcTextSize(text).x;
        ImGui.popFont();
        return width;
    }

    // measured at the 16px the values are drawn with
    private static String ellipsize(String text, float maxWidth) {
        float scale = 16 / 17f;
        if (textWidth(ImGuiImpl.regular, text) * scale <= maxWidth) return text;
        for (int length = text.length() - 1; length > 1; length--) {
            String cut = text.substring(0, length) + "...";
            if (textWidth(ImGuiImpl.regular, cut) * scale <= maxWidth) return cut;
        }
        // always a hint of the value, even when it barely fits
        return text.substring(0, Math.min(2, text.length())) + "...";
    }

    private static int rgbGray(int value) {
        return Theme.rgb(value << 16 | value << 8 | value);
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
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

    // input

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening != null) {
            int key = event.key();
            listening.setValue(key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE
                    ? Bind.none() : Bind.keyboard(key));
            listening = null;
            return true;
        }
        if (!ImGuiImpl.wantsTextInput() && ClickGuiModule.getInstance().getBind().getKey() == event.key()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        // typing anywhere goes to the search bar
        if (!ImGuiImpl.wantsTextInput() && listening == null && Character.isLetterOrDigit(event.codepoint())) {
            focusSearch = true;
        }
        return super.charTyped(event);
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
        dragging = null;
        super.onClose();
        ClickGuiModule gui = ClickGuiModule.getInstance();
        if (gui.isEnabled()) gui.disable();
        SinepSinem.configManager.save();
    }
}
