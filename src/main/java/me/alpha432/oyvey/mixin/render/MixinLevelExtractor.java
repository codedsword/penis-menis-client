package me.alpha432.oyvey.mixin.render;

import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.util.render.ProjectionUtil;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.alpha432.oyvey.util.traits.Util.EVENT_BUS;
import static me.alpha432.oyvey.util.traits.Util.mc;

@Mixin(LevelExtractor.class)
public class MixinLevelExtractor {
    // gizmos emitted here are drained into the level renderer right after
    @Inject(method = "extractGizmos", at = @At("HEAD"))
    private void extractGizmosHook(CallbackInfo ci) {
        if (mc.player == null || mc.level == null) return;
        ProjectionUtil.capture();
        Profiler.get().push("oyvey-render-3d");
        EVENT_BUS.post(new Render3DEvent(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)));
        Profiler.get().pop();
    }
}
