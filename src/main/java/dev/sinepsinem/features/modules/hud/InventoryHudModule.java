package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.util.imgui.Theme;
import imgui.ImDrawList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2f;

/**
 * Ambient's "Inventory": the 27 storage slots on a grid of dark rounded cells.
 */
public class InventoryHudModule extends ImGuiHudModule {
    // in gui pixels, the items are drawn by vanilla at gui scale
    private static final int SLOT = 18, GAP = 2, PAD = 4;

    public InventoryHudModule() {
        super("Inventory", "Shows your inventory", 186, 74);
        pos.setValue(new Vector2f(0.004f, 0.56f));
    }

    private float gridWidth() {
        return 9 * SLOT + 8 * GAP;
    }

    @Override
    protected float[] draw(ImDrawList draw, float x, float y) {
        float s = scale;
        float w = (gridWidth() + PAD * 2) * s, h = TITLE + (3 * SLOT + 2 * GAP + PAD * 2) * s;
        float top = panel(draw, x, y, w, h, "Inventory");
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                float sx = x + (PAD + col * (SLOT + GAP)) * s, sy = top + (PAD + row * (SLOT + GAP)) * s;
                draw.addRectFilled(sx, sy, sx + SLOT * s, sy + SLOT * s, Theme.rgba(0x000000, 90), 4);
            }
        }
        return new float[]{w, h};
    }

    @Override
    protected void drawVanilla(GuiGraphicsExtractor graphics, float x, float y) {
        float top = y + TITLE / scale;
        for (int i = 0; i < 27; i++) {
            ItemStack stack = mc.player.getInventory().getItem(9 + i);
            if (stack.isEmpty()) continue;
            int col = i % 9, row = i / 9;
            int ix = Math.round(x + PAD + col * (SLOT + GAP) + 1), iy = Math.round(top + PAD + row * (SLOT + GAP) + 1);
            graphics.item(stack, ix, iy);
            graphics.itemDecorations(mc.font, stack, ix, iy);
        }
    }
}
