package dev.sinepsinem.features.modules.misc;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.impl.network.PacketEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.chat.ChatUtil;
import dev.sinepsinem.util.chat.SimpleSignature;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

import java.util.Locale;

/**
 * Tells you when the server sets you back (an anticheat lag back) and when it stops responding.
 */
public class LagAlertModule extends Module {
    private static final SimpleSignature LAGBACK = SimpleSignature.from("lagalert/lagback");
    private static final SimpleSignature SERVER = SimpleSignature.from("lagalert/server");

    private final Setting<Boolean> lagbacks = bool("Lagbacks", true);
    private final Setting<Boolean> server = bool("ServerLag", true);
    private final Setting<Integer> threshold = num("Threshold", 1500, 500, 5000);

    private int ticks, setbacks;
    private boolean lagging;
    private Object connection;

    public LagAlertModule() {
        super("LagAlert", "Warns about lag backs and server lag", Category.MISC);
    }

    @Override
    public void onEnable() {
        ticks = 0;
        setbacks = 0;
    }

    @Subscribe
    public void onPacket(PacketEvent.Receive event) {
        if (!(event.getPacket() instanceof ClientboundPlayerPositionPacket) || !lagbacks.getValue()) return;
        // the first teleports after joining are the spawn, not a lag back
        if (ticks < 40) return;
        mc.execute(() -> {
            setbacks++;
            ChatUtil.sendMessage(Component.literal("Lagback detected").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" [x" + setbacks + "]").withStyle(ChatFormatting.GRAY)), LAGBACK);
        });
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        // a new server starts the count over
        if (mc.getConnection() != connection) {
            connection = mc.getConnection();
            ticks = 0;
            setbacks = 0;
        }
        ticks++;
        if (!server.getValue()) return;
        long silent = SinepSinem.serverManager.serverRespondingTime();
        if (silent > threshold.getValue() && ticks > 40) {
            lagging = true;
            ChatUtil.sendMessage(Component.literal(String.format(Locale.ROOT, "Server is not responding (%.1fs)", silent / 1000.0))
                    .withStyle(ChatFormatting.RED), SERVER);
        } else if (lagging && silent < 200) {
            lagging = false;
            ChatUtil.sendMessage(Component.literal("Server is responding again").withStyle(ChatFormatting.GREEN), SERVER);
        }
    }
}
