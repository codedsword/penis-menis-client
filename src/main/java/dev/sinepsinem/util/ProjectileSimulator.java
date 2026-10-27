package dev.sinepsinem.util;

import dev.sinepsinem.util.traits.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Steps projectiles the way their tick methods do (26.2), to find where and when they land.
 * The order of gravity, drag and movement differs per projectile family and matters for the result.
 */
public final class ProjectileSimulator implements Util {
    private static final int MAX_TICKS = 400;

    private ProjectileSimulator() {
    }

    public enum Family {
        // AbstractArrow: move, drag (unless in water, where drag comes before the move), gravity
        ARROW,
        // ThrowableProjectile: gravity, drag, move
        THROWABLE,
        // AbstractHurtingProjectile: accelerate and drag, move; no gravity
        HURTING
    }

    public record Physics(Family family, double gravity, float drag, float waterDrag, double acceleration) {
        static final Physics ARROW = new Physics(Family.ARROW, 0.05, 0.99f, 0.6f, 0);
        static final Physics TRIDENT = new Physics(Family.ARROW, 0.05, 0.99f, 0.99f, 0);
        static final Physics THROWABLE = new Physics(Family.THROWABLE, 0.03, 0.99f, 0.8f, 0);
        static final Physics POTION = new Physics(Family.THROWABLE, 0.05, 0.99f, 0.8f, 0);
        static final Physics EXPERIENCE_BOTTLE = new Physics(Family.THROWABLE, 0.07, 0.99f, 0.8f, 0);
        static final Physics WIND_CHARGE = new Physics(Family.HURTING, 0, 1.0f, 0.8f, 0);
    }

    /**
     * @param points positions at every tick, starting with the spawn position
     * @param ticks  ticks until the projectile hits something (or the simulation gave up)
     */
    public record Result(List<Vec3> points, @Nullable HitResult hit, int ticks) {
        public Vec3 end() {
            return hit != null ? hit.getLocation() : points.getLast();
        }

        public boolean landed() {
            return hit != null;
        }
    }

    /**
     * What the held item would shoot right now, as start position and velocity, or null if it
     * does not shoot anything (or a bow is not being drawn).
     */
    public static @Nullable Shot aim(Player player, float partialTicks) {
        for (ItemStack stack : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            Shot shot = aim(player, stack, partialTicks);
            if (shot != null) return shot;
        }
        return null;
    }

    private static @Nullable Shot aim(Player player, ItemStack stack, float partialTicks) {
        Physics physics;
        float power, pitchOffset = 0;
        boolean inheritMotion = true;
        switch (stack.getItem()) {
            case BowItem ignored -> {
                if (!player.isUsingItem() || player.getUseItem() != stack) return null;
                power = BowItem.getPowerForTime(player.getTicksUsingItem()) * 3.0f;
                if (power < 0.3f) return null;
                physics = Physics.ARROW;
            }
            case CrossbowItem ignored -> {
                if (!CrossbowItem.isCharged(stack)) return null;
                power = 3.15f;
                physics = Physics.ARROW;
                // crossbows shoot along the view vector without adding the shooter's motion
                inheritMotion = false;
            }
            case TridentItem ignored -> {
                if (!player.isUsingItem() || player.getUseItem() != stack) return null;
                power = TridentItem.PROJECTILE_SHOOT_POWER;
                physics = Physics.TRIDENT;
            }
            case SnowballItem ignored -> {
                power = SnowballItem.PROJECTILE_SHOOT_POWER;
                physics = Physics.THROWABLE;
            }
            case EggItem ignored -> {
                power = EggItem.PROJECTILE_SHOOT_POWER;
                physics = Physics.THROWABLE;
            }
            case EnderpearlItem ignored -> {
                power = EnderpearlItem.PROJECTILE_SHOOT_POWER;
                physics = Physics.THROWABLE;
            }
            case ThrowablePotionItem ignored -> {
                power = ThrowablePotionItem.PROJECTILE_SHOOT_POWER;
                pitchOffset = -20.0f;
                physics = Physics.POTION;
            }
            case ExperienceBottleItem ignored -> {
                power = 0.7f;
                pitchOffset = -20.0f;
                physics = Physics.EXPERIENCE_BOTTLE;
            }
            case WindChargeItem ignored -> {
                power = WindChargeItem.PROJECTILE_SHOOT_POWER;
                physics = Physics.WIND_CHARGE;
            }
            default -> {
                return null;
            }
        }

        float yRot = player.getViewYRot(partialTicks), xRot = player.getViewXRot(partialTicks);
        float rad = Mth.DEG_TO_RAD;
        Vec3 direction = new Vec3(
                -Mth.sin(yRot * rad) * Mth.cos(xRot * rad),
                -Mth.sin((xRot + pitchOffset) * rad),
                Mth.cos(yRot * rad) * Mth.cos(xRot * rad)).normalize();
        Vec3 velocity = direction.scale(power);
        if (inheritMotion) {
            Vec3 motion = player.getKnownMovement();
            velocity = velocity.add(motion.x, player.onGround() ? 0 : motion.y, motion.z);
        }

        Vec3 position = player.getPosition(partialTicks);
        double y = physics.family == Family.HURTING ? player.getEyePosition(partialTicks).y : position.y + player.getEyeHeight() - 0.1f;
        return new Shot(new Vec3(position.x, y, position.z), velocity, physics, stack);
    }

    public record Shot(Vec3 position, Vec3 velocity, Physics physics, ItemStack stack) {
    }

    /**
     * @return the physics of an entity we know how to predict, null for anything else
     */
    public static @Nullable Physics physicsOf(Entity entity) {
        return switch (entity) {
            case ThrownTrident ignored -> Physics.TRIDENT;
            case AbstractArrow ignored -> Physics.ARROW;
            case ThrownExperienceBottle ignored -> Physics.EXPERIENCE_BOTTLE;
            case AbstractThrownPotion ignored -> Physics.POTION;
            case ThrowableProjectile ignored -> Physics.THROWABLE;
            case AbstractWindCharge ignored -> Physics.WIND_CHARGE;
            case AbstractHurtingProjectile hurting -> new Physics(Family.HURTING, 0, 0.95f, 0.8f, hurting.accelerationPower);
            default -> null;
        };
    }

    /**
     * Simulates a projectile from {@code position} with {@code velocity}.
     *
     * @param source never counted as a hit (the shooter, or the projectile itself)
     * @param owner  also never counted as a hit, may be null
     */
    public static Result simulate(Vec3 position, Vec3 velocity, Physics physics, Entity source, @Nullable Entity owner) {
        List<Vec3> points = new ArrayList<>();
        points.add(position);
        Predicate<Entity> canHit = e -> e != owner && e.isPickable() && !e.isSpectator() && e.isAlive();

        Vec3 pos = position, vel = velocity;
        for (int tick = 1; tick <= MAX_TICKS; tick++) {
            boolean water = inWater(pos);
            switch (physics.family) {
                case THROWABLE -> vel = vel.subtract(0, physics.gravity, 0).scale(water ? physics.waterDrag : physics.drag);
                case HURTING -> vel = vel.add(vel.normalize().scale(physics.acceleration)).scale(water ? physics.waterDrag : physics.drag);
                case ARROW -> {
                    if (water) vel = vel.scale(physics.waterDrag);
                }
            }

            Vec3 next = pos.add(vel);
            BlockHitResult block = mc.level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
            Vec3 end = block.getType() != HitResult.Type.MISS ? block.getLocation() : next;
            AABB area = new AABB(pos, end).inflate(1.0);
            EntityHitResult entity = ProjectileUtil.getEntityHitResult(mc.level, source, pos, end, area, canHit, 0.3f);
            HitResult hit = entity != null ? entity : block.getType() != HitResult.Type.MISS ? block : null;
            if (hit != null) {
                points.add(hit.getLocation());
                return new Result(points, hit, tick);
            }

            pos = next;
            points.add(pos);
            if (physics.family == Family.ARROW) {
                if (!inWater(pos)) vel = vel.scale(physics.drag);
                vel = vel.subtract(0, physics.gravity, 0);
            }
            if (pos.y < mc.level.getMinY() - 64) break;
        }
        return new Result(points, null, MAX_TICKS);
    }

    public static Result simulate(Projectile projectile, Vec3 position) {
        Physics physics = physicsOf(projectile);
        return simulate(position, projectile.getDeltaMovement(), physics == null ? Physics.THROWABLE : physics, projectile, projectile.getOwner());
    }

    private static boolean inWater(Vec3 pos) {
        return mc.level.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER);
    }
}
