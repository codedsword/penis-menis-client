package dev.sinepsinem.features.modules.misc;

import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.chat.ChatUtil;
import dev.sinepsinem.util.chat.SimpleSignature;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.scores.PlayerTeam;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Warns when staff is online: players whose tab name or team prefix carries a staff rank.
 * Experimental also flags players in spectator mode, which is how most vanish plugins hide staff.
 */
public class StaffDetectorModule extends Module {
    public enum Mode {
        RANKS, EXPERIMENTAL
    }

    private static final String[] RANKS = {"staff", "mod", "admin", "helper", "owner", "developer", "[dev]", "manager", "support", "trial", "jrmod", "srmod"};

    private final Setting<Mode> mode = mode("Mode", Mode.EXPERIMENTAL);
    private final Setting<String> extra = str("ExtraRanks", "");
    private final Setting<Boolean> leaves = bool("Leaves", true);

    private final Set<UUID> staff = new HashSet<>();
    private int ticks;

    public StaffDetectorModule() {
        super("StaffDetector", "Warns you when staff is online", Category.MISC);
    }

    @Override
    public void onDisable() {
        staff.clear();
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (++ticks % 20 != 0 || mc.getConnection() == null) return;
        Set<UUID> online = new HashSet<>();
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            String reason = reason(info);
            if (reason == null) continue;
            UUID id = info.getProfile().id();
            online.add(id);
            if (staff.add(id)) {
                ChatUtil.sendMessage(Component.literal("Staff ").withStyle(ChatFormatting.RED)
                        .append(Component.literal(info.getProfile().name()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" is online (" + reason + ")").withStyle(ChatFormatting.GRAY)),
                        SimpleSignature.from("staff/" + id));
            }
        }
        for (UUID id : Set.copyOf(staff)) {
            if (online.contains(id)) continue;
            staff.remove(id);
            if (leaves.getValue()) {
                ChatUtil.sendMessage(Component.literal("A staff member left").withStyle(ChatFormatting.GREEN), SimpleSignature.from("staff/" + id));
            }
        }
    }

    private String reason(PlayerInfo info) {
        if (mc.player != null && info.getProfile().id().equals(mc.player.getUUID())) return null;
        StringBuilder text = new StringBuilder();
        if (info.getTabListDisplayName() != null) text.append(info.getTabListDisplayName().getString()).append(' ');
        PlayerTeam team = info.getTeam();
        if (team != null) text.append(team.getPlayerPrefix().getString()).append(' ').append(team.getName()).append(' ');
        String lower = text.toString().toLowerCase(Locale.ROOT);
        for (String rank : RANKS) {
            if (lower.contains(rank)) return rank;
        }
        for (String rank : extra.getValue().toLowerCase(Locale.ROOT).split(",")) {
            if (!rank.isBlank() && lower.contains(rank.trim())) return rank.trim();
        }
        if (mode.getValue() == Mode.EXPERIMENTAL && info.getGameMode() == GameType.SPECTATOR) return "spectator";
        return null;
    }
}
