package dev.sinepsinem.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.event.impl.render.Render2DEvent;
import dev.sinepsinem.features.gui.ClickGuiScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import dev.sinepsinem.event.impl.render.Render3DEvent;
import dev.sinepsinem.features.Feature;
import dev.sinepsinem.features.commands.ModuleCommand;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.modules.client.*;
import dev.sinepsinem.features.modules.combat.*;
import dev.sinepsinem.features.modules.ghost.*;
import dev.sinepsinem.features.modules.hud.*;
import dev.sinepsinem.features.modules.misc.*;
import dev.sinepsinem.features.modules.movement.*;
import dev.sinepsinem.features.modules.player.*;
import dev.sinepsinem.features.modules.render.*;
import dev.sinepsinem.util.traits.Jsonable;
import dev.sinepsinem.util.traits.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class ModuleManager implements Jsonable, Util {
    private static final Logger LOGGER = LoggerFactory.getLogger("ModuleManager");

    private final Map<Class<? extends Module>, Module> fastRegistry = new HashMap<>();
    private final List<Module> modules = new ArrayList<>();
    // the last loaded config, script modules pick their settings from it when they register
    private JsonObject saved;

    public void init() {
        // in panel order, the ClickGui lists every category's modules in the order they are registered

        // combat
        register(new KillAuraModule());
        register(new TargetStrafeModule());
        register(new CriticalsModule());
        register(new AutoTotemModule());
        register(new MaceSwapModule());
        register(new AutoSpearModule());
        register(new AutoBreezeChargeModule());
        register(new KeyPearlModule());
        // ghost
        register(new HitboxesModule());
        register(new SprintResetModule());
        // movement
        register(new SprintModule());
        register(new MovementFixModule());
        register(new KeepSprintModule());
        register(new SpeedModule());
        register(new FlightModule());
        register(new StepModule());
        register(new ReverseStepModule());
        register(new FastAccelModule());
        register(new NoPushModule());
        register(new NoDelayModule());
        // render
        register(new ESPModule());
        register(new NameTagsModule());
        register(new StorageESPModule());
        register(new TracersModule());
        register(new TrajectoriesModule());
        register(new AnimationsModule());
        register(new BlockHighlightModule());
        register(new GlowParticlesModule());
        // player
        register(new VelocityModule());
        register(new NoFallModule());
        register(new FastPlaceModule());
        register(new AutoToolModule());
        register(new NoRotateModule());
        register(new AutoRespawnModule());
        // misc
        register(new AntiBotModule());
        register(new TeamsModule());
        register(new AntiCheatModule());
        register(new NickHiderModule());
        register(new StaffDetectorModule());
        register(new LagAlertModule());
        register(new ClientSpooferModule());
        register(new ResourcePackSpoofModule());
        register(new CompactChatModule());
        register(new BaseFinderModule());
        // client
        register(new ClickGuiModule());
        register(new InterfaceModule());
        register(new HudEditorModule());
        register(new NotificationsModule());
        // hud
        register(new WatermarkHudModule());
        register(new ArrayListHudModule());
        register(new InfoHudModule());
        register(new SessionInfoHudModule());
        register(new TargetHudModule());
        register(new KeybindsHudModule());
        register(new SpeedGraphHudModule());
        register(new InventoryHudModule());
        register(new RadarHudModule());
        register(new PotionsHudModule());

        LOGGER.info("Registered {} modules", modules.size());

        // on by default, the saved config decides afterwards
        getModuleByClass(WatermarkHudModule.class).enable();
        getModuleByClass(ArrayListHudModule.class).enable();
        getModuleByClass(TargetHudModule.class).enable();
        getModuleByClass(InterfaceModule.class).enable();

        // Create a command for each module for modules to be configurable via command line
        for (Module module : modules) {
            SinepSinem.commandManager.register(new ModuleCommand(module));
        }

        SinepSinem.configManager.addConfig(this);
    }

    public void register(Module module) {
        getModules().add(module);
        fastRegistry.put(module.getClass(), module);
    }

    /**
     * Script modules come and go at runtime, their saved settings wait for them in {@link #saved}.
     */
    public void registerScriptModule(Module module) {
        getModules().add(module);
        if (saved != null && saved.has(module.getName())) module.fromJson(saved.get(module.getName()));
    }

    public void unregisterScriptModule(Module module) {
        if (module.isEnabled()) module.disable();
        if (saved == null) saved = new JsonObject();
        saved.add(module.getName(), module.toJson());
        getModules().remove(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public Stream<Module> stream() {
        return getModules().stream();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModuleByClass(Class<T> clazz) {
        return (T) fastRegistry.get(clazz);
    }

    public Module getModuleByName(String name) {
        return stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public Module getModuleByDisplayName(String display) {
        return stream().filter(m -> m.getDisplayName().equalsIgnoreCase(display)).findFirst().orElse(null);
    }

    public List<Module> getModulesByCategory(Module.Category category) {
        return stream().filter(m -> m.getCategory() == category).toList();
    }

    public List<Module.Category> getCategories() {
        return Arrays.asList(Module.Category.values());
    }

    public void onLoad() {
        getModules().forEach(Module::onLoad);
    }

    public void onTick() {
        stream().filter(Feature::isEnabled).forEach(Module::onTick);
    }

    public void onRender2D(Render2DEvent event) {
        stream().filter(Feature::isEnabled).forEach(module -> module.onRender2D(event));
    }

    public void onRender3D(Render3DEvent event) {
        stream().filter(Feature::isEnabled).forEach(module -> module.onRender3D(event));
    }

    public void onUnload() {
        getModules().forEach(EVENT_BUS::unregister);
        getModules().forEach(Module::onUnload);
    }

    public void onKeyPressed(int key) {
        if (key <= 0) return;
        if (mc.gui.screen() instanceof TitleScreen || mc.gui.screen() instanceof JoinMultiplayerScreen) {
            if (ClickGuiModule.getInstance().getBind().getKey() == key) {
                ClickGuiScreen.open(mc.gui.screen());
            }
            return;
        }
        if (mc.gui.screen() != null) return;
        stream().filter(module -> module.getBind().getKey() == key).forEach(Module::toggle);
    }

    public void onMouseClicked(int button) {
        if (mc.gui.screen() != null) return;
        int key = -button - 2;
        stream().filter(module -> module.getBind().getKey() == key).forEach(Module::toggle);
    }

    @Override
    public JsonElement toJson() {
        // keeps the settings of script modules that are not loaded right now
        JsonObject object = saved == null ? new JsonObject() : saved.deepCopy();
        for (Module module : getModules()) {
            object.add(module.getName(), module.toJson());
        }
        return object;
    }

    @Override
    public void fromJson(JsonElement element) {
        saved = element.getAsJsonObject();
        for (Module module : getModules()) {
            module.fromJson(element.getAsJsonObject().get(module.getName()));
        }
    }

    @Override
    public String getFileName() {
        return "modules.json";
    }
}
