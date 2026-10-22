package me.alpha432.oyvey.util.render;

import me.alpha432.oyvey.util.render.state.RectRenderState;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3x2f;

import java.awt.*;

public class RenderUtil implements Util {

    public static void rect(GuiGraphicsExtractor context, float x1, float y1, float x2, float y2, int color) {
        context.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), color);
    }

    public static void rect(GuiGraphicsExtractor context, float x1, float y1, float x2, float y2, int color, float width) {
        int w = Math.max(1, Math.round(width));
        context.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y1) + w, color);
        context.fill(Math.round(x2) - w, Math.round(y1), Math.round(x2), Math.round(y2), color);
        context.fill(Math.round(x1), Math.round(y2) - w, Math.round(x2), Math.round(y2), color);
        context.fill(Math.round(x1), Math.round(y1), Math.round(x1) + w, Math.round(y2), color);
    }

    public static void horizontalGradient(GuiGraphicsExtractor context, float x1, float y1, float x2, float y2, Color left, Color right) {
        gradient(context, Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2),
                left.getRGB(), left.getRGB(), right.getRGB(), right.getRGB());
    }

    public static void verticalGradient(GuiGraphicsExtractor context, float x1, float y1, float x2, float y2, Color top, Color bottom) {
        gradient(context, Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2),
                top.getRGB(), bottom.getRGB(), bottom.getRGB(), top.getRGB());
    }

    public static void gradient(GuiGraphicsExtractor graphics,
                                int x1, int y1, int x2, int y2,
                                int topLeft, int bottomLeft, int bottomRight, int topRight) {
        graphics.guiRenderState.addGuiElement(new RectRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(graphics.pose()),
                x1, y1, x2, y2,
                topLeft, bottomLeft, bottomRight, topRight,
                graphics.scissorStack.peek()
        ));
    }

    // 3d, only valid while a Render3DEvent is being posted

    public static void drawBoxFilled(AABB box, Color c) {
        Gizmos.cuboid(box, GizmoStyle.fill(c.getRGB())).setAlwaysOnTop();
    }

    public static void drawBoxFilled(BlockPos bp, Color c) {
        drawBoxFilled(new AABB(bp), c);
    }

    public static void drawBox(AABB box, Color c, float lineWidth) {
        Gizmos.cuboid(box, GizmoStyle.stroke(c.getRGB(), lineWidth)).setAlwaysOnTop();
    }

    public static void drawBox(AABB box, Color outline, Color fill, float lineWidth) {
        Gizmos.cuboid(box, GizmoStyle.strokeAndFill(outline.getRGB(), lineWidth, fill.getRGB())).setAlwaysOnTop();
    }

    public static void drawBox(VoxelShape shape, Color c, float lineWidth) {
        int color = c.getRGB();
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) ->
                Gizmos.line(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), color, lineWidth).setAlwaysOnTop());
    }

    public static void drawBox(BlockPos bp, Color c, float lineWidth) {
        drawBox(new AABB(bp), c, lineWidth);
    }

    public static void drawShape(VoxelShape shape, BlockPos pos, Color c, float lineWidth) {
        drawBox(shape.move(pos), c, lineWidth);
    }

    public static void drawLine(Vec3 from, Vec3 to, Color c, float lineWidth) {
        Gizmos.line(from, to, c.getRGB(), lineWidth).setAlwaysOnTop();
    }

    public static AABB box(BlockPos pos) {
        return Shapes.block().bounds().move(pos);
    }
}
