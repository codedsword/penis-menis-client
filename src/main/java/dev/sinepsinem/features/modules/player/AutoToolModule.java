package dev.sinepsinem.features.modules.player;

import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Picks the fastest hotbar tool for the block you are mining, and optionally goes back to the
 * slot you had once you stop.
 */
public class AutoToolModule extends Module {
    private final Setting<Boolean> switchBack = bool("SwitchBack", true);
    private final Setting<Boolean> saveDurability = bool("SaveDurability", true);

    private int previousSlot = -1;

    public AutoToolModule() {
        super("AutoTool", "Switches to the best tool while mining", Category.PLAYER);
    }

    @Override
    public void onDisable() {
        previousSlot = -1;
    }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        boolean mining = mc.options.keyAttack.isDown() && mc.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK && !mc.level.getBlockState(hit.getBlockPos()).isAir();
        if (!mining) {
            if (switchBack.getValue() && previousSlot != -1) {
                mc.player.getInventory().setSelectedSlot(previousSlot);
            }
            previousSlot = -1;
            return;
        }

        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        int current = mc.player.getInventory().getSelectedSlot();
        int best = current;
        float bestSpeed = speed(mc.player.getInventory().getItem(current), state);
        for (int slot = 0; slot < 9; slot++) {
            float speed = speed(mc.player.getInventory().getItem(slot), state);
            if (speed > bestSpeed + 0.01f) {
                best = slot;
                bestSpeed = speed;
            }
        }
        if (best != current) {
            if (previousSlot == -1) previousSlot = current;
            mc.player.getInventory().setSelectedSlot(best);
        }
    }

    private float speed(ItemStack stack, BlockState state) {
        // never wear a tool down to nothing
        if (saveDurability.getValue() && stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() < 5) return 0;
        float speed = stack.getDestroySpeed(state);
        return state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state) ? speed * 0.3f : speed;
    }
}
