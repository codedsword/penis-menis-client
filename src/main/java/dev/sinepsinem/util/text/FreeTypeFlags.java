package dev.sinepsinem.util.text;

/**
 * Vanilla loads ttf glyphs with FreeType's default (full) hinting, which snaps outlines to the pixel
 * grid and makes small text look uneven. Light hinting only snaps vertically, keeping the glyph
 * shapes, so text renders smoother.
 */
public final class FreeTypeFlags {
    private static final int TARGET_MASK = 0xF << 16;
    private static final int FT_LOAD_TARGET_LIGHT = 1 << 16;

    private FreeTypeFlags() {
    }

    public static int smooth(int flags) {
        return (flags & ~TARGET_MASK) | FT_LOAD_TARGET_LIGHT;
    }
}
