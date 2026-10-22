package dev.sinepsinem.features.modules.misc;

import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;

import java.util.regex.Pattern;

/**
 * Flags anticheat bots and NPCs so they are skipped by targeting and ESP.
 */
public class AntiBotModule extends Module {
    private static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    private static AntiBotModule INSTANCE;

    private final Setting<Boolean> tabList = bool("TabList", true);
    private final Setting<Boolean> tabListed = bool("MustBeListed", false);
    private final Setting<Boolean> nameCheck = bool("NameCheck", true);
    private final Setting<Boolean> duplicateName = bool("DuplicateName", true);
    private final Setting<Boolean> pingCheck = bool("ZeroPing", false);
    private final Setting<Boolean> ticksCheck = bool("TicksExisted", false);
    private final Setting<Integer> minTicks = num("MinTicks", 10, 0, 200);

    public AntiBotModule() {
        super("AntiBot", "Excludes bots from targeting and ESP (tab list, name check)", Category.MISC);
        minTicks.setVisibility(v -> ticksCheck.getValue());
        INSTANCE = this;
    }

    public static boolean isBot(Player player) {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.check(player);
    }

    private boolean check(Player player) {
        if (player == mc.player || mc.getConnection() == null) return false;
        String name = player.getGameProfile().name();

        if (nameCheck.getValue() && (name == null || !VALID_NAME.matcher(name).matches())) {
            return true;
        }

        PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
        if (tabList.getValue()) {
            // bots spawned by anticheats are usually never sent in the player list
            if (info == null) return true;
            // entity uuid points at someone else's tab entry
            if (nameCheck.getValue() && !info.getProfile().name().equals(name)) return true;
            if (tabListed.getValue() && !mc.getConnection().getListedOnlinePlayers().contains(info)) return true;
        }

        if (duplicateName.getValue()) {
            int count = 0;
            for (Player other : mc.level.players()) {
                if (name.equals(other.getGameProfile().name()) && ++count > 1) return true;
            }
        }

        if (pingCheck.getValue() && info != null && info.getLatency() <= 0) return true;

        return ticksCheck.getValue() && player.tickCount < minTicks.getValue();
    }
}
