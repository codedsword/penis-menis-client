package dev.sinepsinem.features.modules.misc;

import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import net.minecraft.world.entity.player.Player;

/**
 * Replaces your name everywhere text is drawn (chat, tab list, name tags, scoreboard, hud), for
 * recording without showing your account.
 */
public class NickHiderModule extends Module {
    private static NickHiderModule INSTANCE;

    private final Setting<String> nick = str("Nick", "You");

    public NickHiderModule() {
        super("NickHider", "Hides your name on screen", Category.MISC);
        INSTANCE = this;
    }

    private static boolean active() {
        return INSTANCE != null && INSTANCE.isEnabled() && mc.getUser() != null && !INSTANCE.nick.getValue().isEmpty();
    }

    /**
     * @return {@code text} with your name replaced by the nick, the same string when nothing changes
     */
    public static String replace(String text) {
        if (!active() || text == null) return text;
        String name = mc.getUser().getName();
        return name.length() >= 3 && text.contains(name) ? text.replace(name, INSTANCE.nick.getValue()) : text;
    }

    /**
     * @return the name to show for {@code player}
     */
    public static String name(Player player) {
        String name = player.getGameProfile().name();
        return player == mc.player ? replace(name) : name;
    }
}
