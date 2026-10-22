package dev.sinepsinem.features.modules.combat;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.MathUtil;
import dev.sinepsinem.util.RotationUtil;
import dev.sinepsinem.util.TargetUtil;
import dev.sinepsinem.util.models.Angles;
import dev.sinepsinem.util.models.Timer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
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
        // vanilla cannot attack while eating, blocking or drawing a bow
        if (mc.player.isUsingItem()) return;

        double reach = Math.min(range.getValue(), mc.player.entityInteractionRange());
        List<LivingEntity> targets = TargetUtil.targets(Math.max(reach, scanRange.getValue()),
                players.getValue(), hostiles.getValue(), passives.getValue(), priority.getValue());
        if (targets.isEmpty()) return;
        target = targets.getFirst();

        // We are at the start of the tick, before this tick's movement packet, like vanilla's attack
        // key handling. The server checks the hit against the rotation it already has, so that is
        // what has to look at the target, the new rotation below is for the next hit.
        Angles server = SinepSinem.rotationManager.getServerAngles();
        if (canAttack() && distanceTo(target) <= reach
                && (!rayTrace.getValue() || RotationUtil.isLookingAt(server, target, reach))) {
            attack(target);
        }

        if (!rotate.getValue()) return;
        Angles wanted = RotationUtil.toEntity(target, server);
        Angles next = RotationUtil.smooth(server, wanted,
                yawSpeed.getValue() * MathUtil.getRandom(0.85f, 1.0f),
                pitchSpeed.getValue() * MathUtil.getRandom(0.85f, 1.0f));
        SinepSinem.rotationManager.rotate(next, 10);
    }

    private boolean canAttack() {
        boolean ready = switch (timing.getValue()) {
            case COOLDOWN -> mc.player.getAttackStrengthScale(0.5f) >= cooldown.getValue();
            case CPS -> cpsTimer.passedMs(nextDelay);
        };
        return ready && CriticalsModule.allowsAttack();
    }

    private double distanceTo(LivingEntity entity) {
        Vec3 eyes = mc.player.getEyePosition();
        return eyes.distanceTo(MathUtil.clamp(eyes, entity.getBoundingBox()));
    }

    private void attack(LivingEntity entity) {
        if (!entity.isAlive() || entity.isRemoved()) return;
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
