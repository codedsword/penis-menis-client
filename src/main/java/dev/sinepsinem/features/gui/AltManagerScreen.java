package dev.sinepsinem.features.gui;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiMouseCursor;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.manager.AltManager;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.ImGuiScreen;
import dev.sinepsinem.util.imgui.Theme;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;

public class AltManagerScreen extends ImGuiScreen {
    private static final float WIDTH = 640, HEIGHT = 420;

    private final ImString username = new ImString(16);
    private final ImString token = new ImString(4096);
    private final ImString uuid = new ImString(36);
    private AltManager.Type type = AltManager.Type.OFFLINE;
    private @Nullable String status;
    private boolean statusError;

    public AltManagerScreen(@Nullable Screen parent) {
        super(Component.literal("Alt Manager"), parent);
    }

    @Override
    protected void renderImGui() {
        float displayW = ImGui.getIO().getDisplaySizeX();
        float displayH = ImGui.getIO().getDisplaySizeY();
        float width = Math.min(WIDTH, displayW - 40), height = Math.min(HEIGHT, displayH - 40);
        ImGui.setNextWindowPos((displayW - width) / 2, (displayH - height) / 2, ImGuiCond.Always);
        ImGui.setNextWindowSize(width, height, ImGuiCond.Always);

        ImGui.pushFont(ImGuiImpl.regular);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 20, 18);
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoScrollbar;
        if (ImGui.begin("##altmanager", flags)) {
            header();
            ImGui.dummy(0, 6);

            float listWidth = (ImGui.getContentRegionAvailX() - 16) * 0.55f;
            altList(listWidth);
            ImGui.sameLine(0, 16);
            form();
        }
        ImGui.end();
        ImGui.popStyleVar();
        ImGui.popFont();
    }

    private void header() {
        ImGui.pushFont(ImGuiImpl.title);
        ImGui.text("Alt Manager");
        ImGui.popFont();
        ImGui.pushFont(ImGuiImpl.small);
        ImGui.textColored(Theme.TEXT_DIM, "Logged in as ");
        ImGui.sameLine(0, 0);
        ImGui.textColored(Theme.ACCENT, minecraft.getUser().getName());
        ImGui.popFont();
    }

    private void altList(float width) {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8, 8);
        ImGui.beginChild("##alts", width, 0, true, ImGuiWindowFlags.AlwaysUseWindowPadding);
        AltManager alts = SinepSinem.altManager;
        if (alts.getAlts().isEmpty()) {
            ImGui.textColored(Theme.TEXT_FAINT, "No alts yet, add one on the right.");
        }

        AltManager.Alt remove = null;
        for (AltManager.Alt alt : new ArrayList<>(alts.getAlts())) {
            float x = ImGui.getCursorScreenPosX(), y = ImGui.getCursorScreenPosY();
            float w = ImGui.getContentRegionAvailX();
            boolean current = alts.isCurrent(alt);

            ImGui.invisibleButton("##alt_" + alt.name() + alt.type(), w - 64, 44);
            boolean hovered = ImGui.isItemHovered();
            if (hovered) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
            if (ImGui.isItemClicked(0)) login(alt);

            ImDrawList draw = ImGui.getWindowDrawList();
            draw.addRectFilled(x, y, x + w, y + 44, hovered ? Theme.SURFACE_HOVER : Theme.BACKGROUND, Theme.ROUNDING);
            draw.addRect(x, y, x + w, y + 44, current ? Theme.ACCENT : Theme.BORDER, Theme.ROUNDING, 0, 1);
            draw.addText(ImGuiImpl.regular, 17, x + 12, y + 5, Theme.TEXT, alt.name());
            draw.addText(ImGuiImpl.small, 14, x + 12, y + 24, current ? Theme.ACCENT : Theme.TEXT_FAINT,
                    current ? "Current session" : alt.type() == AltManager.Type.OFFLINE ? "Offline" : "Token");

            ImGui.sameLine(0, 4);
            ImGui.setCursorScreenPos(x + w - 70, y + 10);
            ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 6);
            if (ImGui.button("Remove##" + alt.name() + alt.type(), 60, 24)) remove = alt;
            ImGui.popStyleVar();
            ImGui.setCursorScreenPos(x, y + 50);
        }
        if (remove != null) {
            alts.remove(remove);
            SinepSinem.configManager.save();
        }
        ImGui.endChild();
        ImGui.popStyleVar();
    }

    private void form() {
        ImGui.beginGroup();
        float w = ImGui.getContentRegionAvailX();
        label("ADD ACCOUNT");

        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, Theme.ROUNDING);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 10, 7);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, Theme.SURFACE);

        if (segment("Offline", type == AltManager.Type.OFFLINE, w / 2 - 4)) type = AltManager.Type.OFFLINE;
        ImGui.sameLine(0, 8);
        if (segment("Token", type == AltManager.Type.TOKEN, w / 2 - 4)) type = AltManager.Type.TOKEN;

        ImGui.setNextItemWidth(w);
        ImGui.inputTextWithHint("##username", "Username", username);
        if (type == AltManager.Type.TOKEN) {
            ImGui.setNextItemWidth(w);
            ImGui.inputTextWithHint("##token", "Access token", token, imgui.flag.ImGuiInputTextFlags.Password);
            ImGui.setNextItemWidth(w);
            ImGui.inputTextWithHint("##uuid", "UUID (optional)", uuid);
        }

        ImGui.popStyleColor();
        ImGui.dummy(0, 2);
        if (accentButton("Add & Login", w)) submit(true);
        if (ImGui.button("Add", w / 2 - 4, 30)) submit(false);
        ImGui.sameLine(0, 8);
        if (ImGui.button("Random name", w / 2 - 4, 30)) username.set(AltManager.randomName());
        if (ImGui.button("Restore original session", w, 30)) {
            SinepSinem.altManager.restore();
            setStatus("Restored " + minecraft.getUser().getName(), false);
        }
        ImGui.popStyleVar(2);

        if (status != null) {
            ImGui.dummy(0, 4);
            ImGui.pushTextWrapPos(ImGui.getCursorPosX() + w);
            ImGui.textColored(statusError ? Theme.rgb(0xF87171) : Theme.rgb(0x4ADE80), status);
            ImGui.popTextWrapPos();
        }
        ImGui.endGroup();
    }

    private void submit(boolean login) {
        String name = username.get().trim();
        if (!AltManager.VALID_NAME.matcher(name).matches()) {
            setStatus("Usernames are 3-16 characters: letters, digits and _", true);
            return;
        }

        UUID id = null;
        String accessToken = null;
        if (type == AltManager.Type.TOKEN) {
            accessToken = token.get().trim();
            if (accessToken.isEmpty()) {
                setStatus("A token alt needs an access token", true);
                return;
            }
            String rawUuid = uuid.get().trim();
            if (!rawUuid.isEmpty()) {
                try {
                    id = UUID.fromString(rawUuid.length() == 32 ? rawUuid.replaceFirst(
                            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5") : rawUuid);
                } catch (IllegalArgumentException e) {
                    setStatus("Invalid UUID", true);
                    return;
                }
            }
        }

        AltManager.Alt alt = new AltManager.Alt(name, type, id, accessToken);
        SinepSinem.altManager.add(alt);
        SinepSinem.configManager.save();
        if (login) login(alt);
        else setStatus("Added " + name, false);
        username.set("");
        token.set("");
        uuid.set("");
    }

    private void login(AltManager.Alt alt) {
        SinepSinem.altManager.login(alt);
        setStatus("Logged in as " + alt.name(), false);
    }

    private void setStatus(String text, boolean error) {
        status = text;
        statusError = error;
    }

    private static boolean segment(String text, boolean selected, float width) {
        ImGui.pushStyleColor(ImGuiCol.Button, selected ? Theme.ACCENT : Theme.SURFACE);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, selected ? Theme.ACCENT_HOVER : Theme.BORDER);
        boolean clicked = ImGui.button(text, width, 30);
        ImGui.popStyleColor(2);
        return clicked;
    }

    private static boolean accentButton(String text, float width) {
        ImGui.pushStyleColor(ImGuiCol.Button, Theme.ACCENT);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, Theme.ACCENT_HOVER);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, Theme.ACCENT_HOVER);
        ImGui.pushFont(ImGuiImpl.regular);
        boolean clicked = ImGui.button(text, width, 32);
        ImGui.popFont();
        ImGui.popStyleColor(3);
        return clicked;
    }

    private static void label(String text) {
        ImGui.pushFont(ImGuiImpl.small);
        ImGui.textColored(Theme.ACCENT, text);
        ImGui.popFont();
    }
}
