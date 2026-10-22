package dev.sinepsinem.features.modules.render;

import dev.sinepsinem.event.impl.render.Render2DEvent;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.TargetUtil;
import dev.sinepsinem.util.render.ProjectionUtil;
import dev.sinepsinem.util.text.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Screen space name tags in Product Sans, drawn on the hud so they keep a constant size.
 */
public class NameTagsModule extends Module {
    private static NameTagsModule INSTANCE;

    private final Setting<Float> scale = num("Scale", 1.0f, 0.5f, 2.0f);
    private final Setting<Boolean> health = bool("Health", true);
    private final Setting<Boolean> distance = bool("Distance", true);
    private final Setting<Boolean> ping = bool("Ping", false);
    private final Setting<Boolean> items = bool("Items", true);
    private final Setting<Boolean> self = bool("Self", false);

    public NameTagsModule() {
        super("NameTags", "Better name tags for players", Category.RENDER);
        INSTANCE = this;
    }

    /**
     * Used by the entity renderer mixin to hide vanilla tags we replace.
     */
    public static boolean replaces(Player player) {
        return INSTANCE != null && INSTANCE.isEnabled() && (player != mc.player || INSTANCE.self.getValue());
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        GuiGraphicsExtractor graphics = event.getContext();
        float delta = event.getDelta();

        List<Tag> tags = new ArrayList<>();
        for (Player player : mc.level.players()) {
            if (player == mc.player ? !self.getValue() || mc.options.getCameraType().isFirstPerson() : !TargetUtil.isValidPlayer(player)) {
                continue;
            }
            Vec3 pos = player.getPosition(delta).add(0, player.getBbHeight() + 0.25, 0);
            Vector3f screen = ProjectionUtil.project(pos);
            if (screen != null) tags.add(new Tag(player, screen));
        }
        // far tags first so near ones end up on top
        tags.sort(Comparator.comparingDouble((Tag t) -> t.screen.z).reversed());

        for (Tag tag : tags) {
            draw(graphics, tag.player, tag.screen);
        }
    }

    private void draw(GuiGraphicsExtractor graphics, Player player, Vector3f screen) {
        MutableComponent text = Fonts.bold(player.getGameProfile().name()).withColor(0xFFFFFFFF);
        if (health.getValue()) {
            float hp = player.getHealth() + player.getAbsorptionAmount();
            text.append(Fonts.text(String.format(" %.1f", hp)).withColor(healthColor(player)));
        }
        if (distance.getValue() && player != mc.player) {
            text.append(Fonts.text(String.format(" %.0fm", player.distanceTo(mc.player))).withColor(0xFFA3A3A3));
        }
        if (ping.getValue() && mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
            if (info != null) text.append(Fonts.text(" " + info.getLatency() + "ms").withColor(0xFFA3A3A3));
        }

        int width = mc.font.width(text);
        float s = scale.getValue();
        graphics.pose().pushMatrix();
        graphics.pose().translate(screen.x, screen.y);
        graphics.pose().scale(s, s);

        int x = -width / 2;
        graphics.fill(x - 4, -12, x + width + 4, 1, 0xA0101010);
        // health bar along the bottom edge, legitclient pink while healthy
        float ratio = Mth.clamp(player.getHealth() / player.getMaxHealth(), 0, 1);
        graphics.fill(x - 4, 0, x - 4 + Math.round((width + 8) * ratio), 1, healthColor(player));
        graphics.text(mc.font, text, x, -10, -1, false);

        if (items.getValue()) {
            List<ItemStack> stacks = new ArrayList<>();
            stacks.add(player.getMainHandItem());
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                stacks.add(player.getItemBySlot(slot));
            }
            stacks.add(player.getOffhandItem());
            stacks.removeIf(ItemStack::isEmpty);

            int itemX = -stacks.size() * 16 / 2;
            for (ItemStack stack : stacks) {
                graphics.item(stack, itemX, -30);
                graphics.itemDecorations(mc.font, stack, itemX, -30);
                itemX += 16;
            }
        }
        graphics.pose().popMatrix();
    }

    private static int healthColor(Player player) {
        float ratio = Mth.clamp((player.getHealth() + player.getAbsorptionAmount()) / player.getMaxHealth(), 0, 1);
        if (ratio > 0.66f) return 0xFFE662A3;
        if (ratio > 0.33f) return 0xFFFACC15;
        return ARGB.color(255, 0xF87171);
    }

    private record Tag(Player player, Vector3f screen) {
    }
}
