package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.Theme;
import imgui.ImDrawList;
import net.minecraft.world.level.Level;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ambient's bottom left readout: fps, blocks per second, ping and coordinates (with the other
 * dimension's), labels in the accent and values in white.
 */
public class InfoHudModule extends ImGuiHudModule {
    private final Setting<Boolean> fps = bool("Fps", true);
    private final Setting<Boolean> bps = bool("Bps", true);
    private final Setting<Boolean> ping = bool("Ping", true);
    private final Setting<Boolean> tps = bool("Tps", false);
    private final Setting<Boolean> coords = bool("Coords", true);
    private final Setting<Boolean> otherDimension = bool("OtherDimension", true);

    public InfoHudModule() {
        super("Info", "Fps, speed, ping and coordinates", 120, 40);
        pos.setValue(new Vector2f(0.004f, 0.88f));
    }

    @Override
    protected float[] draw(ImDrawList draw, float x, float y) {
        List<String[]> lines = new ArrayList<>();
        if (fps.getValue()) lines.add(new String[]{"FPS", String.valueOf(mc.getFps())});
        if (bps.getValue()) {
            lines.add(new String[]{"BPS", String.format(Locale.ROOT, "%.2f", SinepSinem.speedManager.getSpeedBpS(mc.player))});
        }
        if (ping.getValue()) lines.add(new String[]{"Ping", SinepSinem.serverManager.getPing() + "ms"});
        if (tps.getValue()) lines.add(new String[]{"TPS", String.format(Locale.ROOT, "%.1f", SinepSinem.serverManager.getTps())});
        if (coords.getValue()) {
            String xyz = mc.player.getBlockX() + ", " + mc.player.getBlockY() + ", " + mc.player.getBlockZ();
            if (otherDimension.getValue()) {
                if (mc.level.dimension() == Level.NETHER) {
                    xyz += " [O: " + mc.player.getBlockX() * 8 + ", " + mc.player.getBlockZ() * 8 + "]";
                } else if (mc.level.dimension() == Level.OVERWORLD) {
                    xyz += " [N: " + Math.floorDiv(mc.player.getBlockX(), 8) + ", " + Math.floorDiv(mc.player.getBlockZ(), 8) + "]";
                }
            }
            lines.add(new String[]{"XYZ", xyz});
        }

        float lineHeight = ImGuiImpl.FONT_SIZE + 2, width = 0;
        float total = lines.size() * lineHeight;
        float cy = y;
        for (int i = 0; i < lines.size(); i++) {
            String[] line = lines.get(i);
            String label = line[0] + ": ";
            int color = Theme.gradient(lines.size() == 1 ? 0 : (float) i / (lines.size() - 1));
            text(draw, ImGuiImpl.regular, x + 1, cy + 1, Theme.rgba(0x000000, 120), label + line[1]);
            text(draw, ImGuiImpl.regular, x, cy, color, label);
            float labelWidth = width(ImGuiImpl.regular, label);
            text(draw, ImGuiImpl.regular, x + labelWidth, cy, Theme.TEXT, line[1]);
            width = Math.max(width, labelWidth + width(ImGuiImpl.regular, line[1]));
            cy += lineHeight;
        }
        return new float[]{Math.max(width, 20), Math.max(total, lineHeight)};
    }
}
