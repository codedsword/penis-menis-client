package dev.sinepsinem.features.modules.misc;

import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;

/**
 * Changes the client brand the server sees (it asks right after you join).
 */
public class ClientSpooferModule extends Module {
    private static ClientSpooferModule INSTANCE;

    public enum Brand {
        VANILLA("vanilla"),
        LUNAR("lunarclient:v2.18.3-2451"),
        FABRIC("fabric"),
        FORGE("forge"),
        BADLION("badlion"),
        FEATHER("feather");

        final String brand;

        Brand(String brand) {
            this.brand = brand;
        }
    }

    private final Setting<Brand> mode = mode("Mode", Brand.LUNAR);

    public ClientSpooferModule() {
        super("ClientSpoofer", "Pretends to be another client", Category.MISC);
        INSTANCE = this;
    }

    /**
     * @return the brand to send, null to send the real one
     */
    public static String brand() {
        ClientSpooferModule module = INSTANCE;
        return module != null && module.isEnabled() ? module.mode.getValue().brand : null;
    }
}
