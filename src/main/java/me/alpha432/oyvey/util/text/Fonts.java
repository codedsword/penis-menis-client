package me.alpha432.oyvey.util.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * Product Sans (bundled as Google Sans) registered as vanilla ttf fonts, see assets/oyvey/font.
 */
public final class Fonts {
    public static final FontDescription REGULAR = font("product_sans");
    public static final FontDescription BOLD = font("product_sans_bold");
    public static final FontDescription TITLE = font("product_sans_title");

    private Fonts() {
    }

    public static MutableComponent text(String text) {
        return Component.literal(text).withStyle(s -> s.withFont(REGULAR));
    }

    public static MutableComponent bold(String text) {
        return Component.literal(text).withStyle(s -> s.withFont(BOLD));
    }

    public static MutableComponent title(String text) {
        return Component.literal(text).withStyle(s -> s.withFont(TITLE));
    }

    public static MutableComponent withFont(Component component, FontDescription font) {
        return component.copy().withStyle(s -> s.withFont(font));
    }

    private static FontDescription font(String name) {
        return new FontDescription.Resource(Identifier.fromNamespaceAndPath("oyvey", name));
    }
}
