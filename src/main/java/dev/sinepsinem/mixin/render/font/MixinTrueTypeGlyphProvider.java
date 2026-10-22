package dev.sinepsinem.mixin.render.font;

import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import dev.sinepsinem.util.text.FreeTypeFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TrueTypeGlyphProvider.class)
public class MixinTrueTypeGlyphProvider {
    // glyph metrics, must use the same hinting target as the rasterization in NativeImage#copyFromFont
    @ModifyArg(method = "loadGlyph", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/freetype/FreeType;FT_Load_Glyph(Lorg/lwjgl/util/freetype/FT_Face;II)I"), index = 2)
    private int lightHintingMetrics(int flags) {
        return FreeTypeFlags.smooth(flags);
    }
}
