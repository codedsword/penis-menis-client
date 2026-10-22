package dev.sinepsinem.util.inventory.strategy;

import dev.sinepsinem.util.inventory.InventoryUtil;
import dev.sinepsinem.util.inventory.Result;
import dev.sinepsinem.util.inventory.ResultType;
import net.minecraft.world.inventory.ContainerInput;

public final class InventoryStrategy implements SwapStrategy {
    public static final InventoryStrategy INSTANCE = new InventoryStrategy();

    private InventoryStrategy() {
    }

    @Override
    public boolean swap(Result result) {
        if (result.type() != ResultType.INVENTORY && result.type() != ResultType.HOTBAR)
            return false;
        int slot = inventorySlot(result);
        InventoryUtil.click(slot, InventoryUtil.selected(), ContainerInput.SWAP);
        return true;
    }

    @Override
    public boolean swapBack(int last, Result result) {
        return swap(result);
    }

    private static int inventorySlot(Result result) {
        return result.type() == ResultType.HOTBAR ? result.slot() + 36 : result.slot();
    }
}
