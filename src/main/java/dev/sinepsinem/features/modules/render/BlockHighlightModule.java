package dev.sinepsinem.features.modules.render;

import dev.sinepsinem.event.impl.render.Render3DEvent;
import dev.sinepsinem.event.impl.render.RenderBlockOutlineEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.render.RenderUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.awt.*;

public class BlockHighlightModule extends Module {
    public final Setting<Color> color = color("Color", 255, 0, 0, 255);
    public final Setting<Float> lineWidth = num("LineWidth", 1.0f, 0.1f, 5.0f);

    public BlockHighlightModule() {
        super("BlockHighlight", "Draws box at the block that you are looking at", Category.RENDER);
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (mc.hitResult instanceof BlockHitResult result) {
            VoxelShape shape = mc.level.getBlockState(result.getBlockPos()).getShape(mc.level, result.getBlockPos());
            if (shape.isEmpty() || result.getType() == HitResult.Type.MISS) return;
            AABB aabb = shape.bounds();
            aabb = aabb.move(result.getBlockPos());
            RenderUtil.drawBox(aabb, color.getValue(), lineWidth.getValue());
        }
    }

    @Subscribe
    public void onRenderBlockOutline(RenderBlockOutlineEvent event) {
        event.cancel();
    }
}
