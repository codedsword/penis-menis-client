package dev.sinepsinem.mixin.render.font;

import com.mojang.blaze3d.platform.NativeImage;
import dev.sinepsinem.util.text.FreeTypeFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(NativeImage.class)
public class MixinNativeImage {
    // FreeType renders the glyph into the 8 bit bitmap that gets copied into the font atlas
    @ModifyArg(method = "copyFromFont", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/freetype/FreeType;FT_Load_Glyph(Lorg/lwjgl/util/freetype/FT_Face;II)I"), index = 2)
    private int lightHintingRender(int flags) {
        return FreeTypeFlags.smooth(flags);
    }
}
