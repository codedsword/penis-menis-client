package dev.sinepsinem.mixin.render.gui;

import dev.sinepsinem.event.impl.render.Render2DEvent;
import dev.sinepsinem.event.impl.render.RenderOverlayEvent;
import dev.sinepsinem.features.gui.hud.CustomHotbar;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.sinepsinem.util.traits.Util.EVENT_BUS;
import static dev.sinepsinem.util.traits.Util.mc;

@Mixin(Hud.class)
public class MixinHud {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    public void render(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        boolean debugOpen = mc.debugEntries.isOverlayVisible();

        if (debugOpen || mc.gui.hud.isHidden()) return;

        Render2DEvent event = new Render2DEvent(context, tickCounter.getGameTimeDeltaPartialTick(true));
        EVENT_BUS.post(event);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    public void renderOverlay(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (mc.player == null || mc.level == null || mc.debugEntries.isOverlayVisible() || mc.gui.hud.isHidden()) return;
        EVENT_BUS.post(new RenderOverlayEvent(context, tickCounter.getGameTimeDeltaPartialTick(true)));
    }

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void customHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (CustomHotbar.INSTANCE.isActive()) {
            CustomHotbar.INSTANCE.extract(graphics, deltaTracker, (HudAccessor) this);
            ci.cancel();
        }
    }

}
