package dev.sinepsinem.features.modules.hud;

import dev.sinepsinem.event.impl.render.Render2DEvent;
import dev.sinepsinem.features.modules.client.HudModule;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.BuildConfig;
import dev.sinepsinem.util.TextUtil;
import net.minecraft.network.chat.Component;

public class WatermarkHudModule extends HudModule {
    public final Setting<String> text = str("Text", BuildConfig.NAME);
    public final Setting<Boolean> fullVersion = new Setting<>("FullVersion", false);

    public WatermarkHudModule() {
        super("Watermark", "Display watermark", 100, 10);
        if (BuildConfig.USING_GIT) {
            register(fullVersion);
        }
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);
        String watermarkString = "{global} %s {} %s";
        if (fullVersion.getValue() && BuildConfig.USING_GIT) {
            watermarkString += "/" + BuildConfig.BRANCH + "-" + BuildConfig.HASH;
        }

        Component component = TextUtil.text(watermarkString, text.getValue(), BuildConfig.VERSION);
        e.getContext().text(mc.font, component, (int) getX(), (int) getY(), -1);

        setWidth(mc.font.width(component));
        setHeight(mc.font.lineHeight);
    }
}

