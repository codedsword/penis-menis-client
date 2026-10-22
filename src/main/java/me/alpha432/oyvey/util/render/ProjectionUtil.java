package me.alpha432.oyvey.util.render;

import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * World to gui coordinates. The matrices are captured while the level is extracted, which
 * happens right before the hud of the same frame.
 */
public final class ProjectionUtil implements Util {
    private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
    private static Vec3 cameraPos = Vec3.ZERO;

    private ProjectionUtil() {
    }

    public static void capture() {
        Camera camera = mc.gameRenderer.mainCamera();
        camera.getViewRotationProjectionMatrix(VIEW_PROJECTION);
        cameraPos = camera.position();
    }

    /**
     * @return x, y in gui coordinates and z as depth, or null if the point is behind the camera
     */
    public static @Nullable Vector3f project(Vec3 pos) {
        Vector4f clip = new Vector4f(
                (float) (pos.x - cameraPos.x), (float) (pos.y - cameraPos.y), (float) (pos.z - cameraPos.z), 1.0f);
        VIEW_PROJECTION.transform(clip);
        if (clip.w <= 0.05f) return null;

        float ndcX = clip.x / clip.w, ndcY = clip.y / clip.w;
        float width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight();
        return new Vector3f((ndcX + 1) * 0.5f * width, (1 - ndcY) * 0.5f * height, clip.w);
    }
}
