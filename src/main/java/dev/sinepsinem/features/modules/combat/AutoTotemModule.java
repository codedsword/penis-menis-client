package dev.sinepsinem.features.modules.combat;

import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.inventory.InventoryUtil;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

/**
 * Keeps a totem of undying in the offhand.
 */
public class AutoTotemModule extends Module {
    private static final int OFFHAND_BUTTON = 40;

    private final Setting<Integer> delay = num("Delay", 1, 0, 10);
    private final Setting<Boolean> onlyInventory = bool("OnlyInventoryOpen", false);

    private int ticks;

    public AutoTotemModule() {
        super("AutoTotem", "Keeps a totem in your offhand", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        if (mc.gui.screen() != null && !(mc.gui.screen() instanceof InventoryScreen)) return;
        if (onlyInventory.getValue() && !(mc.gui.screen() instanceof InventoryScreen)) return;
        if (!mc.player.containerMenu.getCarried().isEmpty()) return;
        if (++ticks <= delay.getValue()) return;
        ticks = 0;

        for (int i = 0; i < 36; i++) {
            if (!mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) continue;
            // inventory menu slots: hotbar is 36-44, main inventory 9-35
            int slot = i < 9 ? 36 + i : i;
            InventoryUtil.click(slot, OFFHAND_BUTTON, ContainerInput.SWAP);
            return;
        }
    }

    @Override
    public String getDisplayInfo() {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) count += mc.player.getInventory().getItem(i).getCount();
        }
        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) count++;
        return String.valueOf(count);
    }
}
