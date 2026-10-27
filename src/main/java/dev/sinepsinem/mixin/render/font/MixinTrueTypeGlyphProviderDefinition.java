package dev.sinepsinem.mixin.render.font;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.util.text.Fonts;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads the user's SF Pro Medium in place of the bundled Inter Medium. Apple ships SF Pro as
 * CFF flavoured otf, which FreeType reads fine but vanilla refuses because it only allows TrueType.
 */
@Mixin(TrueTypeGlyphProviderDefinition.class)
public class MixinTrueTypeGlyphProviderDefinition {
    @Shadow @Final private Identifier location;

    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/ResourceManager;open(Lnet/minecraft/resources/Identifier;)Ljava/io/InputStream;"))
    private InputStream sfProOverride(ResourceManager manager, Identifier id, Operation<InputStream> original) throws IOException {
        if (Fonts.BUNDLED_FILE.equals(location)) {
            Path override = Fonts.sfProOverride();
            if (override != null) {
                SinepSinem.LOGGER.info("Using {} for the vanilla text renderer", override.getFileName());
                return Files.newInputStream(override);
            }
        }
        return original.call(manager, id);
    }

    @ModifyExpressionValue(method = "load", at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"))
    private boolean allowOpenType(boolean trueType) {
        return trueType || Fonts.BUNDLED_FILE.equals(location);
    }
}
