package dev.sinepsinem.features.modules.player;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.inventory.InventoryUtil;
import dev.sinepsinem.util.inventory.Result;
import dev.sinepsinem.util.models.Angles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static dev.sinepsinem.util.inventory.InventoryUtil.HOTBAR_SCOPE;

/**
 * Water bucket clutch. Spoofing onGround is caught by every simulation anticheat, so this places
 * water below you right before landing (aiming down with a silent rotation one tick earlier) and
 * picks it back up once you are standing in it.
 */
public class NoFallModule extends Module {
    private final Setting<Float> minFall = num("MinFall", 4.0f, 3.0f, 30.0f);
    private final Setting<Boolean> pickUp = bool("PickUp", true);

    private State state = State.IDLE;
    private BlockPos placed;
    private int timer;

    public NoFallModule() {
        super("NoFall", "Water bucket clutch", Category.PLAYER);
    }

    @Override
    public void onDisable() {
        state = State.IDLE;
        placed = null;
    }

    @Override
    public void onTick() {
        if (mc.gui.screen() != null) return;

        switch (state) {
            case IDLE -> {
                if (shouldClutch() && find(Items.WATER_BUCKET).found()) {
                    aimDown();
                    state = State.PLACE;
                }
            }
            case PLACE -> {
                state = State.IDLE;
                BlockHitResult hit = groundHit(mc.player.blockInteractionRange());
                Result bucket = find(Items.WATER_BUCKET);
                if (hit == null || !bucket.found() || mc.player.onGround()) return;
                use(bucket);
                placed = hit.getBlockPos().relative(hit.getDirection());
                timer = 20;
                if (pickUp.getValue()) state = State.LANDING;
            }
            case LANDING -> {
                if (--timer < 0 || placed == null) {
                    state = State.IDLE;
                    return;
                }
                boolean standingIn = mc.player.blockPosition().equals(placed)
                        && mc.level.getFluidState(placed).is(Fluids.WATER) && mc.level.getFluidState(placed).isSource();
                if ((mc.player.onGround() || mc.player.isInWater()) && standingIn && find(Items.BUCKET).found()) {
                    aimDown();
                    state = State.PICK_UP;
                }
            }
            case PICK_UP -> {
                state = State.IDLE;
                Result bucket = find(Items.BUCKET);
                if (bucket.found() && mc.level.getFluidState(placed).isSource()) use(bucket);
                placed = null;
            }
        }
    }

    private boolean shouldClutch() {
        if (mc.player.onGround() || mc.player.isInWater() || mc.player.onClimbable() || mc.player.isFallFlying()) return false;
        if (mc.player.fallDistance < minFall.getValue()) return false;
        double fallSpeed = -mc.player.getDeltaMovement().y;
        if (fallSpeed <= 0.1) return false;
        // aim this tick, place next tick: the ground has to be in reach then but not reached yet
        double next = fallSpeed + 0.08;
        BlockHitResult hit = groundHit(mc.player.blockInteractionRange() + next);
        if (hit == null) return false;
        double distance = mc.player.getY() - hit.getLocation().y;
        return distance > next && distance <= next * 3;
    }

    private void aimDown() {
        SinepSinem.rotationManager.rotate(new Angles(SinepSinem.rotationManager.getServerAngles().yRot(), 90f), 45);
    }

    // the server simulates the use with the rotation it already has, which is the one aimed last tick
    private void use(Result result) {
        Angles server = SinepSinem.rotationManager.getServerAngles();
        SinepSinem.rotationManager.rotate(server, 45);
        InventoryUtil.withSwap(result, () ->
                SinepSinem.rotationManager.withRotation(server, () -> mc.gameMode.useItem(mc.player, result.hand())));
    }

    private BlockHitResult groundHit(double range) {
        Vec3 from = mc.player.getEyePosition();
        Vec3 to = from.subtract(0, range, 0);
        BlockHitResult hit = mc.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private Result find(net.minecraft.world.item.Item item) {
        Result result = InventoryUtil.find(item, HOTBAR_SCOPE);
        return result.found() && !mc.player.getCooldowns().isOnCooldown(result.stack()) ? result : InventoryUtil.NONE;
    }

    @Override
    public String getDisplayInfo() {
        return "Bucket";
    }

    private enum State {
        IDLE, PLACE, LANDING, PICK_UP
    }
}
