package dev.sinepsinem.mixin.render.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sinepsinem.features.gui.hud.CustomChat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChatScreen.class)
public class MixinChatScreen {
    // the input box background
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V", ordinal = 0))
    private void glassInput(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
        if (CustomChat.INSTANCE.isActive()) {
            CustomChat.INSTANCE.inputBox(graphics, x1, y1, x2, y2);
        } else {
            original.call(graphics, x1, y1, x2, y2, color);
        }
    }
}
