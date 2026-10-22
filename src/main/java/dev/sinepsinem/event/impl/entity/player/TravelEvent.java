package dev.sinepsinem.event.impl.entity.player;

import dev.sinepsinem.event.Event;

public class TravelEvent extends Event {
    public static class Post extends TravelEvent { }

    public static class Pre extends TravelEvent { }
}
