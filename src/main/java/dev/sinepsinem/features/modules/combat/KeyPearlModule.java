package dev.sinepsinem.features.modules.combat;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.util.TickScheduler;
import dev.sinepsinem.util.inventory.InventoryUtil;
import dev.sinepsinem.util.inventory.Result;
import net.minecraft.world.item.Items;

import static dev.sinepsinem.util.inventory.InventoryUtil.HOTBAR_SCOPE;

public class KeyPearlModule extends Module {

    public KeyPearlModule() {
        super("KeyPearl", "Throws a pearl when enabled.", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        disable();

        if (nullCheck()) return;

        // toggling happens from a key callback between ticks, throw on the next tick like vanilla would
        TickScheduler.nextTick(() -> {
            Result result = InventoryUtil.find(Items.ENDER_PEARL, HOTBAR_SCOPE);
            // thrown where the server thinks we look, a silent rotation may still be easing back
            if (result.found()) InventoryUtil.withSwap(result, () -> SinepSinem.rotationManager.withRotation(
                    SinepSinem.rotationManager.getServerAngles(), () -> mc.gameMode.useItem(mc.player, result.hand())));
        });
    }
}
