package dev.sinepsinem.features.modules.render;

import dev.sinepsinem.event.impl.entity.player.AttackEvent;
import dev.sinepsinem.event.impl.render.RenderOverlayEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.imgui.Draw;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.Theme;
import dev.sinepsinem.util.render.ProjectionUtil;
import imgui.ImDrawList;
import imgui.ImGui;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Custom glowing particles in the world: motes floating around you, a trail behind you while you
 * move, and a burst on every hit. They are simulated here and drawn as additive lights projected
 * onto the screen, under the hud.
 */
public class GlowParticlesModule extends Module {
    private final Setting<Boolean> ambient = bool("Ambient", true);
    private final Setting<Boolean> trail = bool("Trail", true);
    private final Setting<Boolean> hits = bool("Hits", true);
    private final Setting<Integer> density = num("Density", 6, 1, 20);
    private final Setting<Float> size = num("Size", 1.0f, 0.3f, 3.0f);
    private final Setting<Float> lifetime = num("Lifetime", 2.5f, 0.5f, 6.0f);
    private final Setting<Color> color = color("Color", 230, 98, 163, 255);
    private final Setting<Color> secondColor = color("SecondColor", 140, 82, 255, 255);
    private final Setting<Boolean> throughWalls = bool("ThroughWalls", false);

    private static final int MAX_PARTICLES = 600;

    private final ImGuiImpl.Layer layer = new ImGuiImpl.Layer("world_particles", false);
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private long lastFrame = System.nanoTime();

    public GlowParticlesModule() {
        super("GlowParticles", "Flying glow particles around you and on hits", Category.RENDER);
    }

    private static final class Particle {
        Vec3 pos, vel;
        float life, maxLife, size, hue, phase;
        float visibility = 1;
        boolean visible = true;
    }

    @Override
    public void onDisable() {
        particles.clear();
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        int amount = density.getValue();
        Vec3 feet = mc.player.position();

        if (ambient.getValue() && random.nextInt(20) < amount) {
            Vec3 around = feet.add((random.nextDouble() - 0.5) * 6, random.nextDouble() * 2.5, (random.nextDouble() - 0.5) * 6);
            spawn(around, new Vec3((random.nextDouble() - 0.5) * 0.2, 0.15 + random.nextDouble() * 0.25, (random.nextDouble() - 0.5) * 0.2), 1.0f);
        }

        Vec3 motion = mc.player.getKnownMovement();
        if (trail.getValue() && motion.horizontalDistanceSqr() > 0.004) {
            for (int i = 0; i < Math.max(1, amount / 3); i++) {
                Vec3 at = feet.add((random.nextDouble() - 0.5) * 0.6, random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.6);
                spawn(at, new Vec3((random.nextDouble() - 0.5) * 0.3, 0.3 + random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 0.3), 0.8f);
            }
        }

        // occlusion is a ray per particle, 20 times a second is plenty
        Vec3 camera = mc.gameRenderer.mainCamera().position();
        for (Particle p : particles) {
            p.visible = throughWalls.getValue() || mc.level.clip(new ClipContext(camera, p.pos,
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.MISS;
        }
    }

    @Subscribe
    public void onAttack(AttackEvent.Post event) {
        if (!hits.getValue()) return;
        Entity target = event.getTarget();
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.6, 0);
        for (int i = 0; i < 10 + density.getValue() * 3; i++) {
            Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.6 + 0.4, random.nextGaussian()).normalize();
            spawn(center, direction.scale(2.0 + random.nextDouble() * 3.0), 0.9f);
        }
    }

    // velocities are in blocks per second
    private void spawn(Vec3 pos, Vec3 velocity, float sizeFactor) {
        if (particles.size() >= MAX_PARTICLES) particles.removeFirst();
        Particle p = new Particle();
        p.pos = pos;
        p.vel = velocity;
        p.maxLife = lifetime.getValue() * (0.6f + random.nextFloat() * 0.8f);
        p.size = (0.03f + random.nextFloat() * 0.04f) * sizeFactor;
        p.hue = random.nextFloat();
        p.phase = random.nextFloat() * (float) Math.PI * 2;
        particles.add(p);
    }

    @Subscribe
    public void onRenderOverlay(RenderOverlayEvent event) {
        long now = System.nanoTime();
        float dt = Math.min((now - lastFrame) / 1.0E9f, 0.1f);
        lastFrame = now;

        particles.removeIf(p -> {
            p.life += dt;
            // drag, a little lift, and a slow swirl
            p.vel = p.vel.scale(Math.exp(-dt * 1.6)).add(
                    Math.sin(p.phase + p.life * 2) * 0.15 * dt, 0.25 * dt, Math.cos(p.phase + p.life * 2) * 0.15 * dt);
            p.pos = p.pos.add(p.vel.scale(dt));
            p.visibility += ((p.visible ? 1 : 0) - p.visibility) * Math.min(1, dt * 10);
            return p.life >= p.maxLife;
        });
        if (particles.isEmpty()) return;

        float s = mc.getWindow().getGuiScale();
        // pixels per block at a distance of one block
        float focal = (float) (mc.getWindow().getHeight() / (2 * Math.tan(Math.toRadians(mc.options.fov().get()) / 2)));
        int first = Theme.fromArgb(color.getValue().getRGB()), second = Theme.fromArgb(secondColor.getValue().getRGB());

        List<Projected> projected = new ArrayList<>();
        for (Particle p : particles) {
            if (p.visibility < 0.02f) continue;
            Vector3f screen = ProjectionUtil.project(p.pos);
            if (screen == null) continue;
            projected.add(new Projected(p, screen));
        }
        // far ones first, the additive blend does not care but the cores do
        projected.sort(Comparator.comparingDouble((Projected pr) -> pr.screen.z).reversed());

        ImGuiImpl.frame(layer, event.getContext(), () -> {
            ImDrawList draw = ImGui.getBackgroundDrawList();
            for (Projected pr : projected) {
                Particle p = pr.particle;
                float radius = p.size * size.getValue() * focal / pr.screen.z;
                if (radius < 0.2f) continue;
                // anything right in front of the camera would be a huge blob, let it fade out instead
                float near = Math.clamp((pr.screen.z - 0.8f) / 1.5f, 0, 1);
                float fade = Math.min(1, Math.min(p.life * 4, (p.maxLife - p.life) * 1.5f)) * p.visibility * near;
                if (fade < 0.01f) continue;
                float twinkle = 0.75f + 0.25f * (float) Math.sin(p.phase * 3 + p.life * 6);
                int tint = Theme.lerp(first, second, p.hue);
                float x = pr.screen.x * s, y = pr.screen.y * s;
                // a small hot core inside a wide soft halo
                Draw.light(draw, x, y, Math.min(radius * 5, 30 * s), Theme.withAlpha(tint, 0.55f * fade * twinkle));
                draw.addCircleFilled(x, y, Math.clamp(radius * 0.5f, 0.6f, 2.5f * s),
                        Theme.withAlpha(Theme.lerp(tint, Theme.TEXT, 0.7f), 0.95f * fade), 16);
            }
        });
    }

    private record Projected(Particle particle, Vector3f screen) {
    }
}
