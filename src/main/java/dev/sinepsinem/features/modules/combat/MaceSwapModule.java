package dev.sinepsinem.features.modules.combat;

import dev.sinepsinem.event.impl.entity.player.AttackEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.EnchantmentUtil;
import dev.sinepsinem.util.TickScheduler;
import dev.sinepsinem.util.inventory.InventoryUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Swaps to a mace for the hit when falling, then back to the previous slot.
 */
public class MaceSwapModule extends Module {
    private final Setting<Float> minFall = num("MinFallDistance", 1.6f, 0.0f, 10.0f);
    private final Setting<Prefer> prefer = mode("Prefer", Prefer.AUTO);
    private final Setting<Float> densityFall = num("DensityFrom", 6.0f, 1.0f, 30.0f);
    private final Setting<Boolean> swapBack = bool("SwapBack", true);

    private int previousSlot = -1;

    public MaceSwapModule() {
        super("MaceSwap", "Hits with a mace while falling", Category.COMBAT);
        densityFall.setVisibility(v -> prefer.getValue() == Prefer.AUTO);
    }

    @Subscribe
    public void onAttackPre(AttackEvent.Pre event) {
        if (nullCheck() || !(event.getTarget() instanceof LivingEntity)) return;
        if (mc.player.fallDistance < minFall.getValue() || mc.player.onGround()) return;
        if (mc.player.getMainHandItem().is(Items.MACE) && prefer.getValue() == Prefer.ANY) return;

        int slot = findMace();
        if (slot == -1 || slot == mc.player.getInventory().getSelectedSlot()) return;

        previousSlot = mc.player.getInventory().getSelectedSlot();
        InventoryUtil.swap(slot);
    }

    @Subscribe
    public void onAttackPost(AttackEvent.Post event) {
        if (previousSlot == -1) return;
        int slot = previousSlot;
        previousSlot = -1;
        // one slot change per tick, like a player pressing a number key
        if (swapBack.getValue()) TickScheduler.nextTick(() -> InventoryUtil.swap(slot));
    }

    private int findMace() {
        Prefer mode = prefer.getValue();
        if (mode == Prefer.AUTO) {
            // density scales with the fall, breach is better for short drops onto armored targets
            mode = mc.player.fallDistance >= densityFall.getValue() ? Prefer.DENSITY : Prefer.BREACH;
        }

        int fallback = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.is(Items.MACE)) continue;
            if (fallback == -1) fallback = i;
            if (mode == Prefer.DENSITY && EnchantmentUtil.has(Enchantments.DENSITY, stack)) return i;
            if (mode == Prefer.BREACH && EnchantmentUtil.has(Enchantments.BREACH, stack)) return i;
        }
        return fallback;
    }

    public enum Prefer {
        AUTO,
        DENSITY,
        BREACH,
        ANY
    }
}
