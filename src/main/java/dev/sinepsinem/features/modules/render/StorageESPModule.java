package dev.sinepsinem.features.modules.render;

import dev.sinepsinem.event.impl.render.Render3DEvent;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.render.RenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Outlines storage blocks around you. Glow stacks a few widening outlines so the box looks lit,
 * like Ambient's glowing chests.
 */
public class StorageESPModule extends Module {
    public enum Mode {
        GLOW, BOX, OUTLINE
    }

    private final Setting<Mode> mode = mode("Mode", Mode.GLOW);
    private final Setting<Integer> range = num("Range", 64, 8, 256);
    private final Setting<Boolean> chests = bool("Chests", true);
    private final Setting<Boolean> enderChests = bool("EnderChests", true);
    private final Setting<Boolean> shulkers = bool("Shulkers", true);
    private final Setting<Boolean> barrels = bool("Barrels", true);
    private final Setting<Boolean> hoppers = bool("Hoppers", false);
    private final Setting<Boolean> furnaces = bool("Furnaces", false);
    private final Setting<Boolean> dispensers = bool("Dispensers", false);
    private final Setting<Color> chestColor = color("ChestColor", 255, 0, 145, 255).group("Colors");
    private final Setting<Color> enderColor = color("EnderColor", 140, 82, 255, 255).group("Colors");
    private final Setting<Color> shulkerColor = color("ShulkerColor", 255, 120, 200, 255).group("Colors");
    private final Setting<Color> otherColor = color("OtherColor", 200, 200, 200, 255).group("Colors");

    private final List<Entry> found = new ArrayList<>();
    private int ticks;

    private record Entry(AABB box, Color color) {
    }

    public StorageESPModule() {
        super("StorageESP", "Highlights chests and other storage", Category.RENDER);
    }

    @Override
    public void onTick() {
        // block entities only change when chunks do, a scan every half second is plenty
        if (ticks++ % 10 != 0) return;
        found.clear();
        int chunks = Math.min(mc.options.getEffectiveRenderDistance(), range.getValue() / 16 + 1);
        ChunkPos center = mc.player.chunkPosition();
        double rangeSq = (double) range.getValue() * range.getValue();
        for (int x = -chunks; x <= chunks; x++) {
            for (int z = -chunks; z <= chunks; z++) {
                if (!(mc.level.getChunkSource().getChunk(center.x() + x, center.z() + z, ChunkStatus.FULL, false) instanceof LevelChunk chunk)) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    Color color = color(blockEntity);
                    BlockPos pos = blockEntity.getBlockPos();
                    if (color == null || pos.distToCenterSqr(mc.player.position()) > rangeSq) continue;
                    found.add(new Entry(bounds(pos), color));
                }
            }
        }
    }

    private AABB bounds(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        VoxelShape shape = state.getShape(mc.level, pos);
        return shape.isEmpty() ? RenderUtil.box(pos) : shape.bounds().move(pos);
    }

    private Color color(BlockEntity blockEntity) {
        return switch (blockEntity) {
            case ChestBlockEntity ignored when chests.getValue() -> chestColor.getValue();
            case EnderChestBlockEntity ignored when enderChests.getValue() -> enderColor.getValue();
            case ShulkerBoxBlockEntity ignored when shulkers.getValue() -> shulkerColor.getValue();
            case BarrelBlockEntity ignored when barrels.getValue() -> otherColor.getValue();
            case HopperBlockEntity ignored when hoppers.getValue() -> otherColor.getValue();
            case AbstractFurnaceBlockEntity ignored when furnaces.getValue() -> otherColor.getValue();
            case DispenserBlockEntity ignored when dispensers.getValue() -> otherColor.getValue();
            default -> null;
        };
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        for (Entry entry : found) {
            Color c = entry.color();
            switch (mode.getValue()) {
                case BOX -> RenderUtil.drawBox(entry.box(), c, alpha(c, 50), 1.5f);
                case OUTLINE -> RenderUtil.drawBox(entry.box(), c, 2.0f);
                case GLOW -> {
                    RenderUtil.drawBox(entry.box(), c, alpha(c, 35), 2.0f);
                    // widening, fading shells read as a glow around the outline
                    for (int i = 1; i <= 3; i++) {
                        RenderUtil.drawBox(entry.box().inflate(0.02 * i), alpha(c, 110 - i * 30), 2.0f + i * 1.5f);
                    }
                }
            }
        }
    }

    private static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    @Override
    public void onDisable() {
        found.clear();
    }
}
