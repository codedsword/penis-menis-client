package dev.sinepsinem.mixin.render.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Hud.class)
public interface HudAccessor {
    @Invoker("getCameraPlayer")
    Player sinepsinem$getCameraPlayer();

    // draws the item with vanilla's pick up "pop" and its decorations
    @Invoker("extractSlot")
    void sinepsinem$extractSlot(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int seed);
}
