package me.alpha432.oyvey.mixin.render.gui;

import me.alpha432.oyvey.util.BuildConfig;
import me.alpha432.oyvey.util.text.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.alpha432.oyvey.util.traits.Util.mc;

@Mixin(LogoRenderer.class)
public class MixinLogoRenderer {
    @Shadow
    @Final
    private boolean keepLogoThroughFade;

    // replaces the Minecraft logo on the title screen with the client's name
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFI)V", at = @At("HEAD"), cancellable = true)
    private void drawClientLogo(GuiGraphicsExtractor graphics, int width, float alpha, int heightOffset, CallbackInfo ci) {
        if (!(mc.gui.screen() instanceof TitleScreen)) return;
        ci.cancel();

        float effectiveAlpha = keepLogoThroughFade ? 1.0f : alpha;
        Component title = Fonts.title(BuildConfig.NAME);
        int titleWidth = mc.font.width(title);
        int x = width / 2 - titleWidth / 2;
        int y = heightOffset + 4;
        graphics.text(mc.font, title, x, y, ARGB.white(effectiveAlpha), false);

        // pink underline accent like the legitclient.com headings
        int lineWidth = Math.max(24, titleWidth / 3);
        graphics.fill(width / 2 - lineWidth / 2, y + mc.font.lineHeight + 22, width / 2 + lineWidth / 2, y + mc.font.lineHeight + 24,
                ARGB.color(Math.round(255 * effectiveAlpha), 0xE662A3));

        Component subtitle = Fonts.text("for Minecraft 26.2");
        graphics.text(mc.font, subtitle, width / 2 - mc.font.width(subtitle) / 2, y + mc.font.lineHeight + 30,
                ARGB.color(Math.round(255 * effectiveAlpha), 0xA3A3A3), false);
    }
}
