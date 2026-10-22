package dev.sinepsinem.features.modules.combat;

import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.inventory.InventoryUtil;
import dev.sinepsinem.util.inventory.Result;
import net.minecraft.world.item.Items;

import static dev.sinepsinem.util.inventory.InventoryUtil.FULL_SCOPE;
import static dev.sinepsinem.util.inventory.InventoryUtil.HOTBAR_SCOPE;

public class KeyPearlModule extends Module {
    private final Setting<Boolean> inventory = bool("Inventory", false);

    public KeyPearlModule() {
        super("KeyPearl", "Throws a pearl when enabled.", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        disable();

        if (nullCheck()) return;

        Result result = InventoryUtil.find(Items.ENDER_PEARL, inventory.getValue() ? FULL_SCOPE : HOTBAR_SCOPE);
        InventoryUtil.withSwap(result, () -> mc.gameMode.useItem(mc.player, result.hand()));
    }
}
