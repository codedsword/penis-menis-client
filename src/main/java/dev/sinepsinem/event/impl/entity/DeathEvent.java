package dev.sinepsinem.event.impl.entity;

import dev.sinepsinem.event.Event;
import net.minecraft.world.entity.LivingEntity;

public class DeathEvent extends Event {
    private final LivingEntity entity;

    public DeathEvent(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity getEntity() {
        return entity;
    }
}
