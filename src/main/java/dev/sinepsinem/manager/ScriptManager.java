package dev.sinepsinem.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.features.scripting.Scripts;
import dev.sinepsinem.util.traits.Jsonable;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * Owns the scripts folder and remembers which scripts were loaded, so they load again next start.
 */
public class ScriptManager implements Jsonable {
    public static final Path DIRECTORY = FabricLoader.getInstance().getGameDir().resolve(SinepSinem.MOD_ID).resolve("scripts");

    private final Scripts scripts = new Scripts(DIRECTORY);

    public void init() {
        scripts.createExample();
        SinepSinem.configManager.addConfig(this);
    }

    public Scripts scripts() {
        return scripts;
    }

    @Override
    public JsonElement toJson() {
        JsonArray loaded = new JsonArray();
        scripts.loaded().forEach(script -> loaded.add(script.getName()));
        return loaded;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonArray()) return;
        for (JsonElement name : element.getAsJsonArray()) {
            scripts.load(name.getAsString(), false);
        }
    }

    @Override
    public String getFileName() {
        return "scripts.json";
    }
}
