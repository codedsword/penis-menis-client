package dev.sinepsinem.mixin.render;

import dev.sinepsinem.util.render.blur.KawaseBlur;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public class MixinGuiRenderer {
    // the world (and the title screen panorama) is in the main target, no gui element is yet
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepare()V"))
    private void kawaseBlur(CallbackInfo ci) {
        KawaseBlur.process();
    }
}
