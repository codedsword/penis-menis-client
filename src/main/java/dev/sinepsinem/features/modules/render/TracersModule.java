package dev.sinepsinem.features.modules.render;

import dev.sinepsinem.event.impl.render.Render3DEvent;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.modules.misc.TeamsModule;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.TargetUtil;
import dev.sinepsinem.util.imgui.Theme;
import dev.sinepsinem.util.render.RenderUtil;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.awt.Color;

/**
 * Lines from your crosshair to other players, colored from the theme accent (close) to its
 * gradient end (far), teammates in the team color.
 */
public class TracersModule extends Module {
    private final Setting<Float> width = num("Width", 1.5f, 0.5f, 5.0f);
    private final Setting<Integer> maxDistance = num("MaxDistance", 128, 16, 512);
    private final Setting<Boolean> body = bool("ToBody", true);
    private final Setting<Integer> alpha = num("Alpha", 200, 20, 255);

    public TracersModule() {
        super("Tracers", "Lines to other players", Category.RENDER);
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        Camera camera = mc.gameRenderer.mainCamera();
        Vector3fc forward = camera.forwardVector();
        // a little in front of the camera, a line starting in the eye would be a single point
        Vec3 start = camera.position().add(forward.x(), forward.y(), forward.z());
        for (Player player : mc.level.players()) {
            if (!TargetUtil.isValidPlayer(player)) continue;
            double distance = player.distanceTo(mc.player);
            if (distance > maxDistance.getValue()) continue;
            Vec3 end = player.getPosition(event.getDelta()).add(0, body.getValue() ? player.getBbHeight() * 0.5 : 0, 0);
            RenderUtil.drawLine(start, end, color(player, distance), width.getValue());
        }
    }

    private Color color(Entity entity, double distance) {
        if (entity instanceof Player player && TeamsModule.isTeammate(player)) {
            Color team = TeamsModule.teamColor();
            return new Color(team.getRed(), team.getGreen(), team.getBlue(), alpha.getValue());
        }
        float t = (float) Math.min(1, distance / 48.0);
        int argb = Theme.toArgb(Theme.gradient(t));
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, alpha.getValue());
    }
}
