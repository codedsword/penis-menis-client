package dev.sinepsinem.manager;

import dev.sinepsinem.event.impl.entity.DeathEvent;
import dev.sinepsinem.event.impl.entity.player.AttackEvent;
import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.Feature;
import dev.sinepsinem.features.modules.combat.KillAuraModule;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Play session stats for the hud: time since joining, kills and deaths, and who we are fighting.
 */
public class SessionManager extends Feature {
    // a kill counts when the player dies this soon after we last hit them
    private static final long KILL_WINDOW = 5000;
    private static final long TARGET_TIMEOUT = 6000;

    private @Nullable ClientPacketListener connection;
    private long start = System.currentTimeMillis();
    private int kills, deaths;

    private @Nullable LivingEntity lastAttacked;
    private long lastAttackTime;

    public void init() {
        EVENT_BUS.register(this);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        // every join gets a new connection, a dimension change keeps it
        if (mc.getConnection() != connection) {
            connection = mc.getConnection();
            reset();
        }
    }

    @Subscribe
    public void onAttack(AttackEvent.Post event) {
        if (event.getTarget() instanceof LivingEntity living) {
            lastAttacked = living;
            lastAttackTime = System.currentTimeMillis();
        }
    }

    @Subscribe
    public void onDeath(DeathEvent event) {
        if (event.getEntity() == mc.player) {
            deaths++;
        } else if (event.getEntity() == lastAttacked && System.currentTimeMillis() - lastAttackTime < KILL_WINDOW) {
            kills++;
            lastAttacked = null;
        }
    }

    public void reset() {
        start = System.currentTimeMillis();
        kills = deaths = 0;
        lastAttacked = null;
    }

    public long playTimeMillis() {
        return System.currentTimeMillis() - start;
    }

    public int kills() {
        return kills;
    }

    public int deaths() {
        return deaths;
    }

    /**
     * @return the kill aura target, otherwise whoever we hit last if that was recent and they are close
     */
    public @Nullable LivingEntity target() {
        LivingEntity aura = KillAuraModule.getTarget();
        if (aura != null) return aura;
        LivingEntity last = lastAttacked;
        if (last == null || !last.isAlive() || last.isRemoved() || mc.player == null) return null;
        if (System.currentTimeMillis() - lastAttackTime > TARGET_TIMEOUT || last.distanceTo(mc.player) > 12) return null;
        return last;
    }

    public static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600, minutes = seconds / 60 % 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds % 60 + "s";
        return seconds % 60 + "s";
    }

    public static boolean isSelf(Entity entity) {
        return entity == mc.player;
    }
}
