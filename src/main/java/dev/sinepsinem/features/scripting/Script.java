package dev.sinepsinem.features.scripting;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.util.chat.ChatUtil;
import dev.sinepsinem.util.chat.SimpleSignature;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * One loaded .lua file: its own Lua globals and the modules it registered.
 */
public class Script {
    private final String name;
    private final Path file;
    private final List<ScriptModule> modules = new ArrayList<>();
    private Globals globals;

    Script(String name, Path file) {
        this.name = name;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public List<ScriptModule> getModules() {
        return modules;
    }

    /**
     * Runs the file from the top, which registers its modules.
     */
    void run() throws IOException {
        globals = JsePlatform.standardGlobals();
        ScriptApi.install(this, globals);
        LuaValue chunk = globals.load(Files.readString(file), "@" + file.getFileName());
        chunk.call();
    }

    ScriptModule register(String moduleName, String description) {
        ScriptModule module = new ScriptModule(this, moduleName, description);
        modules.add(module);
        SinepSinem.moduleManager.registerScriptModule(module);
        return module;
    }

    void unregisterAll() {
        for (ScriptModule module : modules) {
            SinepSinem.moduleManager.unregisterScriptModule(module);
        }
        modules.clear();
    }

    void print(String text) {
        ChatUtil.sendMessage(Component.literal("[" + name + "] ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE)), SimpleSignature.from("script/" + name + "/" + text));
    }

    void error(String where, Throwable error) {
        SinepSinem.LOGGER.error("Script {} failed in {}", name, where, error);
        String message = error instanceof LuaError lua ? lua.getMessage() : String.valueOf(error.getMessage());
        ChatUtil.sendMessage(Component.literal("[" + name + "] ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(where + ": " + message).withStyle(ChatFormatting.RED)), SimpleSignature.from("script-error/" + name));
    }
}
