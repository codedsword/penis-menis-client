package dev.sinepsinem.mixin.render;

import dev.sinepsinem.ducks.render.IChatComponent;
import dev.sinepsinem.features.gui.hud.CustomChat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static dev.sinepsinem.util.traits.Util.mc;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent implements IChatComponent {
    @Unique
    private static final String EXTRACT_ACCESS = "Lnet/minecraft/client/gui/components/ChatComponent;extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V";
    @Unique
    private static final String EXTRACT_GRAPHICS = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V";

    @Final @Shadow
    private List<GuiMessage> allMessages;

    @Shadow
    private int chatScrollbarPos;

    @Unique
    private GuiGraphicsExtractor sinepsinem$graphics;
    @Unique
    private boolean sinepsinem$refreshing;

    @Shadow
    private void refreshTrimmedMessages() {}

    @Override
    public void sinepsinem$addMessage(GuiMessage message) {
        this.allMessages.addFirst(message);
        this.refreshTrimmedMessages();
    }

    @Override
    public void sinepsinem$removeMessage(MessageSignature signature) {
        this.allMessages.removeIf(message -> signature.equals(message.signature()));
        this.refreshTrimmedMessages();
    }

    @Inject(method = EXTRACT_GRAPHICS, at = @At("HEAD"))
    private void captureGraphics(GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY,
                                 ChatComponent.DisplayMode displayMode, boolean changeCursorOnInsertions, CallbackInfo ci) {
        this.sinepsinem$graphics = graphics;
    }

    // all chat drawing goes through this access, the custom chat sits in front of vanilla's
    @ModifyArg(method = EXTRACT_GRAPHICS, at = @At(value = "INVOKE", target = EXTRACT_ACCESS), index = 0)
    private ChatComponent.ChatGraphicsAccess smoothChat(ChatComponent.ChatGraphicsAccess access) {
        GuiGraphicsExtractor graphics = this.sinepsinem$graphics;
        this.sinepsinem$graphics = null;
        return CustomChat.INSTANCE.isActive() ? CustomChat.INSTANCE.wrap(access, graphics, graphics.guiHeight()) : access;
    }

    // clicks and hovers must find the text where it is drawn
    @ModifyArg(method = "captureClickableText", at = @At(value = "INVOKE", target = EXTRACT_ACCESS), index = 0)
    private ChatComponent.ChatGraphicsAccess smoothChatClicks(ChatComponent.ChatGraphicsAccess access) {
        return CustomChat.INSTANCE.isActive() ? CustomChat.INSTANCE.wrap(access, null, mc.getWindow().getGuiScaledHeight()) : access;
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V"))
    private void slideIn(GuiMessage message, CallbackInfo ci) {
        if (!this.sinepsinem$refreshing && this.chatScrollbarPos == 0) {
            CustomChat.INSTANCE.onLineAdded();
        }
    }

    // rewrapping every message (rescale, deletions) is not new chat
    @Inject(method = "refreshTrimmedMessages", at = @At("HEAD"))
    private void refreshStart(CallbackInfo ci) {
        this.sinepsinem$refreshing = true;
    }

    @Inject(method = "refreshTrimmedMessages", at = @At("RETURN"))
    private void refreshEnd(CallbackInfo ci) {
        this.sinepsinem$refreshing = false;
    }
}
