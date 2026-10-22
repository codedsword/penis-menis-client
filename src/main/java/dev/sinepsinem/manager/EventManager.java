package dev.sinepsinem.manager;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.event.Stage;
import dev.sinepsinem.event.impl.entity.DeathEvent;
import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.impl.entity.player.UpdateWalkingPlayerEvent;
import dev.sinepsinem.event.impl.input.KeyInputEvent;
import dev.sinepsinem.event.impl.input.MouseInputEvent;
import dev.sinepsinem.event.impl.network.ChatEvent;
import dev.sinepsinem.event.impl.network.PacketEvent;
import dev.sinepsinem.event.impl.render.Render2DEvent;
import dev.sinepsinem.event.impl.render.Render3DEvent;
import dev.sinepsinem.event.impl.render.RenderBlockOutlineEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.Feature;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

public class EventManager extends Feature {
    private final Set<Integer> deadPlayers = new HashSet<>();

    public void init() {
        EVENT_BUS.register(this);
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outline) ->
                !EVENT_BUS.post(new RenderBlockOutlineEvent()));
    }

    public void onUnload() {
        EVENT_BUS.unregister(this);
    }

    // before the movement packet, so packets modules send in onTick pass post checks
    @Subscribe
    public void onTickPre(TickEvent.Pre event) {
        if (nullCheck())
            return;
        SinepSinem.moduleManager.onTick();
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck())
            return;
        SinepSinem.speedManager.update();
        Set<Integer> dead = new HashSet<>();
        for (Player player : mc.level.players()) {
            if (player == null || player.getHealth() > 0.0F)
                continue;
            dead.add(player.getId());
            if (!deadPlayers.contains(player.getId()))
                EVENT_BUS.post(new DeathEvent(player));
        }
        deadPlayers.clear();
        deadPlayers.addAll(dead);
    }

    @Subscribe
    public void onPacketReceive(PacketEvent.Receive event) {
        SinepSinem.serverManager.onPacketReceived();
        if (event.getPacket() instanceof ClientboundSetTimePacket)
            SinepSinem.serverManager.update();
        if (event.getPacket() instanceof ClientboundCustomPayloadPacket(CustomPacketPayload payload)
                && payload instanceof BrandPayload(String brand)) {
            SinepSinem.serverManager.setServerBrand(brand);
        }
    }

    @Subscribe
    public void onWorldRender(Render3DEvent event) {
        SinepSinem.moduleManager.onRender3D(event);
    }

    @Subscribe
    public void onRenderGameOverlayEvent(Render2DEvent event) {
        SinepSinem.moduleManager.onRender2D(event);
    }

    @Subscribe
    public void onKeyInput(KeyInputEvent event) {
        SinepSinem.moduleManager.onKeyPressed(event.getKey());
    }

    @Subscribe
    public void onMouseInput(MouseInputEvent event) {
        if (event.getAction() == 1) {
            SinepSinem.moduleManager.onMouseClicked(event.getButton());
        }
    }

    @Subscribe
    public void onChatSent(ChatEvent event) {
        String message = event.getMessage();
        if (!message.startsWith(SinepSinem.commandManager.getCommandPrefix())) {
            return;
        }
        event.cancel();
        SinepSinem.commandManager.onChatSent(message);
    }
}