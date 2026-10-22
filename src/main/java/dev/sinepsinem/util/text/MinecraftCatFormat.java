package dev.sinepsinem.util.text;

import dev.cattyn.catformat.CatFormatImpl;
import dev.cattyn.catformat.text.Modifier;
import dev.cattyn.catformat.text.TextWrapper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import java.util.Locale;

/**
 * Inlined port of catformat-fabric, whose published jar is intermediary-mapped
 * and cannot load on unobfuscated (26.x) Minecraft.
 */
public class MinecraftCatFormat extends CatFormatImpl<MutableComponent> {
    public MinecraftCatFormat(boolean vanilla) {
        super(new Wrapper());
        if (vanilla) {
            for (ChatFormatting value : ChatFormatting.values()) {
                TextColor color = TextColor.fromLegacyFormat(value);
                if (color == null) continue;
                add(value.name().toLowerCase(Locale.ROOT), color.getValue());
            }
        }
    }

    private static class Wrapper implements TextWrapper<MutableComponent> {
        @Override
        public MutableComponent colored(MutableComponent text, int color) {
            return text.withColor(color);
        }

        @Override
        public MutableComponent concat(MutableComponent text, MutableComponent text2) {
            return text.append(text2);
        }

        @Override
        public MutableComponent modify(MutableComponent text, int modifiers) {
            if (Modifier.BOLD.isIn(modifiers)) text.withStyle(ChatFormatting.BOLD);
            if (Modifier.ITALIC.isIn(modifiers)) text.withStyle(ChatFormatting.ITALIC);
            if (Modifier.UNDERLINE.isIn(modifiers)) text.withStyle(ChatFormatting.UNDERLINE);
            if (Modifier.STRIKETHROUGH.isIn(modifiers)) text.withStyle(ChatFormatting.STRIKETHROUGH);
            if (Modifier.OBFUSCATED.isIn(modifiers)) text.withStyle(ChatFormatting.OBFUSCATED);
            return text;
        }

        @Override
        public MutableComponent newText(String content) {
            return Component.literal(content);
        }
    }
}
