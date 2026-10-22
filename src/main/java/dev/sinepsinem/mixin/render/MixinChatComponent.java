package dev.sinepsinem.mixin.render;

import dev.sinepsinem.ducks.render.IChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent implements IChatComponent {
    @Final @Shadow
    private List<GuiMessage> allMessages;

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
}