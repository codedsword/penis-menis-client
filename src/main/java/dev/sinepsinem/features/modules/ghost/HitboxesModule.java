package dev.sinepsinem.features.modules.ghost;

import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.features.modules.misc.TeamsModule;
import dev.sinepsinem.util.TargetUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Grows the boxes your crosshair picks entities with, the server still checks reach.
 */
public class HitboxesModule extends Module {
    private static HitboxesModule INSTANCE;

    private final Setting<Float> expand = num("Expand", 0.2f, 0.0f, 1.0f);
    private final Setting<Boolean> players = bool("Players", true);
    private final Setting<Boolean> hostiles = bool("Hostiles", false);
    private final Setting<Boolean> passives = bool("Passives", false);

    public HitboxesModule() {
        super("Hitboxes", "Makes entities easier to hit", Category.GHOST);
        INSTANCE = this;
    }

    /**
     * @return how much bigger {@code entity}'s pick box should be
     */
    public static float expansion(Entity entity) {
        HitboxesModule module = INSTANCE;
        if (module == null || !module.isEnabled() || mc.player == null) return 0;
        if (entity instanceof Player player && TeamsModule.isTeammate(player)) return 0;
        return TargetUtil.isValidTarget(entity, module.players.getValue(), module.hostiles.getValue(), module.passives.getValue())
                ? module.expand.getValue() : 0;
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(expand.getValue());
    }
}
