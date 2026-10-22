package me.alpha432.oyvey.util;

import dev.cattyn.catformat.CatFormat;
import me.alpha432.oyvey.util.text.MinecraftCatFormat;
import me.alpha432.oyvey.OyVey;
import net.minecraft.network.chat.MutableComponent;

public final class TextUtil {
    private static CatFormat<MutableComponent> formatter;

    private TextUtil() {
        throw new AssertionError("Can't create an instance of utility class");
    }

    public static void init() {
        if (formatter != null) throw new IllegalStateException("Formatter is already initialized");
        formatter = new MinecraftCatFormat(true);
        initColors();
    }

    public static MutableComponent text(String content, Object... obj) {
        if (obj == null || obj.length == 0) {
            return formatter.format(content);
        }
        return formatter.format(content, obj);
    }

    public static CatFormat<MutableComponent> getFormatter() {
        return formatter;
    }

    private static void initColors() {
        formatter.add("global", () -> OyVey.colorManager.getColorAsInt());
    }
}
