package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.BuildConfig;
import dev.sinepsinem.util.imgui.Icons;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.Theme;
import imgui.ImDrawList;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Ambient's watermarks: the client name with its first letter in the accent, or the "modern"
 * row of pills (logo, account name, fps).
 */
public class WatermarkHudModule extends ImGuiHudModule {
    public enum Style {
        PILLS, TEXT
    }

    public final Setting<Style> style = mode("Style", Style.PILLS);
    public final Setting<String> text = str("Text", BuildConfig.NAME);
    public final Setting<Boolean> fps = bool("Fps", true);
    public final Setting<Boolean> account = bool("Account", true);

    public WatermarkHudModule() {
        super("Watermark", "Client name, account and fps", 100, 12);
        pos.setValue(new Vector2f(0.004f, 0.006f));
        fps.setVisibility(v -> style.getValue() == Style.PILLS);
        account.setVisibility(v -> style.getValue() == Style.PILLS);
    }

    @Override
    protected float[] draw(ImDrawList draw, float x, float y) {
        String name = text.getValue().isEmpty() ? BuildConfig.NAME : text.getValue();
        if (style.getValue() == Style.TEXT) {
            String first = name.substring(0, 1), rest = name.substring(1);
            text(draw, ImGuiImpl.heading, x, y, Theme.accent(), first);
            float firstWidth = width(ImGuiImpl.heading, first);
            text(draw, ImGuiImpl.heading, x + firstWidth, y, Theme.TEXT, rest);
            return new float[]{firstWidth + width(ImGuiImpl.heading, rest), ImGuiImpl.FONT_SIZE + 4};
        }

        float h = 30, cx = x;
        // logo: the client's initial in an accent square
        panel(draw, cx, y, h, h, null);
        String initial = name.substring(0, 1).toUpperCase();
        text(draw, ImGuiImpl.heading, cx + (h - width(ImGuiImpl.heading, initial)) / 2, y + (h - 19) / 2, Theme.accent(), initial);
        cx += h + 6;

        List<String[]> parts = new ArrayList<>();
        if (account.getValue()) parts.add(new String[]{Icons.USER, mc.getUser().getName()});
        if (fps.getValue()) parts.add(new String[]{Icons.ZAP, mc.getFps() + "fps"});
        if (parts.isEmpty()) parts.add(new String[]{Icons.SPARKLES, name});

        float gap = 14, w = 22 - gap;
        for (String[] part : parts) w += width(ImGuiImpl.regular, part[0] + " ") + width(ImGuiImpl.regular, part[1]) + gap;
        panel(draw, cx, y, w, h, null);
        // icon in the accent, value in white, like Ambient's pills
        float tx = cx + 11, ty = y + (h - ImGuiImpl.FONT_SIZE) / 2;
        for (String[] part : parts) {
            text(draw, ImGuiImpl.regular, tx, ty, Theme.accent(), part[0]);
            tx += width(ImGuiImpl.regular, part[0] + " ");
            text(draw, ImGuiImpl.regular, tx, ty, Theme.TEXT, part[1]);
            tx += width(ImGuiImpl.regular, part[1]) + gap;
        }
        return new float[]{cx + w - x, h};
    }
}
