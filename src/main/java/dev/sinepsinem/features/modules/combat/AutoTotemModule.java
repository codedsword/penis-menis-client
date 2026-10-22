package dev.sinepsinem.features.modules.combat;

import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.TickScheduler;
import dev.sinepsinem.util.inventory.InventoryUtil;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

/**
 * Keeps a totem of undying in the offhand, only with actions a player can do:
 * <ul>
 *     <li>A hotbar totem is selected and swapped with the offhand key, the old slot is reselected
 *     next tick.</li>
 *     <li>Totems in the main inventory are only moved while the inventory is open, clicking items
 *     in a closed inventory is flagged.</li>
 * </ul>
 */
public class AutoTotemModule extends Module {
    private static final int OFFHAND_BUTTON = 40;

    private final Setting<Integer> delay = num("Delay", 1, 0, 10);
    private final Setting<Boolean> hotbar = bool("Hotbar", true);

    private int ticks;
    // waiting for the server to confirm the last swap
    private int pending;

    public AutoTotemModule() {
        super("AutoTotem", "Keeps a totem in your offhand", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (pending > 0) {
            pending--;
            return;
        }
        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        if (!mc.player.containerMenu.getCarried().isEmpty() || mc.player.isUsingItem()) return;
        if (++ticks <= delay.getValue()) return;

        if (mc.gui.screen() instanceof InventoryScreen) {
            for (int i = 0; i < 36; i++) {
                if (!mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) continue;
                // inventory menu slots: hotbar is 36-44, main inventory 9-35
                int slot = i < 9 ? 36 + i : i;
                InventoryUtil.click(slot, OFFHAND_BUTTON, ContainerInput.SWAP);
                done();
                return;
            }
        } else if (mc.gui.screen() == null && hotbar.getValue()) {
            for (int i = 0; i < 9; i++) {
                if (!mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) continue;
                int last = InventoryUtil.selected();
                InventoryUtil.swap(i);
                // the offhand key, like vanilla's handleKeybinds
                mc.player.connection.send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
                if (last != i) TickScheduler.nextTick(() -> InventoryUtil.swap(last));
                done();
                return;
            }
        }
    }

    private void done() {
        ticks = 0;
        pending = 3;
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
