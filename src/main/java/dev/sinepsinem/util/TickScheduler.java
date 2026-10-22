package dev.sinepsinem.util;

import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.util.traits.Util;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Runs actions at the start of the next player tick, before that tick's movement packet, which is
 * where vanilla sends attacks, item uses and slot changes. Packets sent after the movement packet,
 * or from input callbacks between ticks, are flagged by post checks.
 */
public final class TickScheduler implements Util {
    private static final Queue<Runnable> NEXT_TICK = new ArrayDeque<>();

    static {
        EVENT_BUS.register(new TickScheduler());
    }

    private TickScheduler() {
    }

    public static void init() {
        // forces the static registration
    }

    public static void nextTick(Runnable action) {
        NEXT_TICK.add(action);
    }

    // runs before every module's TickEvent.Pre handler
    @Subscribe(priority = 2000)
    public void onTickPre(TickEvent.Pre event) {
        int pending = NEXT_TICK.size();
        for (int i = 0; i < pending; i++) {
            Runnable action = NEXT_TICK.poll();
            if (action != null && mc.player != null) action.run();
        }
    }
}
