package dev.sinepsinem.util.text;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

/**
 * Surrounds a glyph with a transparent 1px border in the atlas, so bilinear filtering never picks
 * up the neighbouring glyph.
 */
public record PaddedGlyphBitmap(GlyphBitmap glyph) implements GlyphBitmap {
    private static final int PADDING = 1;

    @Override
    public int getPixelWidth() {
        return glyph.getPixelWidth() + PADDING * 2;
    }

    @Override
    public int getPixelHeight() {
        return glyph.getPixelHeight() + PADDING * 2;
    }

    @Override
    public void upload(int x, int y, GpuTexture texture) {
        // atlas contents start out undefined, clear the border first
        NativeImage.Format format = glyph.isColored() ? NativeImage.Format.RGBA : NativeImage.Format.LUMINANCE;
        try (NativeImage clear = new NativeImage(format, getPixelWidth(), getPixelHeight(), true)) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, clear, 0, 0, x, y);
        }
        glyph.upload(x + PADDING, y + PADDING, texture);
    }

    @Override
    public boolean isColored() {
        return glyph.isColored();
    }

    @Override
    public float getOversample() {
        return glyph.getOversample();
    }

    @Override
    public float getBearingLeft() {
        return glyph.getBearingLeft() - PADDING / getOversample();
    }

    @Override
    public float getBearingTop() {
        return glyph.getBearingTop() + PADDING / getOversample();
    }
}
