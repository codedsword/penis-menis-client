package dev.sinepsinem.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.sinepsinem.features.commands.Command;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.manager.CommandManager;

import static dev.sinepsinem.features.commands.argument.ModuleArgumentType.getModule;
import static dev.sinepsinem.features.commands.argument.ModuleArgumentType.module;

public class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle", "t");
        setDescription("Toggles a module");
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        builder.then(argument("module", module(true))
                .executes((ctx) -> {
                    Module module = getModule(ctx, "module");
                    module.toggle();
                    boolean toggled = module.isEnabled();
                    return success("{gray} %s {reset} is now %s %s",
                            module.getDisplayName(),
                            toggled ? "{green}" : "{red}",
                            toggled ? "enabled" : "disabled");
                }));
    }
}
