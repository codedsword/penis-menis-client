package dev.sinepsinem.features.modules.render;

import dev.sinepsinem.event.impl.render.Render3DEvent;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.modules.combat.KillAuraModule;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.TargetUtil;
import dev.sinepsinem.util.render.RenderUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

/**
 * Highlights players, bots flagged by AntiBot are skipped.
 */
public class ESPModule extends Module {
    private static ESPModule INSTANCE;

    private final Setting<Mode> mode = mode("Mode", Mode.BOX);
    private final Setting<Boolean> players = bool("Players", true);
    private final Setting<Boolean> hostiles = bool("Hostiles", false);
    private final Setting<Boolean> passives = bool("Passives", false);
    private final Setting<Color> color = color("Color", 230, 98, 163, 255);
    private final Setting<Color> targetColor = color("TargetColor", 248, 113, 113, 255);
    private final Setting<Float> lineWidth = num("LineWidth", 2.0f, 0.5f, 5.0f);
    private final Setting<Integer> fillAlpha = num("FillAlpha", 40, 0, 255);

    public ESPModule() {
        super("ESP", "Highlights entities through walls", Category.RENDER);
        lineWidth.setVisibility(v -> mode.getValue() == Mode.BOX);
        fillAlpha.setVisibility(v -> mode.getValue() == Mode.BOX);
        INSTANCE = this;
    }

    public static boolean shouldGlow(Entity entity) {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.mode.getValue() == Mode.GLOW && INSTANCE.shouldRender(entity);
    }

    public static int glowColor(Entity entity) {
        return INSTANCE.colorFor(entity).getRGB() & 0xFFFFFF;
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (mode.getValue() != Mode.BOX) return;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!shouldRender(entity)) continue;
            Vec3 lerped = entity.getPosition(event.getDelta());
            AABB box = entity.getBoundingBox().move(lerped.subtract(entity.position()));
            Color outline = colorFor(entity);
            Color fill = new Color(outline.getRed(), outline.getGreen(), outline.getBlue(), fillAlpha.getValue());
            RenderUtil.drawBox(box, outline, fill, lineWidth.getValue());
        }
    }

    private boolean shouldRender(Entity entity) {
        return TargetUtil.isValidTarget(entity, players.getValue(), hostiles.getValue(), passives.getValue());
    }

    private Color colorFor(Entity entity) {
        return entity == KillAuraModule.getTarget() ? targetColor.getValue() : color.getValue();
    }

    @Override
    public String getDisplayInfo() {
        return dev.sinepsinem.features.settings.EnumConverter.getProperName(mode.getValue());
    }

    public enum Mode {
        BOX,
        GLOW
    }
}
