package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.event.impl.render.Render2DEvent;
import dev.sinepsinem.features.modules.client.HudModule;
import dev.sinepsinem.features.settings.Setting;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class CoordinatesHudModule extends HudModule {
    public final Setting<Boolean> nether = bool("Nether", false);

    public CoordinatesHudModule() {
        super("Coordinates", "Display coordinates", 150, 20);
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);

        if (nullCheck()) return;

        String coordsStr = String.format("X: %d Y: %d Z: %d",
                mc.player.getBlockX(),
                mc.player.getBlockY(),
                mc.player.getBlockZ());

        if (nether.getValue()) {
            ResourceKey<Level> dimension = mc.player.level().dimension();
            if (dimension == Level.NETHER) {
                coordsStr += String.format(" [%d, %d]", mc.player.getBlockX() * 8, mc.player.getBlockZ() * 8);
            } else if (dimension == Level.OVERWORLD) {
                coordsStr += String.format(" [%d, %d]", Math.floorDiv(mc.player.getBlockX(), 8), Math.floorDiv(mc.player.getBlockZ(), 8));
            }
        }

        e.getContext().text(mc.font, coordsStr,
                (int) getX(), (int) getY(), -1);

        setWidth(mc.font.width(coordsStr));
        setHeight(mc.font.lineHeight);
    }
}

