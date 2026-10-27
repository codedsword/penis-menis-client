package dev.sinepsinem.util.imgui;

import imgui.ImDrawList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Glowing motes that drift up the screen, sway, twinkle, shy away from the mouse and join up
 * with thin lines when they pass close to each other. Coordinates are ImGui pixels.
 */
public final class GlowParticles {
    private static final int SECONDARY = Theme.rgb(0x8C52FF);

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private boolean seeded;

    private static final class Particle {
        float x, y, vx, vy;
        float size, life, maxLife, phase, sway, hue;
    }

    /**
     * @param scale gui scale, sizes and speeds are given in gui pixels
     */
    public void update(float dt, float width, float height, int count, float scale, float mouseX, float mouseY) {
        // the first batch fills the screen, later ones rise from the bottom edge
        while (particles.size() < count) {
            particles.add(spawn(width, height, scale, !seeded));
        }
        seeded = true;

        float repel = 70 * scale;
        Iterator<Particle> iterator = particles.iterator();
        int alive = 0;
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            p.life += dt;
            float dx = p.x - mouseX, dy = p.y - mouseY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance < repel && distance > 0.01f) {
                float push = (1 - distance / repel) * 120 * scale * dt;
                p.x += dx / distance * push;
                p.y += dy / distance * push;
            }
            p.x += (p.vx + (float) Math.sin(p.phase + p.life * 1.3f) * p.sway) * dt;
            p.y += p.vy * dt;
            if (p.life > p.maxLife || p.y < -40 * scale || p.x < -40 * scale || p.x > width + 40 * scale || ++alive > count) {
                iterator.remove();
            }
        }
    }

    private Particle spawn(float width, float height, float scale, boolean anywhere) {
        Particle p = new Particle();
        p.x = random.nextFloat() * width;
        p.y = anywhere ? random.nextFloat() * height : height + random.nextFloat() * 20 * scale;
        p.vx = (random.nextFloat() - 0.5f) * 8 * scale;
        p.vy = -(10 + random.nextFloat() * 28) * scale;
        p.size = (0.6f + random.nextFloat() * random.nextFloat() * 2.2f) * scale;
        p.maxLife = 6 + random.nextFloat() * 10;
        p.life = anywhere ? random.nextFloat() * p.maxLife * 0.6f : 0;
        p.phase = random.nextFloat() * (float) Math.PI * 2;
        p.sway = (4 + random.nextFloat() * 10) * scale;
        p.hue = random.nextFloat();
        return p;
    }

    /**
     * Next update fills the whole screen again, e.g. when a gui opens.
     */
    public void reseed() {
        particles.clear();
        seeded = false;
    }

    public void draw(ImDrawList draw, int accent, float alpha, float scale) {
        float link = 55 * scale;
        for (int i = 0; i < particles.size(); i++) {
            Particle a = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle b = particles.get(j);
                float dx = a.x - b.x, dy = a.y - b.y;
                float distanceSq = dx * dx + dy * dy;
                if (distanceSq > link * link) continue;
                float strength = (1 - (float) Math.sqrt(distanceSq) / link) * Math.min(fade(a), fade(b));
                draw.addLine(a.x, a.y, b.x, b.y, Theme.withAlpha(accent, 0.25f * strength * alpha), Math.max(1, 0.5f * scale));
            }
        }

        for (Particle p : particles) {
            float twinkle = 0.7f + 0.3f * (float) Math.sin(p.phase * 3 + p.life * 4);
            float a = fade(p) * twinkle * alpha;
            int color = Theme.lerp(accent, SECONDARY, p.hue);
            Draw.light(draw, p.x, p.y, p.size * 7, Theme.withAlpha(color, 0.45f * a));
            draw.addCircleFilled(p.x, p.y, p.size, Theme.withAlpha(Theme.lerp(color, Theme.TEXT, 0.6f), 0.9f * a), 12);
        }
    }

    // fade in over the first second, out over the last two
    private static float fade(Particle p) {
        float in = Math.min(1, p.life);
        float out = Math.min(1, (p.maxLife - p.life) / 2);
        return Math.max(0, Math.min(in, out));
    }
}
