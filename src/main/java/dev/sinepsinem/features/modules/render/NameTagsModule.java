package dev.sinepsinem.features.modules.render;

import dev.sinepsinem.event.impl.render.Render2DEvent;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.modules.misc.NickHiderModule;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.TargetUtil;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.Theme;
import dev.sinepsinem.util.render.ProjectionUtil;
import imgui.ImDrawList;
import imgui.ImGui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Ambient's name tags: a dark rounded pill with the health in red, the name in white and the
 * distance in blue, drawn on the hud so they keep a constant size. Armor and held items above.
 */
public class NameTagsModule extends Module {
    private static NameTagsModule INSTANCE;

    private static final int HEALTH = Theme.rgb(0xFF4D4D), DISTANCE = Theme.rgb(0x6F8BFF), PING = Theme.rgb(0x9A9A9A);

    private final Setting<Float> scale = num("Scale", 1.0f, 0.5f, 2.0f);
    private final Setting<Boolean> health = bool("Health", true);
    private final Setting<Boolean> distance = bool("Distance", true);
    private final Setting<Boolean> ping = bool("Ping", false);
    private final Setting<Boolean> items = bool("Items", true);
    private final Setting<Boolean> self = bool("Self", false);

    private final ImGuiImpl.Layer layer = new ImGuiImpl.Layer("nametags", false);

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
        if (tags.isEmpty()) return;
        // far tags first so near ones end up on top
        tags.sort(Comparator.comparingDouble((Tag t) -> t.screen.z).reversed());

        float guiScale = mc.getWindow().getGuiScale();
        ImGuiImpl.frame(layer, graphics, () -> {
            ImDrawList draw = ImGui.getBackgroundDrawList();
            for (Tag tag : tags) pill(draw, tag, guiScale);
        });
        if (items.getValue()) {
            for (Tag tag : tags) items(graphics, tag);
        }
    }

    private void pill(ImDrawList draw, Tag tag, float guiScale) {
        Player player = tag.player;
        List<String[]> parts = new ArrayList<>();
        if (health.getValue()) {
            float hp = player.getHealth() + player.getAbsorptionAmount();
            parts.add(new String[]{hp == Math.floor(hp) ? String.valueOf((int) hp) : String.format(Locale.ROOT, "%.1f", hp), "health"});
        }
        parts.add(new String[]{NickHiderModule.name(player), "name"});
        if (distance.getValue() && player != mc.player) {
            parts.add(new String[]{String.format(Locale.ROOT, "%.1fm", player.distanceTo(mc.player)), "distance"});
        }
        if (ping.getValue() && mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
            if (info != null) parts.add(new String[]{info.getLatency() + "ms", "ping"});
        }

        float size = ImGuiImpl.FONT_SIZE * scale.getValue();
        float ratio = size / ImGuiImpl.FONT_SIZE;
        float space = 5 * ratio, padding = 8 * ratio, height = size + 8 * ratio;
        float width = padding * 2 - space;
        for (String[] part : parts) width += textWidth(part[0]) * ratio + space;

        float cx = tag.screen.x * guiScale, bottom = tag.screen.y * guiScale;
        float x = cx - width / 2, y = bottom - height;
        draw.addRectFilled(x, y, x + width, y + height, Theme.rgba(0x141414, 170), height / 2.5f);
        float tx = x + padding, ty = y + (height - size) / 2;
        for (String[] part : parts) {
            int color = switch (part[1]) {
                case "health" -> HEALTH;
                case "distance" -> DISTANCE;
                case "ping" -> PING;
                default -> Theme.TEXT;
            };
            draw.addText(ImGuiImpl.regular, Math.round(size), tx, ty, color, part[0]);
            tx += textWidth(part[0]) * ratio + space;
        }
        tag.top = y / guiScale;
    }

    private static float textWidth(String text) {
        ImGui.pushFont(ImGuiImpl.regular);
        float width = ImGui.calcTextSize(text).x;
        ImGui.popFont();
        return width;
    }

    private void items(GuiGraphicsExtractor graphics, Tag tag) {
        Player player = tag.player;
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(player.getMainHandItem());
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            stacks.add(player.getItemBySlot(slot));
        }
        stacks.add(player.getOffhandItem());
        stacks.removeIf(ItemStack::isEmpty);
        if (stacks.isEmpty()) return;

        float s = scale.getValue();
        graphics.pose().pushMatrix();
        graphics.pose().translate(tag.screen.x, tag.top - 2);
        graphics.pose().scale(s, s);
        int itemX = -stacks.size() * 16 / 2;
        for (ItemStack stack : stacks) {
            graphics.item(stack, itemX, -16);
            graphics.itemDecorations(mc.font, stack, itemX, -16);
            itemX += 16;
        }
        graphics.pose().popMatrix();
    }

    private static final class Tag {
        final Player player;
        final Vector3f screen;
        // top of the pill in gui pixels, the items go above it
        float top;

        Tag(Player player, Vector3f screen) {
            this.player = player;
            this.screen = screen;
            this.top = screen.y;
        }
    }
}
