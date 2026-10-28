package dev.sinepsinem.features.modules.misc;

import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import java.awt.Color;

/**
 * Keeps combat modules off your teammates. Teammates are decided by the scoreboard team, the color
 * of the name, or the color of dyed leather armor (bed wars style games).
 */
public class TeamsModule extends Module {
    private static TeamsModule INSTANCE;

    public enum Condition {
        ANY, SCOREBOARD, NAME_COLOR, ARMOR_COLOR
    }

    private final Setting<Condition> conditions = mode("TeamConditions", Condition.ANY);
    private final Setting<Color> teamColor = color("TeamColor", 125, 150, 190, 255);

    public TeamsModule() {
        super("Teams", "Does not target teammates", Category.MISC);
        INSTANCE = this;
    }

    public static Color teamColor() {
        return INSTANCE == null ? new Color(125, 150, 190) : INSTANCE.teamColor.getValue();
    }

    public static boolean isTeammate(Player player) {
        TeamsModule module = INSTANCE;
        if (module == null || !module.isEnabled() || mc.player == null || player == mc.player) return false;
        return switch (module.conditions.getValue()) {
            case SCOREBOARD -> scoreboard(player);
            case NAME_COLOR -> nameColor(player);
            case ARMOR_COLOR -> armorColor(player);
            case ANY -> scoreboard(player) || nameColor(player) || armorColor(player);
        };
    }

    private static boolean scoreboard(Player player) {
        return mc.player.getTeam() != null && mc.player.isAlliedTo(player);
    }

    private static boolean nameColor(Player player) {
        int own = mc.player.getTeamColor(), theirs = player.getTeamColor();
        // white means no color at all
        return own != 0xFFFFFF && own == theirs;
    }

    private static boolean armorColor(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            Integer own = dye(mc.player.getItemBySlot(slot)), theirs = dye(player.getItemBySlot(slot));
            if (own != null && own.equals(theirs)) return true;
        }
        return false;
    }

    private static Integer dye(ItemStack stack) {
        DyedItemColor color = stack.get(DataComponents.DYED_COLOR);
        return color == null ? null : color.rgb();
    }

    @Override
    public String getDisplayInfo() {
        return dev.sinepsinem.features.settings.EnumConverter.getProperName(conditions.getValue());
    }
}
