package dev.sinepsinem.mixin.network;

import dev.sinepsinem.features.modules.misc.ClientSpooferModule;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public class MixinClientBrandRetriever {
    @Inject(method = "getClientModName", at = @At("HEAD"), cancellable = true)
    private static void spoofBrand(CallbackInfoReturnable<String> cir) {
        String brand = ClientSpooferModule.brand();
        if (brand != null) cir.setReturnValue(brand);
    }
}
