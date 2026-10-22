package dev.sinepsinem.features.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.sinepsinem.features.commands.CommandExceptions;
import dev.sinepsinem.features.settings.Bind;
import dev.sinepsinem.util.KeyboardUtil;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static dev.sinepsinem.features.commands.ArgumentSuggestions.suggest;

public final class KeybindArgumentType implements ArgumentType<Bind> {
    private static final List<String> EXAMPLES = List.of("A", "RIGHT_SHIFT", "MOUSE0");

    @Override
    public Bind parse(StringReader reader) throws CommandSyntaxException {
        String input = reader.readString();
        Bind bind = KeyboardUtil.getBind(input);
        if (bind == null) {
            throw CommandExceptions.invalidArgument("Invalid key name").createWithContext(reader);
        }
        return bind;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return suggest(KeyboardUtil.iterateKeys(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static KeybindArgumentType keybind() {
        return new KeybindArgumentType();
    }
}
