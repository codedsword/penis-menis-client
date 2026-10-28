package dev.sinepsinem.features.scripting;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.features.commands.Command;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static dev.sinepsinem.features.commands.MessageSignatures.GENERAL;

/**
 * Loading, reloading and unloading the .lua files in the scripts folder. Errors never escape, they
 * go to chat and the log.
 */
public class Scripts {
    private final Path directory;
    private final Map<String, Script> loaded = new LinkedHashMap<>();

    public Scripts(Path directory) {
        this.directory = directory;
    }

    public Path directory() {
        return directory;
    }

    public Collection<Script> loaded() {
        return loaded.values();
    }

    /**
     * @return the .lua files in the folder, without the extension
     */
    public List<String> available() {
        if (!Files.isDirectory(directory)) return List.of();
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(".lua"))
                    .map(name -> name.substring(0, name.length() - 4))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static String normalize(String name) {
        return name.endsWith(".lua") ? name.substring(0, name.length() - 4) : name;
    }

    public @Nullable Script get(String name) {
        return loaded.get(normalize(name));
    }

    /**
     * @param report whether to say so in chat (not while loading the config at startup)
     * @return whether the script runs now
     */
    public boolean load(String name, boolean report) {
        name = normalize(name);
        if (loaded.containsKey(name)) {
            if (report) Command.sendMessage("{red} %s {reset} is already loaded, use reload", GENERAL, name);
            return false;
        }
        Path file = directory.resolve(name + ".lua");
        if (!Files.isRegularFile(file)) {
            if (report) Command.sendMessage("{red} No script %s in the scripts folder", GENERAL, name + ".lua");
            return false;
        }
        Script script = new Script(name, file);
        try {
            script.run();
        } catch (Throwable t) {
            // whatever the script registered before failing goes away again
            script.unregisterAll();
            script.error("load", t);
            return false;
        }
        loaded.put(name, script);
        if (report) {
            Command.sendMessage("Loaded {global} %s {reset} (%s modules)", GENERAL, name, script.getModules().size());
        }
        return true;
    }

    public boolean unload(String name, boolean report) {
        Script script = loaded.remove(normalize(name));
        if (script == null) {
            if (report) Command.sendMessage("{red} %s {reset} is not loaded", GENERAL, normalize(name));
            return false;
        }
        script.unregisterAll();
        if (report) Command.sendMessage("Unloaded {global} %s", GENERAL, script.getName());
        return true;
    }

    public boolean reload(String name) {
        boolean wasLoaded = unload(name, false);
        boolean ok = load(name, false);
        if (ok) Command.sendMessage("%s {global} %s", GENERAL, wasLoaded ? "Reloaded" : "Loaded", normalize(name));
        return ok;
    }

    /**
     * Puts an example next to the scripts on first start.
     */
    public void createExample() {
        try {
            Files.createDirectories(directory);
            Path example = directory.resolve("example.lua");
            if (Files.exists(example)) return;
            try (InputStream in = Scripts.class.getResourceAsStream("/assets/sinepsinem/scripts/example.lua")) {
                if (in != null) Files.copy(in, example);
            }
        } catch (IOException e) {
            SinepSinem.LOGGER.error("Failed to create the scripts folder", e);
        }
    }
}
