package me.alpha432.oyvey.mixin.render.gui;

import me.alpha432.oyvey.features.gui.AltManagerScreen;
import me.alpha432.oyvey.util.text.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {
    @Unique
    private static final int ALT_BUTTON_WIDTH = 100;
    @Unique
    private static final int ALT_BUTTON_HEIGHT = 20;

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addAltManagerButton(CallbackInfo ci) {
        // half the screen width, height minus the button size
        this.addRenderableWidget(Button.builder(Component.literal("Alt Manager"),
                        button -> this.minecraft.gui.setScreen(new AltManagerScreen(this)))
                .bounds(this.width / 2 - ALT_BUTTON_WIDTH / 2, this.height - ALT_BUTTON_HEIGHT,
                        ALT_BUTTON_WIDTH, ALT_BUTTON_HEIGHT)
                .build());
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void drawSession(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        Component text = Fonts.text("Logged in as ").withColor(0xFFA3A3A3)
                .append(Fonts.bold(this.minecraft.getUser().getName()).withColor(0xFFE662A3));
        graphics.text(this.font, text, 4, 4, -1, false);
    }
}
