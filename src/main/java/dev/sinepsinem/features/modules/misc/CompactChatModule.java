package dev.sinepsinem.features.modules.misc;

import dev.sinepsinem.features.modules.Module;

/**
 * Stacks repeated chat lines into one with a counter, "message [x3]".
 */
public class CompactChatModule extends Module {
    private static CompactChatModule INSTANCE;

    public CompactChatModule() {
        super("CompactChat", "Merges repeated chat messages", Category.MISC);
        INSTANCE = this;
    }

    public static boolean active() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }
}
