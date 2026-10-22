package dev.sinepsinem.mixin.render.gui;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.manager.CommandManager;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class MixinCommandSuggestions {
    @Shadow
    private CommandSuggestions.SuggestionsList suggestions;
    @Shadow
    private @Nullable CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow
    private @Nullable ParseResults<ClientSuggestionProvider> currentParse;
    @Shadow
    boolean keepSuggestions;
    @Shadow
    @Final
    EditBox input;

    @Shadow
    protected abstract void updateUsageInfo(ParseResults<ClientSuggestionProvider> currentParse, Suggestions suggestions);

    @SuppressWarnings("unchecked")
    @Inject(method = "updateCommandInfo", at = @At("RETURN"))
    public void updateCommandInfo(CallbackInfo ci, @Local StringReader stringReader) {
        String prefix = SinepSinem.commandManager.getCommandPrefix();
        if (!stringReader.canRead() || !stringReader.getString().startsWith(prefix)) {
            return;
        }
        stringReader.setCursor(prefix.length());

        CommandDispatcher<CommandManager> dispatcher = SinepSinem.commandManager.getDispatcher();
        ParseResults<CommandManager> parse = dispatcher.parse(stringReader, SinepSinem.commandManager);
        currentParse = (ParseResults<ClientSuggestionProvider>) (Object) parse;
        if (suggestions == null || !keepSuggestions) {
            pendingSuggestions = dispatcher.getCompletionSuggestions(parse, input.getCursorPosition());
            pendingSuggestions.thenAccept(result -> {
                if (pendingSuggestions.isDone()) {
                    updateUsageInfo(currentParse, result);
                }
            });
        }
    }
}
