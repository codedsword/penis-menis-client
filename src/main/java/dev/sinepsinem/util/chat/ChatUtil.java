package dev.sinepsinem.util.chat;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.ducks.render.IChatComponent;
import dev.sinepsinem.features.commands.Command;
import dev.sinepsinem.util.BuildConfig;
import dev.sinepsinem.util.TextUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import static dev.sinepsinem.util.traits.Util.mc;

public class ChatUtil {
    public static MutableComponent prefix() {
        return withPrefix((Component) null);
    }

    public static MutableComponent withPrefix(String string) {
        return withPrefix(string, new Object[0]);
    }

    public static MutableComponent withPrefix(String string, Object... o) {
        return withPrefix(TextUtil.text(string, o));
    }

    // Ambient's prefix: the client name in the accent, then a gray guillemet
    public static MutableComponent withPrefix(Component component) {
        MutableComponent text = Component.empty()
                .append(getClientNameComponent())
                .append(Component.literal(" \u00bb").withStyle(ChatFormatting.DARK_GRAY));
        if (component != null) text.append(" ").append(component);
        return text;
    }

    public static void sendMessage(Component message, Signature identifier) {
        sendClientSideMessage(withPrefix(message), identifier);
    }

    public static void sendClientSideMessage(Component message, Signature sig) {
        if (Command.nullCheck()) return;

        IChatComponent chat = (IChatComponent) mc.gui.hud.getChat();
        MessageSignature signature = new MessageSignature(sig.getByteSignature());
        chat.sinepsinem$removeMessage(signature);
        chat.sinepsinem$addMessage(new GuiMessage(mc.gui.hud.getGuiTicks(), message, signature, GuiMessageSource.SYSTEM_CLIENT, getMessageTag()));
    }

    public static Component getClientNameComponent() {
        return Component.empty().withColor(SinepSinem.colorManager.getColorAsInt()).append(BuildConfig.NAME);
    }

    private static GuiMessageTag getMessageTag() {
        return new GuiMessageTag(SinepSinem.colorManager.getColorAsInt(), null, null, null);
    }
}