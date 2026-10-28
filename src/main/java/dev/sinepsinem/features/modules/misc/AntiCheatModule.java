package dev.sinepsinem.features.modules.misc;

import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.TargetUtil;
import dev.sinepsinem.util.chat.ChatUtil;
import dev.sinepsinem.util.chat.SimpleSignature;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A small anticheat for other players, judged from what the server sends us about them:
 * <ul>
 *     <li>Speed: moving faster than sprint jumping allows, for several ticks</li>
 *     <li>Flight: hovering in the air for a long time</li>
 *     <li>Invalid Rotation: looking further up or down than vanilla lets you</li>
 *     <li>Invalid Movement: rising more than a jump or a step in one tick</li>
 * </ul>
 * Alerts look like Ambient's, "[AC] Name flagged Check [x2]", and stack per player and check.
 */
public class AntiCheatModule extends Module {
    private enum Check {
        SPEED("Speed", 4), FLIGHT("Flight", 10), ROTATION("Invalid Rotation", 1), MOVEMENT("Invalid Movement", 2);

        final String name;
        final int threshold;

        Check(String name, int threshold) {
            this.name = name;
            this.threshold = threshold;
        }
    }

    private final Setting<Boolean> speed = bool("Speed", true);
    private final Setting<Boolean> flight = bool("Flight", true);
    private final Setting<Boolean> rotation = bool("Rotation", true);
    private final Setting<Boolean> movement = bool("Movement", true);
    private final Setting<Integer> cooldown = num("Cooldown", 3, 1, 20);

    private final Map<UUID, Data> data = new HashMap<>();

    private static final class Data {
        Vec3 last;
        int airTicks, hoverTicks;
        boolean wasOnGround = true;
        final Map<Check, Integer> violations = new EnumMap<>(Check.class);
        final Map<Check, Integer> flags = new EnumMap<>(Check.class);
        final Map<Check, Long> lastAlert = new EnumMap<>(Check.class);
    }

    public AntiCheatModule() {
        super("AntiCheat", "Flags other players that cheat", Category.MISC);
    }

    @Override
    public void onDisable() {
        data.clear();
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        data.keySet().removeIf(id -> mc.level.getPlayerByUUID(id) == null);
        for (Player player : mc.level.players()) {
            if (!TargetUtil.isValidPlayer(player) || player.isCreative() || player.isPassenger()) continue;
            check(player, data.computeIfAbsent(player.getUUID(), id -> new Data()));
        }
    }

    private void check(Player player, Data d) {
        Vec3 pos = player.position();
        Vec3 last = d.last;
        d.last = pos;
        if (last == null) return;
        Vec3 delta = pos.subtract(last);
        // a teleport, not movement
        if (delta.lengthSqr() > 64) return;

        boolean exempt = player.isFallFlying() || player.isAutoSpinAttack() || player.isInLiquid() || player.onClimbable()
                || player.hurtTime > 0 || player.hasEffect(MobEffects.LEVITATION) || player.hasEffect(MobEffects.SLOW_FALLING)
                || player.hasEffect(MobEffects.JUMP_BOOST);

        if (speed.getValue() && !exempt) {
            double allowed = 0.72;
            if (player.hasEffect(MobEffects.SPEED)) allowed *= 1 + 0.2 * (player.getEffect(MobEffects.SPEED).getAmplifier() + 1);
            violate(player, d, Check.SPEED, delta.horizontalDistance() > allowed);
        }

        if (player.onGround()) {
            d.airTicks = 0;
            d.hoverTicks = 0;
        } else {
            d.airTicks++;
            d.hoverTicks = Math.abs(delta.y) < 0.02 ? d.hoverTicks + 1 : 0;
        }
        if (flight.getValue() && !exempt) violate(player, d, Check.FLIGHT, d.airTicks > 40 && d.hoverTicks > 10);

        if (rotation.getValue()) violate(player, d, Check.ROTATION, Math.abs(player.getXRot()) > 90.01f);

        if (movement.getValue() && !exempt) {
            // a jump rises 0.42 in its first tick, a step 0.6
            violate(player, d, Check.MOVEMENT, delta.y > 1.0 || (d.wasOnGround && player.onGround() && delta.y > 0.65));
        }
        d.wasOnGround = player.onGround();
    }

    private void violate(Player player, Data d, Check check, boolean failed) {
        int violations = failed ? d.violations.getOrDefault(check, 0) + 1 : Math.max(0, d.violations.getOrDefault(check, 0) - 1);
        d.violations.put(check, violations);
        if (violations < check.threshold) return;
        d.violations.put(check, 0);
        long now = System.currentTimeMillis();
        if (now - d.lastAlert.getOrDefault(check, 0L) < cooldown.getValue() * 1000L) return;
        d.lastAlert.put(check, now);
        int count = d.flags.merge(check, 1, Integer::sum);

        MutableComponent message = Component.literal("[AC] ").withStyle(ChatFormatting.RED)
                .append(Component.literal(player.getGameProfile().name()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" flagged ").withStyle(ChatFormatting.RED))
                .append(Component.literal(check.name).withStyle(ChatFormatting.YELLOW));
        if (count > 1) message.append(Component.literal(" [x" + count + "]").withStyle(ChatFormatting.GRAY));
        // one line per player and check, a new flag replaces the old line
        ChatUtil.sendMessage(message, SimpleSignature.from("anticheat/" + player.getUUID() + "/" + check));
    }
}
