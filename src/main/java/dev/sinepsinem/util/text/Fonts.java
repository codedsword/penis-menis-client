package dev.sinepsinem.util.text;

import dev.sinepsinem.SinepSinem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * SF Pro Medium, used for everything: ImGui, the hud, and (see MixinFontManager) every piece of
 * vanilla text drawn with the default font.
 * <p>
 * SF Pro is Apple's and may not be redistributed, so the jar ships Inter Medium (SIL OFL 1.1, see
 * assets/sinepsinem/font/ofl.txt), which was drawn after it. Dropping one of {@link #SF_PRO_FILES}
 * into .minecraft/sinepsinem/fonts swaps the real SF Pro Medium in for both renderers.
 */
public final class Fonts {
    public static final FontDescription MEDIUM = font("sf_pro");
    public static final FontDescription TITLE = font("sf_pro_title");

    /** the ttf the vanilla font definitions point at, replaced by the SF Pro override when there is one */
    public static final Identifier BUNDLED_FILE = Identifier.fromNamespaceAndPath(SinepSinem.MOD_ID, "inter-medium.ttf");

    public static final Path OVERRIDE_DIR = FabricLoader.getInstance().getGameDir().resolve(SinepSinem.MOD_ID).resolve("fonts");
    // Apple ships SF Pro as otf, the names are what the official download installs
    private static final List<String> SF_PRO_FILES = List.of(
            "SF-Pro-Text-Medium.otf", "SF-Pro-Display-Medium.otf", "SF-Pro-Medium.otf",
            "SFProText-Medium.otf", "SFProDisplay-Medium.otf",
            "SF-Pro-Text-Medium.ttf", "SF-Pro-Display-Medium.ttf", "SFProText-Medium.ttf", "SFProDisplay-Medium.ttf");

    private Fonts() {
    }

    /**
     * @return the user's SF Pro Medium if they dropped one into the fonts folder
     */
    public static @Nullable Path sfProOverride() {
        for (String name : SF_PRO_FILES) {
            Path file = OVERRIDE_DIR.resolve(name);
            if (Files.isRegularFile(file)) return file;
        }
        return null;
    }

    public static MutableComponent text(String text) {
        return Component.literal(text).withStyle(s -> s.withFont(MEDIUM));
    }

    public static MutableComponent title(String text) {
        return Component.literal(text).withStyle(s -> s.withFont(TITLE));
    }

    public static MutableComponent withFont(Component component, FontDescription font) {
        return component.copy().withStyle(s -> s.withFont(font));
    }

    private static FontDescription font(String name) {
        return new FontDescription.Resource(Identifier.fromNamespaceAndPath(SinepSinem.MOD_ID, name));
    }
}
