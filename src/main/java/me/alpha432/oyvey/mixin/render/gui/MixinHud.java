package me.alpha432.oyvey.mixin.render.gui;

import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.alpha432.oyvey.util.traits.Util.EVENT_BUS;
import static me.alpha432.oyvey.util.traits.Util.mc;

@Mixin(Hud.class)
public class MixinHud {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    public void render(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        boolean debugOpen = mc.debugEntries.isOverlayVisible();

        if (debugOpen || mc.gui.hud.isHidden()) return;

        Render2DEvent event = new Render2DEvent(context, tickCounter.getGameTimeDeltaPartialTick(true));
        EVENT_BUS.post(event);
    }

}
