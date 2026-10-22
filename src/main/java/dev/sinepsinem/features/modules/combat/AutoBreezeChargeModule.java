package dev.sinepsinem.features.modules.combat;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.inventory.InventoryUtil;
import dev.sinepsinem.util.inventory.Result;
import dev.sinepsinem.util.models.Angles;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static dev.sinepsinem.util.inventory.InventoryUtil.FULL_SCOPE;
import static dev.sinepsinem.util.inventory.InventoryUtil.HOTBAR_SCOPE;

/**
 * Throws breeze (wind) charges at your feet: on jump for a boosted launch (mace setups), or right
 * before landing to cancel fall damage. Aiming down is done with a silent rotation.
 */
public class AutoBreezeChargeModule extends Module {
    private final Setting<Mode> mode = mode("Mode", Mode.BOTH);
    private final Setting<Float> clutchFall = num("ClutchFall", 4.0f, 2.0f, 30.0f);
    private final Setting<Integer> clutchTicks = num("ClutchTicks", 3, 1, 8);
    private final Setting<Boolean> inventory = bool("Inventory", false);

    private boolean wasJumping;
    private boolean clutched;

    public AutoBreezeChargeModule() {
        super("AutoBreezeCharge", "Wind charge jumps and fall clutches", Category.COMBAT);
        clutchFall.setVisibility(v -> mode.getValue() != Mode.BOOST);
        clutchTicks.setVisibility(v -> mode.getValue() != Mode.BOOST);
    }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (nullCheck() || mc.gui.screen() != null) return;

        boolean jumping = mc.options.keyJump.isDown();
        boolean boost = mode.getValue() != Mode.CLUTCH && jumping && !wasJumping && mc.player.onGround();
        wasJumping = jumping;

        if (mc.player.onGround()) clutched = false;

        // the burst has to reach us before we land: fall damage then only counts from the burst's height
        double fallSpeed = -mc.player.getDeltaMovement().y;
        boolean clutch = mode.getValue() != Mode.BOOST
                && !clutched
                && !mc.player.onGround()
                && mc.player.fallDistance >= clutchFall.getValue()
                && fallSpeed > 0.3
                && distanceToGround() / fallSpeed <= clutchTicks.getValue();

        if (clutch) clutched = true;
        if (boost || clutch) throwDown();
    }

    private void throwDown() {
        Result result = InventoryUtil.find(Items.WIND_CHARGE, inventory.getValue() ? FULL_SCOPE : HOTBAR_SCOPE);
        if (!result.found() || mc.player.getCooldowns().isOnCooldown(result.stack())) return;

        Angles down = new Angles(mc.player.getYRot(), 90f);
        SinepSinem.rotationManager.rotate(down, 50, () -> InventoryUtil.withSwap(result, () ->
                SinepSinem.rotationManager.withRotation(down, () -> mc.gameMode.useItem(mc.player, result.hand()))));
    }

    private double distanceToGround() {
        Vec3 from = mc.player.position();
        Vec3 to = from.subtract(0, 24, 0);
        BlockHitResult hit = mc.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, mc.player));
        return hit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : from.y - hit.getLocation().y;
    }

    @Override
    public String getDisplayInfo() {
        return dev.sinepsinem.features.settings.EnumConverter.getProperName(mode.getValue());
    }

    public enum Mode {
        BOOST,
        CLUTCH,
        BOTH
    }
}
