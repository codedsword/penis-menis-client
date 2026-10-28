package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.Theme;
import imgui.ImDrawList;
import org.joml.Vector2f;

import java.util.Locale;

/**
 * Ambient's "Speed Graph": your horizontal speed over the last few seconds and its current value.
 */
public class SpeedGraphHudModule extends ImGuiHudModule {
    private static final float WIDTH = 210, HEIGHT = 120, PAD = 10;
    private static final int SAMPLES = 80;

    private final float[] samples = new float[SAMPLES];
    private int head;
    private long lastSample;

    public SpeedGraphHudModule() {
        super("SpeedGraph", "Graph of your speed", 105, 60);
        pos.setValue(new Vector2f(0.004f, 0.09f));
    }

    @Override
    protected float[] draw(ImDrawList draw, float x, float y) {
        float speed = (float) SinepSinem.speedManager.getSpeedBpS(mc.player);
        long now = System.currentTimeMillis();
        // one sample per tick, the graph covers four seconds
        if (now - lastSample >= 50) {
            lastSample = now;
            samples[head] = speed;
            head = (head + 1) % SAMPLES;
        }

        float top = panel(draw, x, y, WIDTH, HEIGHT, "Speed Graph");
        float gx = x + PAD, gy = top + 6, gw = WIDTH - PAD * 2, gh = HEIGHT - (top - y) - 34;
        float max = 6;
        for (float sample : samples) max = Math.max(max, sample * 1.15f);

        float px = 0, py = 0;
        for (int i = 0; i < SAMPLES; i++) {
            float value = samples[(head + i) % SAMPLES];
            float sx = gx + gw * i / (SAMPLES - 1), sy = gy + gh - gh * value / max;
            if (i > 0) {
                int color = Theme.gradient((float) i / SAMPLES);
                // a soft fill under the line, then the line
                draw.addRectFilledMultiColor(px, Math.min(py, sy), sx, gy + gh,
                        Theme.withAlpha(color, 0.18f), Theme.withAlpha(color, 0.18f), Theme.withAlpha(color, 0), Theme.withAlpha(color, 0));
                draw.addLine(px, py, sx, sy, color, 2.0f);
            }
            px = sx;
            py = sy;
        }

        String value = String.format(Locale.ROOT, "%.2f b/s", speed);
        text(draw, ImGuiImpl.regular, x + (WIDTH - width(ImGuiImpl.regular, value)) / 2, y + HEIGHT - 24, Theme.TEXT, value);
        return new float[]{WIDTH, HEIGHT};
    }
}
