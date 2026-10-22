package dev.sinepsinem.event.impl.entity.player;

import dev.sinepsinem.event.Event;

public class TickEvent extends Event {
    public static class Post extends TickEvent { }

    public static class Pre extends TickEvent { }
}
