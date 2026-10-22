package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.entity.player.TickEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.MathUtil;
import me.alpha432.oyvey.util.RotationUtil;
import me.alpha432.oyvey.util.TargetUtil;
import me.alpha432.oyvey.util.models.Angles;
import me.alpha432.oyvey.util.models.Timer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class KillAuraModule extends Module {
    private static KillAuraModule INSTANCE;

    private final Setting<Float> range = num("Range", 3.0f, 1f, 6f);
    private final Setting<Float> scanRange = num("ScanRange", 4.5f, 1f, 8f);
    private final Setting<TargetUtil.Priority> priority = mode("Priority", TargetUtil.Priority.DISTANCE);
    private final Setting<Boolean> players = bool("Players", true);
    private final Setting<Boolean> hostiles = bool("Hostiles", false);
    private final Setting<Boolean> passives = bool("Passives", false);

    private final Setting<Timing> timing = mode("Timing", Timing.COOLDOWN);
    private final Setting<Float> cooldown = num("Cooldown", 0.95f, 0.1f, 1f);
    private final Setting<Integer> minCps = num("MinCPS", 9, 1, 20);
    private final Setting<Integer> maxCps = num("MaxCPS", 13, 1, 20);

    private final Setting<Boolean> rotate = bool("Rotate", true);
    private final Setting<Float> yawSpeed = num("YawSpeed", 80f, 5f, 180f);
    private final Setting<Float> pitchSpeed = num("PitchSpeed", 45f, 5f, 180f);
    private final Setting<Boolean> rayTrace = bool("RayTrace", true);

    private final Setting<Boolean> onlyWeapon = bool("OnlyWeapon", false);
    private final Setting<Boolean> pauseOnUse = bool("PauseOnUse", true);

    private final Timer cpsTimer = new Timer();
    private long nextDelay;
    private @Nullable LivingEntity target;

    public KillAuraModule() {
        super("KillAura", "Attacks entities around you", Category.COMBAT);
        cooldown.setVisibility(v -> timing.getValue() == Timing.COOLDOWN);
        minCps.setVisibility(v -> timing.getValue() == Timing.CPS);
        maxCps.setVisibility(v -> timing.getValue() == Timing.CPS);
        yawSpeed.setVisibility(v -> rotate.getValue());
        pitchSpeed.setVisibility(v -> rotate.getValue());
        rayTrace.setVisibility(v -> rotate.getValue());
        INSTANCE = this;
    }

    public static @Nullable LivingEntity getTarget() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE.target : null;
    }

    @Override
    public void onDisable() {
        target = null;
    }

    @Subscribe
    public void onPreTick(TickEvent.Pre event) {
        if (nullCheck()) return;
        target = null;

        if (onlyWeapon.getValue() && !isHoldingWeapon()) return;
        if (pauseOnUse.getValue() && mc.player.isUsingItem()) return;

        List<LivingEntity> targets = TargetUtil.targets(Math.max(range.getValue(), scanRange.getValue()),
                players.getValue(), hostiles.getValue(), passives.getValue(), priority.getValue());
        if (targets.isEmpty()) return;
        target = targets.getFirst();

        if (!rotate.getValue()) {
            if (canAttack()) attack(target);
            return;
        }

        Angles current = OyVey.rotationManager.getServerAngles();
        Angles wanted = RotationUtil.toEntity(target, current);
        Angles next = RotationUtil.smooth(current, wanted,
                yawSpeed.getValue() * MathUtil.getRandom(0.85f, 1.0f),
                pitchSpeed.getValue() * MathUtil.getRandom(0.85f, 1.0f));

        LivingEntity locked = target;
        OyVey.rotationManager.rotate(next, 10, () -> {
            // runs right after the rotation was sent, so the server validates the hit against it
            if (!canAttack()) return;
            if (rayTrace.getValue() && !RotationUtil.isLookingAt(next, locked, range.getValue())) return;
            attack(locked);
        });
    }

    private boolean canAttack() {
        return switch (timing.getValue()) {
            case COOLDOWN -> mc.player.getAttackStrengthScale(0.5f) >= cooldown.getValue();
            case CPS -> cpsTimer.passedMs(nextDelay);
        };
    }

    private void attack(LivingEntity entity) {
        if (nullCheck() || !entity.isAlive() || entity.isRemoved()) return;
        double reach = mc.player.getEyePosition().distanceTo(MathUtil.clamp(mc.player.getEyePosition(), entity.getBoundingBox()));
        if (reach > range.getValue()) return;

        mc.gameMode.attack(mc.player, entity);
        mc.player.swing(InteractionHand.MAIN_HAND);

        cpsTimer.reset();
        int min = Math.min(minCps.getValue(), maxCps.getValue());
        int max = Math.max(minCps.getValue(), maxCps.getValue());
        nextDelay = (long) (1000.0 / MathUtil.getRandom(min, max));
    }

    private boolean isHoldingWeapon() {
        ItemStack stack = mc.player.getMainHandItem();
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.SPEARS)
                || stack.is(Items.MACE) || stack.is(Items.TRIDENT);
    }

    @Override
    public String getDisplayInfo() {
        return target != null ? target.getName().getString() : null;
    }

    public enum Timing {
        COOLDOWN,
        CPS
    }
}
