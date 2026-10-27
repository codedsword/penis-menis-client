package dev.sinepsinem.mixin.render.font;

import dev.sinepsinem.features.modules.client.InterfaceModule;
import dev.sinepsinem.util.text.Fonts;
import net.minecraft.network.chat.FontDescription;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * SF Pro for everything: text in the default font (chat, menus, scoreboard, item names, ...) is
 * drawn with our font set instead. Our set falls back to the default one for glyphs it lacks.
 */
@Mixin(targets = "net.minecraft.client.gui.font.FontManager$CachedFontProvider")
public class MixinFontManagerProvider {
    @ModifyVariable(method = "getGlyphSource", at = @At("HEAD"), argsOnly = true)
    private FontDescription sfProEverywhere(FontDescription description) {
        return InterfaceModule.sfProEverywhere() && FontDescription.DEFAULT.equals(description) ? Fonts.MEDIUM : description;
    }
}
