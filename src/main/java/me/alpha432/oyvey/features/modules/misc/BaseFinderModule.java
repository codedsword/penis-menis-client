package me.alpha432.oyvey.features.modules.misc;

import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.alpha432.oyvey.util.chat.SimpleSignature.from;

/**
 * Scores loaded chunks by player made block entities (storage, crafting, signs, ...) and reports
 * the ones that look like a base.
 */
public class BaseFinderModule extends Module {
    private final Setting<Integer> threshold = num("Threshold", 12, 3, 60);
    private final Setting<Boolean> notify = bool("Notify", true);
    private final Setting<Boolean> sound = bool("Sound", true);
    private final Setting<Boolean> render = bool("Render", true);
    private final Setting<Boolean> storage = bool("StorageBoxes", true);
    private final Setting<Color> color = color("Color", 230, 98, 163, 255);
    private final Setting<Integer> renderRange = num("RenderRange", 128, 16, 512);

    private final Map<ChunkPos, Base> bases = new ConcurrentHashMap<>();

    public BaseFinderModule() {
        super("BaseFinder", "Finds bases in loaded chunks", Category.MISC);
        storage.setVisibility(v -> render.getValue());
        color.setVisibility(v -> render.getValue());
        renderRange.setVisibility(v -> render.getValue());
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            if (isEnabled()) scan(chunk, true);
        });
    }

    @Override
    public void onEnable() {
        bases.clear();
        if (nullCheck()) return;
        int radius = mc.options.getEffectiveRenderDistance();
        ChunkPos center = mc.player.chunkPosition();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (mc.level.getChunkSource().getChunk(center.x() + x, center.z() + z, ChunkStatus.FULL, false) instanceof LevelChunk chunk) {
                    scan(chunk, false);
                }
            }
        }
        if (notify.getValue()) {
            Command.sendMessage("Scanned loaded chunks, {global} %s {} possible bases", from(getName()), bases.size());
        }
    }

    @Override
    public void onDisable() {
        bases.clear();
    }

    private void scan(LevelChunk chunk, boolean announce) {
        int score = 0;
        List<BlockPos> storageBlocks = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            int weight = weight(entry.getValue());
            if (weight == 0) continue;
            score += weight;
            if (isStorage(entry.getValue())) storageBlocks.add(entry.getKey().immutable());
        }

        ChunkPos pos = chunk.getPos();
        if (score < threshold.getValue()) {
            bases.remove(pos);
            return;
        }

        boolean fresh = bases.put(pos, new Base(pos, score, storageBlocks)) == null;
        if (fresh && announce) {
            if (notify.getValue()) {
                Command.sendMessage("Possible base at {global} %s, %s {} (score %s, %s storage)", from(getName() + pos),
                        pos.getMiddleBlockX(), pos.getMiddleBlockZ(), score, storageBlocks.size());
            }
            if (sound.getValue() && mc.player != null) {
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }
    }

    // naturally generated chests are cheap, things only players make are expensive
    private static int weight(BlockEntity blockEntity) {
        return switch (blockEntity) {
            case ShulkerBoxBlockEntity ignored -> 6;
            case BeaconBlockEntity ignored -> 10;
            case EnderChestBlockEntity ignored -> 4;
            case HopperBlockEntity ignored -> 3;
            case CrafterBlockEntity ignored -> 3;
            case BrewingStandBlockEntity ignored -> 3;
            case EnchantingTableBlockEntity ignored -> 3;
            case SignBlockEntity ignored -> 2;
            case BannerBlockEntity ignored -> 2;
            case DropperBlockEntity ignored -> 2;
            case DispenserBlockEntity ignored -> 1;
            case AbstractFurnaceBlockEntity ignored -> 1;
            case BarrelBlockEntity ignored -> 1;
            case ChestBlockEntity ignored -> 1;
            case ChiseledBookShelfBlockEntity ignored -> 1;
            case JukeboxBlockEntity ignored -> 2;
            default -> 0;
        };
    }

    private static boolean isStorage(BlockEntity blockEntity) {
        return blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity
                || blockEntity instanceof ShulkerBoxBlockEntity || blockEntity instanceof EnderChestBlockEntity;
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!render.getValue()) return;
        double rangeSq = renderRange.getValue() * renderRange.getValue();
        Color c = color.getValue();
        Color fill = new Color(c.getRed(), c.getGreen(), c.getBlue(), 30);

        for (Base base : bases.values()) {
            double dx = base.pos.getMiddleBlockX() - mc.player.getX(), dz = base.pos.getMiddleBlockZ() - mc.player.getZ();
            if (dx * dx + dz * dz > rangeSq) continue;

            int y = base.storage.isEmpty() ? mc.player.getBlockY() : base.storage.getFirst().getY();
            AABB column = new AABB(base.pos.getMinBlockX(), y - 0.5, base.pos.getMinBlockZ(),
                    base.pos.getMaxBlockX() + 1, y + 0.5, base.pos.getMaxBlockZ() + 1);
            RenderUtil.drawBox(column, c, 1.5f);

            if (storage.getValue()) {
                for (BlockPos block : base.storage) {
                    RenderUtil.drawBox(new AABB(block), c, fill, 1.0f);
                }
            }
        }
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(bases.size());
    }

    private record Base(ChunkPos pos, int score, List<BlockPos> storage) {
    }
}
