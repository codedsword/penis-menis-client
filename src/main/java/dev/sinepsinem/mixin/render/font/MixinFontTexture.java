package dev.sinepsinem.mixin.render.font;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.util.text.PaddedGlyphBitmap;
import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * Our ttf fonts are rasterized oversampled, sampling them with NEAREST throws away most of that
 * detail when the gui scale is lower than the oversample. Bilinear filtering averages it instead.
 */
@Mixin(FontTexture.class)
public abstract class MixinFontTexture extends AbstractTexture {
    @Unique
    private boolean sinepsinem$smooth;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void smoothSampler(Supplier<String> label, GlyphRenderTypes renderTypes, boolean colored, CallbackInfo ci) {
        // atlases are named after the font, e.g. sinepsinem:sf_pro/0
        sinepsinem$smooth = label.get().startsWith(SinepSinem.MOD_ID + ":");
        if (sinepsinem$smooth) {
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
    }

    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true)
    private GlyphBitmap padGlyph(GlyphBitmap glyph) {
        return sinepsinem$smooth && !(glyph instanceof PaddedGlyphBitmap) ? new PaddedGlyphBitmap(glyph) : glyph;
    }
}
