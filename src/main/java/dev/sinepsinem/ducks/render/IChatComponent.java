package dev.sinepsinem.ducks.render;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.MessageSignature;

public interface IChatComponent {
    void sinepsinem$addMessage(GuiMessage message);
    void sinepsinem$removeMessage(MessageSignature signature);
}