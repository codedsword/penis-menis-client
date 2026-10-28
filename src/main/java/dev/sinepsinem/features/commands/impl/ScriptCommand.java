package dev.sinepsinem.features.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.features.commands.Command;
import dev.sinepsinem.features.scripting.Script;
import dev.sinepsinem.features.scripting.Scripts;
import dev.sinepsinem.manager.CommandManager;
import net.minecraft.util.Util;

/**
 * .script folder | list | load file | reload file | unload file
 */
public class ScriptCommand extends Command {
    public ScriptCommand() {
        super("script", "scripts");
        setDescription("Loads, reloads and unloads Lua scripts");
    }

    private static Scripts scripts() {
        return SinepSinem.scriptManager.scripts();
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        SuggestionProvider<CommandManager> files = (ctx, suggestions) -> {
            scripts().available().forEach(suggestions::suggest);
            return suggestions.buildFuture();
        };
        SuggestionProvider<CommandManager> loaded = (ctx, suggestions) -> {
            scripts().loaded().forEach(script -> suggestions.suggest(script.getName()));
            return suggestions.buildFuture();
        };

        builder.then(literal("folder").executes(ctx -> {
            scripts().createExample();
            Util.getPlatform().openPath(scripts().directory());
            return success("Opened {global} %s", scripts().directory());
        }));
        builder.then(literal("list").executes(ctx -> {
            if (scripts().loaded().isEmpty()) {
                return success("No scripts loaded, available: {global} %s", String.join(", ", scripts().available()));
            }
            StringBuilder text = new StringBuilder();
            for (Script script : scripts().loaded()) {
                if (!text.isEmpty()) text.append(", ");
                text.append(script.getName()).append(" (").append(script.getModules().size()).append(")");
            }
            return success("Loaded: {global} %s", text);
        }));
        builder.then(literal("load").then(argument("file", StringArgumentType.greedyString()).suggests(files).executes(ctx -> {
            scripts().load(StringArgumentType.getString(ctx, "file"), true);
            return NO_OP;
        })));
        builder.then(literal("reload").then(argument("file", StringArgumentType.greedyString()).suggests(loaded).executes(ctx -> {
            scripts().reload(StringArgumentType.getString(ctx, "file"));
            return NO_OP;
        })));
        builder.then(literal("unload").then(argument("file", StringArgumentType.greedyString()).suggests(loaded).executes(ctx -> {
            scripts().unload(StringArgumentType.getString(ctx, "file"), true);
            return NO_OP;
        })));
    }
}
