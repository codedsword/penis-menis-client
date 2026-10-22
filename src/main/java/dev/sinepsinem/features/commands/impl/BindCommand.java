package dev.sinepsinem.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.sinepsinem.event.impl.input.KeyInputEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.commands.Command;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Bind;
import dev.sinepsinem.manager.CommandManager;
import dev.sinepsinem.util.KeyboardUtil;

import static dev.sinepsinem.features.commands.argument.KeybindArgumentType.keybind;
import static dev.sinepsinem.features.commands.argument.ModuleArgumentType.module;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;

public class BindCommand extends Command {
    private Module module;

    public BindCommand() {
        super("bind", "setbind");
        setDescription("Sets a key bind for a module");
        EVENT_BUS.register(this);
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        builder.then(argument("module", module(true)).then(
                argument("keybind", keybind()).executes(ctx -> {
                    Module module = ctx.getArgument("module", Module.class);
                    Bind bind = ctx.getArgument("keybind", Bind.class);
                    module.bind.setValue(bind);
                    return success("Bind for {green} %s {} set to {green} %s",
                            module.getName(),
                            KeyboardUtil.getKeyName(bind)
                    );
                })
        ).executes((ctx) -> {
            module = ctx.getArgument("module", Module.class);
            return success("Press any key...");
        }));
    }

    @Subscribe
    public void onKey(final KeyInputEvent event) {
        if (nullCheck() || module == null || event.getKey() == GLFW_KEY_UNKNOWN) {
            return;
        }

        if (event.getKey() == GLFW_KEY_ESCAPE) {
            module = null;
            fail("Operation canceled.");
            return;
        }

        module.bind.setValue(Bind.keyboard(event.getKey()));
        success("Bind for {green} %s {} set to {green} %s",
                module.getName(),
                KeyboardUtil.getKeyName(module.getBind())
        );

        module = null;
    }
}
