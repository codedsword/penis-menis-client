package dev.sinepsinem.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.sinepsinem.features.commands.Command;
import dev.sinepsinem.manager.CommandManager;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

public class PrefixCommand extends Command {
    public PrefixCommand() {
        super("prefix", "setprefix");
        setDescription("Sets the command prefix");
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        builder.then(argument("prefix", greedyString())
                .executes((ctx) -> {
                    String prefix = getString(ctx, "prefix").trim();
                    if (prefix.isBlank()) {
                        return fail("Prefix must contain at least one character");
                    }
                    ctx.getSource().setCommandPrefix(prefix);
                    return success("Prefix changed to {green} %s", prefix);
                }));
    }
}
