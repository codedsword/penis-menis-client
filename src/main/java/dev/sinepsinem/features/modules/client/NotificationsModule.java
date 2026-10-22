package dev.sinepsinem.features.modules.client;

import dev.sinepsinem.event.impl.ClientEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.TextUtil;
import dev.sinepsinem.util.chat.ChatUtil;

import static dev.sinepsinem.util.chat.SimpleSignature.from;

public class NotificationsModule extends Module {
    private static final String MODULE_FORMAT = "{gray} %s %s%s";

    public final Setting<Boolean> moduleToggle = bool("ModuleToggle", true);

    public NotificationsModule() {
        super("Notifications", "Displays notifications for various client events", Category.CLIENT);
    }

    @Subscribe
    public void onClient(ClientEvent event) {
        if (!moduleToggle.getValue()
                || event.getType() != ClientEvent.Type.TOGGLE_MODULE
                || event.getFeature() instanceof ClickGuiModule) {
            return;
        }

        boolean moduleState = event.getFeature().isEnabled();
        String name = event.getFeature() instanceof Module module
                ? module.getDisplayName()
                : event.getFeature().getName();

        ChatUtil.sendMessage(TextUtil.text(MODULE_FORMAT,
                name,
                moduleState ? "{green}" : "{red}",
                moduleState ? "on" : "off"), from(event.getFeature().getName()));
    }
}
