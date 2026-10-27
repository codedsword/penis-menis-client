package dev.sinepsinem.features.modules.render;

import dev.sinepsinem.event.impl.render.Render3DEvent;
import dev.sinepsinem.event.impl.render.RenderOverlayEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.ProjectileSimulator;
import dev.sinepsinem.util.imgui.Draw;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.Theme;
import dev.sinepsinem.util.render.ProjectionUtil;
import dev.sinepsinem.util.render.RenderUtil;
import imgui.ImDrawList;
import imgui.ImGui;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shows where projectiles will land and how many seconds until they do: the one the held item
 * would shoot (bow while drawing, charged crossbow, trident, pearls, potions, snowballs, eggs,
 * experience bottles, wind charges) and every one already in the air.
 */
public class TrajectoriesModule extends Module {
    private final Setting<Boolean> held = bool("Held", true);
    private final Setting<Boolean> inFlight = bool("InFlight", true);
    private final Setting<Boolean> own = bool("OwnInFlight", true);
    private final Setting<Boolean> timer = bool("Timer", true);
    private final Setting<Color> color = color("Color", 230, 98, 163, 255);
    private final Setting<Color> landColor = color("LandColor", 140, 82, 255, 255);
    private final Setting<Float> width = num("LineWidth", 2.0f, 0.5f, 5.0f);

    // every prediction is up to a few hundred ray casts per frame
    private static final double MAX_DISTANCE = 128;
    private static final int MAX_IN_FLIGHT = 24;

    private final ImGuiImpl.Layer layer = new ImGuiImpl.Layer("trajectories", false);
    private final List<Prediction> predictions = new ArrayList<>();

    public TrajectoriesModule() {
        super("Trajectories", "Where projectiles land, and in how many seconds", Category.RENDER);
    }

    private record Prediction(ProjectileSimulator.Result result, @Nullable String label, float partialTicks, boolean aim) {
        float secondsLeft() {
            // in flight projectiles were simulated at the last tick, the frame is partialTicks into the next one
            return Math.max(0, (result.ticks() - (aim ? 0 : partialTicks)) / 20.0f);
        }
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        predictions.clear();
        float delta = event.getDelta();

        if (held.getValue()) {
            ProjectileSimulator.Shot shot = ProjectileSimulator.aim(mc.player, delta);
            if (shot != null) {
                predictions.add(new Prediction(ProjectileSimulator.simulate(shot.position(), shot.velocity(), shot.physics(), mc.player, mc.player),
                        shot.stack().getHoverName().getString(), delta, true));
            }
        }

        if (inFlight.getValue()) {
            int count = 0;
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof Projectile projectile) || ProjectileSimulator.physicsOf(projectile) == null) continue;
                if (projectile.distanceToSqr(mc.player) > MAX_DISTANCE * MAX_DISTANCE || count >= MAX_IN_FLIGHT) continue;
                // stuck arrows and tridents have no motion left
                if (projectile.getDeltaMovement().lengthSqr() < 1.0E-4) continue;
                if (!own.getValue() && projectile.getOwner() == mc.player) continue;
                ProjectileSimulator.Result result = ProjectileSimulator.simulate(projectile, projectile.position());
                predictions.add(new Prediction(result, projectile.getName().getString(), delta, false));
                count++;
            }
        }

        for (Prediction prediction : predictions) {
            draw(prediction);
        }
    }

    private void draw(Prediction prediction) {
        List<Vec3> points = prediction.result().points();
        Color from = color.getValue(), to = landColor.getValue();
        for (int i = 1; i < points.size(); i++) {
            float t = (float) i / points.size();
            RenderUtil.drawLine(points.get(i - 1), points.get(i), lerp(from, to, t), width.getValue());
        }

        HitResult hit = prediction.result().hit();
        Color land = new Color(to.getRed(), to.getGreen(), to.getBlue(), 70);
        if (hit instanceof EntityHitResult entityHit) {
            RenderUtil.drawBox(entityHit.getEntity().getBoundingBox(), to, land, width.getValue());
        } else if (hit instanceof BlockHitResult blockHit) {
            RenderUtil.drawBox(landingPlate(blockHit), to, land, width.getValue());
        }
    }

    // a thin square lying on the face the projectile hits
    private static AABB landingPlate(BlockHitResult hit) {
        Vec3 p = hit.getLocation();
        Direction face = hit.getDirection();
        double size = 0.3, thickness = 0.02;
        double sx = face.getAxis() == Direction.Axis.X ? thickness : size;
        double sy = face.getAxis() == Direction.Axis.Y ? thickness : size;
        double sz = face.getAxis() == Direction.Axis.Z ? thickness : size;
        return new AABB(p.x - sx, p.y - sy, p.z - sz, p.x + sx, p.y + sy, p.z + sz);
    }

    @Subscribe
    public void onRenderOverlay(RenderOverlayEvent event) {
        if (!timer.getValue() || predictions.isEmpty()) return;

        ImGuiImpl.frame(layer, event.getContext(), () -> {
            ImDrawList draw = ImGui.getBackgroundDrawList();
            float s = mc.getWindow().getGuiScale();
            for (Prediction prediction : predictions) {
                Vector3f screen = ProjectionUtil.project(prediction.result().end());
                if (screen == null) continue;
                label(draw, screen.x * s, screen.y * s, s, prediction);
            }
        });
    }

    private void label(ImDrawList draw, float x, float y, float s, Prediction prediction) {
        String time = String.format(Locale.ROOT, "%.2fs", prediction.secondsLeft());
        String detail = prediction.result().hit() instanceof EntityHitResult entityHit
                ? entityHit.getEntity().getName().getString()
                : prediction.result().landed() ? prediction.label() : "no landing";

        ImGui.pushFont(ImGuiImpl.regular);
        float timeWidth = ImGui.calcTextSize(time).x;
        ImGui.popFont();
        ImGui.pushFont(ImGuiImpl.small);
        float detailWidth = detail == null ? 0 : ImGui.calcTextSize(detail).x;
        ImGui.popFont();

        float padding = 8, height = 24;
        float w = timeWidth + (detail == null ? 0 : detailWidth + 8) + padding * 2;
        float x1 = x - w / 2, y1 = y - height - 10, x2 = x + w / 2, y2 = y - 10;
        Draw.panel(draw, x1, y1, x2, y2, height / 2, 1.0f, s);
        // pointer down to the landing spot
        draw.addTriangleFilled(x - 5, y2, x + 5, y2, x, y - 4, Theme.withAlpha(Theme.fromArgb(landColor.getValue().getRGB()), 0.9f));
        draw.addText(ImGuiImpl.regular, (int) ImGuiImpl.FONT_SIZE, x1 + padding, y1 + (height - ImGuiImpl.FONT_SIZE) / 2,
                Theme.fromArgb(color.getValue().getRGB() | 0xFF000000), time);
        if (detail != null) {
            draw.addText(ImGuiImpl.small, (int) ImGuiImpl.FONT_SIZE - 3, x1 + padding + timeWidth + 8, y1 + (height - ImGuiImpl.FONT_SIZE + 3) / 2,
                    Theme.TEXT_DIM, detail);
        }
    }

    private static Color lerp(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }
}
