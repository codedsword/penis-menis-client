package dev.sinepsinem.features.modules.combat;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.MathUtil;
import dev.sinepsinem.util.RotationUtil;
import dev.sinepsinem.util.TargetUtil;
import dev.sinepsinem.util.inventory.InventoryUtil;
import dev.sinepsinem.util.models.Angles;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Charges a spear at targets ahead of you. A spear only deals kinetic damage after its charge delay
 * and when you close in faster than its damage threshold, so the charge is started early, while you
 * run at the target, and aimed with silent rotations held for the whole charge. The hit itself is
 * applied by vanilla on contact.
 */
public class AutoSpearModule extends Module {
    private final Setting<Boolean> swap = bool("Swap", true);
    private final Setting<Boolean> rotate = bool("Rotate", true);
    private final Setting<Float> chargeRange = num("ChargeRange", 10.0f, 4.5f, 24.0f);
    private final Setting<Float> speedFactor = num("SpeedFactor", 0.6f, 0.0f, 1.5f);
    private final Setting<Boolean> players = bool("Players", true);
    private final Setting<Boolean> hostiles = bool("Hostiles", false);
    private final Setting<Boolean> passives = bool("Passives", false);

    private @Nullable LivingEntity target;
    private int charging = -1;
    private int previousSlot = -1;

    public AutoSpearModule() {
        super("AutoSpear", "Automatically charges spears at targets", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        stop();
    }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (nullCheck()) return;

        ItemStack held = mc.player.getMainHandItem();
        int spearSlot = held.has(DataComponents.KINETIC_WEAPON) ? mc.player.getInventory().getSelectedSlot() : findSpear();
        if (spearSlot == -1) {
            stop();
            return;
        }
        ItemStack spear = mc.player.getInventory().getItem(spearSlot);
        KineticWeapon kinetic = spear.get(DataComponents.KINETIC_WEAPON);
        if (kinetic == null) {
            stop();
            return;
        }

        List<LivingEntity> targets = TargetUtil.targets(chargeRange.getValue(), players.getValue(),
                hostiles.getValue(), passives.getValue(), TargetUtil.Priority.ANGLE);
        target = targets.isEmpty() ? null : targets.getFirst();

        if (charging >= 0) {
            charging++;
            boolean expired = charging > kinetic.computeDamageUseDuration();
            if (target == null || expired || !mc.player.isUsingItem()) {
                stop();
                return;
            }
            aim(10);
            return;
        }

        // start once we are running at the target fast enough to reach the damage threshold
        float threshold = kinetic.damageConditions().map(KineticWeapon.Condition::minRelativeSpeed).orElse(0f);
        if (target == null || approachSpeed(target) < threshold * speedFactor.getValue()) return;

        if (spearSlot != mc.player.getInventory().getSelectedSlot()) {
            if (!swap.getValue()) return;
            previousSlot = mc.player.getInventory().getSelectedSlot();
            InventoryUtil.swap(spearSlot);
        }

        Angles angles = aim(0);
        SinepSinem.rotationManager.withRotation(angles, () -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
        // vanilla releases item use every frame the use key is not held
        mc.options.keyUse.setDown(true);
        charging = 0;
    }

    private Angles aim(int ticks) {
        Angles current = SinepSinem.rotationManager.getServerAngles();
        Angles wanted = Angles.pos(target.getBoundingBox().getCenter());
        Angles next = rotate.getValue() ? RotationUtil.smooth(current, wanted, 120f, 90f) : wanted;
        if (rotate.getValue()) SinepSinem.rotationManager.rotate(next, 20 + ticks);
        return next;
    }

    // blocks per second we close in on the target along the line between us
    private double approachSpeed(LivingEntity entity) {
        Vec3 direction = entity.getBoundingBox().getCenter().subtract(mc.player.getEyePosition()).normalize();
        Vec3 relative = KineticWeapon.getMotion(mc.player).subtract(KineticWeapon.getMotion(entity));
        return relative.dot(direction);
    }

    private void stop() {
        if (charging >= 0) {
            // give the key back to the physical mouse button
            mc.options.keyUse.setDown(isPhysicallyDown(mc.options.keyUse));
            if (mc.player != null && mc.player.isUsingItem()) {
                mc.gameMode.releaseUsingItem(mc.player);
            }
        }
        charging = -1;
        if (previousSlot != -1 && mc.player != null) {
            InventoryUtil.swap(previousSlot);
        }
        previousSlot = -1;
    }

    private static boolean isPhysicallyDown(KeyMapping key) {
        InputConstants.Key bound = InputConstants.getKey(key.saveString());
        long window = mc.getWindow().handle();
        return bound.getType() == InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(window, bound.getValue()) == GLFW.GLFW_PRESS
                : InputConstants.isKeyDown(mc.getWindow(), bound.getValue());
    }

    private int findSpear() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).has(DataComponents.KINETIC_WEAPON)) return i;
        }
        return -1;
    }

    @Override
    public String getDisplayInfo() {
        return target != null ? String.format("%.1f", MathUtil.round(approachSpeed(target), 1)) : null;
    }
}
