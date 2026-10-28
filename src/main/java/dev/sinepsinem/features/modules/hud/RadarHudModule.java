package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.TargetUtil;
import dev.sinepsinem.util.imgui.Theme;
import imgui.ImDrawList;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector2f;

/**
 * Ambient's "Radar": nearby players (and optionally mobs) as dots around you, facing up.
 */
public class RadarHudModule extends ImGuiHudModule {
    private static final float SIZE = 170, PAD = 8;

    private final Setting<Integer> range = num("Range", 48, 8, 128);
    private final Setting<Boolean> mobs = bool("Mobs", false);

    public RadarHudModule() {
        super("Radar", "Nearby players around you", 85, 95);
        pos.setValue(new Vector2f(0.86f, 0.2f));
    }

    @Override
    protected float[] draw(ImDrawList draw, float x, float y) {
        float height = TITLE + SIZE;
        float top = panel(draw, x, y, SIZE, height, "Radar");
        float cx = x + SIZE / 2, cy = top + (SIZE) / 2, radius = SIZE / 2 - PAD;

        // crosshair lines and range rings
        int line = Theme.rgba(0xFFFFFF, 18);
        draw.addLine(cx, top + PAD, cx, top + SIZE - PAD, line);
        draw.addLine(x + PAD, cy, x + SIZE - PAD, cy, line);
        draw.addCircle(cx, cy, radius / 2, line, 48);

        draw.pushClipRect(x + 2, top, x + SIZE - 2, top + SIZE - 2, true);
        float yaw = mc.player.getViewYRot(1.0f) * Mth.DEG_TO_RAD;
        float sin = Mth.sin(yaw), cos = Mth.cos(yaw);
        for (Entity entity : mc.level.entitiesForRendering()) {
            int color;
            if (entity instanceof Player player) {
                if (!TargetUtil.isValidPlayer(player)) continue;
                color = Theme.accent();
            } else if (mobs.getValue() && entity instanceof Enemy) {
                color = Theme.rgb(0xF87171);
            } else {
                continue;
            }
            double dx = entity.getX() - mc.player.getX(), dz = entity.getZ() - mc.player.getZ();
            // rotate into view space: forward is up on the radar
            double right = -dx * cos - dz * sin, forward = -dx * sin + dz * cos;
            float px = cx + (float) (right / range.getValue()) * radius;
            float py = cy - (float) (forward / range.getValue()) * radius;
            draw.addCircleFilled(px, py, 3.5f, color, 12);
        }
        draw.popClipRect();
        draw.addCircleFilled(cx, cy, 4, Theme.TEXT, 12);
        return new float[]{SIZE, height};
    }
}
