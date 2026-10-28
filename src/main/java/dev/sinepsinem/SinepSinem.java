package dev.sinepsinem;

import dev.sinepsinem.manager.*;
import dev.sinepsinem.util.BuildConfig;
import dev.sinepsinem.util.TextUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SinepSinem implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "sinepsinem";
    public static float TIMER = 1f;

    public static final Logger LOGGER = LogManager.getLogger(BuildConfig.NAME);
    public static ServerManager serverManager;
    public static ColorManager colorManager;
    public static RotationManager rotationManager;
    public static PositionManager positionManager;
    public static HoleManager holeManager;
    public static EventManager eventManager;
    public static SpeedManager speedManager;
    public static CommandManager commandManager;
    public static ModuleManager moduleManager;
    public static ConfigManager configManager;
    public static AltManager altManager;
    public static SessionManager sessionManager;
    public static ScriptManager scriptManager;

    @Override
    public void onInitialize() {
        LOGGER.info("Pre-initializing {} v{}",
                BuildConfig.NAME, BuildConfig.VERSION);
        configManager = new ConfigManager();
        eventManager = new EventManager();
        serverManager = new ServerManager();
        rotationManager = new RotationManager();
        positionManager = new PositionManager();
        colorManager = new ColorManager();
        commandManager = new CommandManager();
        moduleManager = new ModuleManager();
        speedManager = new SpeedManager();
        holeManager = new HoleManager();
        altManager = new AltManager();
        sessionManager = new SessionManager();
        scriptManager = new ScriptManager();

        TextUtil.init();
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {}", BuildConfig.NAME);

        long startTime = System.nanoTime();

        eventManager.init();
        commandManager.init();
        moduleManager.init();
        altManager.init();
        sessionManager.init();
        scriptManager.init();

        configManager.load();
        moduleManager.onLoad();
        colorManager.init();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> configManager.save()));

        // -Dsinepsinem.mixinAudit=true applies every mixin at startup instead of on first class load
        if (Boolean.getBoolean("sinepsinem.mixinAudit")) {
            org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
        }

        long endTime = System.nanoTime();

        LOGGER.info("Initialized {} in {}ms",
                BuildConfig.NAME, (endTime - startTime) / 1000000.0);
    }
}
