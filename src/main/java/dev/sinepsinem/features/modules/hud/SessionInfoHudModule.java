package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.manager.SessionManager;
import dev.sinepsinem.util.imgui.ImGuiImpl;
import dev.sinepsinem.util.imgui.Theme;
import imgui.ImDrawList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import org.joml.Vector2f;

/**
 * Ambient's "Session Information": your head, name, time played, a health bar in the theme
 * gradient, and kills and deaths since joining.
 */
public class SessionInfoHudModule extends ImGuiHudModule {
    private static final float WIDTH = 230, BODY = 64, HEAD = 44, PAD = 10;

    private float health = 1;

    public SessionInfoHudModule() {
        super("SessionInfo", "Play time, kills and deaths", 115, 45);
        pos.setValue(new Vector2f(0.004f, 0.42f));
    }

    @Override
    protected float[] draw(ImDrawList draw, float x, float y) {
        float top = panel(draw, x, y, WIDTH, ImGuiHudModule.TITLE + BODY, "Session Information");
        float tx = x + PAD + HEAD + 10, right = x + WIDTH - PAD;

        text(draw, ImGuiImpl.regular, tx, top + 7, Theme.TEXT, mc.player.getName().getString());
        String time = SessionManager.formatDuration(SinepSinem.sessionManager.playTimeMillis());
        text(draw, ImGuiImpl.regular, right - width(ImGuiImpl.regular, time), top + 7, Theme.TEXT, time);

        float target = (mc.player.getHealth() + mc.player.getAbsorptionAmount()) / mc.player.getMaxHealth();
        health += (target - health) * 0.15f;
        bar(draw, tx, top + 29, right - tx, 5, health);

        String kills = SinepSinem.sessionManager.kills() + " Kills", deaths = SinepSinem.sessionManager.deaths() + " Deaths";
        text(draw, ImGuiImpl.small, tx, top + 41, Theme.TEXT_DIM, kills);
        text(draw, ImGuiImpl.small, right - width(ImGuiImpl.small, deaths), top + 41, Theme.TEXT_DIM, deaths);
        return new float[]{WIDTH, ImGuiHudModule.TITLE + BODY};
    }

    @Override
    protected void drawVanilla(GuiGraphicsExtractor graphics, float x, float y) {
        int size = Math.round(HEAD / scale);
        PlayerFaceExtractor.extractRenderState(graphics, mc.player.getSkin(),
                Math.round(x + PAD / scale), Math.round(y + (ImGuiHudModule.TITLE + (BODY - HEAD) / 2) / scale), size);
    }
}
