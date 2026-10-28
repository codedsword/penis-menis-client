package dev.sinepsinem.mixin.render.font;

import dev.sinepsinem.features.modules.misc.NickHiderModule;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Every string that is drawn passes through here on its way to glyphs, the nick hider swaps your
 * name out before that.
 */
@Mixin(StringDecomposer.class)
public class MixinStringDecomposer {
    @ModifyVariable(method = "iterate", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static String nickIterate(String string) {
        return NickHiderModule.replace(string);
    }

    @ModifyVariable(method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static String nickIterateFormatted(String string) {
        return NickHiderModule.replace(string);
    }
}
