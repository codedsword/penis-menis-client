package me.alpha432.oyvey.event.impl.entity.player;

import me.alpha432.oyvey.event.Event;
import net.minecraft.world.entity.Entity;

/**
 * Posted around {@code MultiPlayerGameMode#attack}. Cancelling {@link Pre} cancels the attack.
 */
public class AttackEvent extends Event {
    private final Entity target;

    private AttackEvent(Entity target) {
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }

    public static class Pre extends AttackEvent {
        public Pre(Entity target) {
            super(target);
        }
    }

    public static class Post extends AttackEvent {
        public Post(Entity target) {
            super(target);
        }
    }
}
