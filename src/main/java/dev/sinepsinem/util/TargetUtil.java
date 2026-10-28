package dev.sinepsinem.util;

import dev.sinepsinem.features.modules.misc.AntiBotModule;
import dev.sinepsinem.features.modules.misc.TeamsModule;
import dev.sinepsinem.util.traits.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Single place deciding which entities may be attacked or highlighted.
 */
public final class TargetUtil implements Util {
    private TargetUtil() {
    }

    public enum Priority {
        DISTANCE, HEALTH, ANGLE
    }

    public static boolean isValidPlayer(Player player) {
        return player != mc.player
                && player.isAlive()
                && !player.isRemoved()
                && !player.isSpectator()
                && !AntiBotModule.isBot(player);
    }

    public static boolean isValidTarget(Entity entity, boolean players, boolean hostiles, boolean passives) {
        if (!(entity instanceof LivingEntity living) || entity == mc.player || !living.isAlive() || entity.isRemoved()) {
            return false;
        }
        if (entity instanceof Player player) return players && isValidPlayer(player);
        if (entity instanceof net.minecraft.world.entity.monster.Enemy) return hostiles;
        return passives && !(entity instanceof net.minecraft.world.entity.decoration.ArmorStand);
    }

    public static List<LivingEntity> targets(double range, boolean players, boolean hostiles, boolean passives, Priority priority) {
        double rangeSq = range * range;
        return StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(e -> isValidTarget(e, players, hostiles, passives))
                .filter(e -> !(e instanceof Player player && TeamsModule.isTeammate(player)))
                .filter(e -> mc.player.getEyePosition().distanceToSqr(MathUtil.clamp(mc.player.getEyePosition(), e.getBoundingBox())) <= rangeSq)
                .map(e -> (LivingEntity) e)
                .sorted(comparator(priority))
                .toList();
    }

    private static Comparator<LivingEntity> comparator(Priority priority) {
        return switch (priority) {
            case DISTANCE -> Comparator.comparingDouble(e -> e.distanceToSqr(mc.player));
            case HEALTH -> Comparator.comparingDouble(e -> e.getHealth() + e.getAbsorptionAmount());
            case ANGLE -> Comparator.comparingDouble(e -> {
                float[] angles = MathUtil.calcAngle(mc.player.getEyePosition(), e.getBoundingBox().getCenter());
                return Math.abs(MathUtil.wrapDegrees(angles[0] - mc.player.getYRot()));
            });
        };
    }
}
