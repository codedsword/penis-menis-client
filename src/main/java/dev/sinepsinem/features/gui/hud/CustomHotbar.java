package dev.sinepsinem.features.gui.hud;

import dev.sinepsinem.features.modules.client.InterfaceModule;
import dev.sinepsinem.mixin.render.gui.HudAccessor;
import dev.sinepsinem.util.imgui.Draw;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.Theme;
import imgui.ImDrawList;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * Glass hotbar: one blurred, glowing panel in the vanilla footprint (so hearts, food and the
 * experience bar stay where they are), a selection that glides between slots, and the selected
 * item lifted a little. The attack indicator becomes a ring.
 */
public final class CustomHotbar implements HudElement {
    public static final CustomHotbar INSTANCE = new CustomHotbar();

    private static final int SLOT = 20;

    private final ImGuiImpl.Layer layer = new ImGuiImpl.Layer("hotbar", false);
    private final float[] lift = new float[9];
    private float selection = -1;
    private float offhandAlpha;
    private long lastFrame = System.nanoTime();

    private CustomHotbar() {
    }

    @Override
    public boolean isActive() {
        InterfaceModule module = InterfaceModule.get();
        return module != null && module.hotbar.getValue();
    }

    @Override
    public ImGuiImpl.Layer layer() {
        return layer;
    }

    @Override
    public void reset() {
        selection = -1;
        offhandAlpha = 0;
        Arrays.fill(lift, 0);
    }

    public void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, HudAccessor hud) {
        Player player = hud.sinepsinem$getCameraPlayer();
        if (player == null) return;

        long now = System.nanoTime();
        float dt = Math.min((now - lastFrame) / 1.0E9f, 0.1f);
        lastFrame = now;
        float t = HudElement.smoothing(dt);

        int center = graphics.guiWidth() / 2;
        int bottom = graphics.guiHeight();
        float x1 = center - 91, y1 = bottom - 23, x2 = center + 91, y2 = bottom - 1;
        int selected = player.getInventory().getSelectedSlot();
        float target = x1 + 1 + selected * SLOT;
        selection = selection < 0 ? target : selection + (target - selection) * t;

        ItemStack offhand = player.getOffhandItem();
        HumanoidArm offhandArm = player.getMainArm().getOpposite();
        offhandAlpha += ((offhand.isEmpty() ? 0 : 1) - offhandAlpha) * t;
        float offX = offhandArm == HumanoidArm.LEFT ? x1 - 28 : x2 + 6;

        float attack = mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR
                ? mc.player.getAttackStrengthScale(0.0f) : 1.0f;
        float ringX = offhandArm == HumanoidArm.RIGHT ? x1 - 13 : x2 + 15;

        float rounding = HudElement.rounding();
        chrome(graphics, c -> {
            ImDrawList draw = c.draw();
            float s = c.scale();
            // tight clip, keeps the element's bounds (and so its layer) small
            draw.pushClipRect((x1 - 60) * s, (y1 - 20) * s, (x2 + 60) * s, (bottom + 1) * s);
            Draw.panel(draw, x1 * s, y1 * s, x2 * s, y2 * s, rounding * s, 1.0f, s);

            float sx1 = selection * s, sy1 = (y1 + 1) * s, sx2 = (selection + SLOT) * s, sy2 = (y2 - 1) * s;
            float r = Math.max(0, rounding - 1) * s;
            Draw.glow(draw, ImGuiImpl.TEX_GLOW, sx1, sy1, sx2, sy2, r, 6 * s, Theme.withAlpha(Theme.accent(), 0.35f), false);
            draw.addRectFilled(sx1, sy1, sx2, sy2, Theme.withAlpha(Theme.accent(), 0.2f), r);
            draw.addRect(sx1, sy1, sx2, sy2, Theme.withAlpha(Theme.accent(), 0.85f), r, 0, Math.max(1, s * 0.6f));

            if (offhandAlpha > 0.01f) {
                Draw.panel(draw, offX * s, y1 * s, (offX + 22) * s, y2 * s, rounding * s, offhandAlpha, s);
            }

            if (attack < 1.0f) {
                float cy = (y1 + y2) / 2 * s, cx = ringX * s, radius = 7 * s;
                draw.addCircle(cx, cy, radius, Theme.rgba(0xFFFFFF, 40), 32, 2 * s);
                draw.pathArcTo(cx, cy, radius, (float) -Math.PI / 2, (float) (-Math.PI / 2 + Math.PI * 2 * attack), 32);
                draw.pathStroke(Theme.accent(), 0, 2 * s);
            }
            draw.popClipRect();
        });

        // items on top of the glass, the selected one lifted
        int seed = 1;
        int itemY = bottom - 20;
        for (int i = 0; i < 9; i++) {
            lift[i] += ((i == selected ? 1 : 0) - lift[i]) * t;
            int x = (int) x1 + 3 + i * SLOT;
            ItemStack stack = player.getInventory().getItem(i);
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + 8, itemY + 8 - lift[i] * 1.5f);
            float scale = 1 + lift[i] * 0.08f;
            graphics.pose().scale(scale, scale);
            graphics.pose().translate(-(x + 8), -(itemY + 8));
            hud.sinepsinem$extractSlot(graphics, x, itemY, deltaTracker, player, stack, seed++);
            graphics.pose().popMatrix();
        }
        if (!offhand.isEmpty()) {
            hud.sinepsinem$extractSlot(graphics, (int) offX + 3, itemY, deltaTracker, player, offhand, seed);
        }
    }
}
