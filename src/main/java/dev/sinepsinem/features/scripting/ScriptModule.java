package dev.sinepsinem.features.scripting;

import dev.sinepsinem.event.impl.entity.player.AttackEvent;
import dev.sinepsinem.event.impl.entity.player.TickEvent;
import dev.sinepsinem.event.impl.network.PacketEvent;
import dev.sinepsinem.event.impl.render.Render2DEvent;
import dev.sinepsinem.event.impl.render.Render3DEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.Module;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A module a Lua script registered. It lives in the Scripts category and forwards client events
 * to the handlers the script set with {@code module:on(event, function)}.
 * <p>
 * Events: enable, disable, game_tick, pre_tick, render_2d, render_3d, attack, packet_send,
 * packet_receive. Packet handlers run on the network thread and may return true to cancel.
 */
public class ScriptModule extends Module {
    private final Script script;
    private final Map<String, List<LuaFunction>> handlers = new HashMap<>();

    ScriptModule(Script script, String name, String description) {
        super(name, description, Category.SCRIPTS);
        this.script = script;
    }

    public Script getScript() {
        return script;
    }

    void on(String event, LuaFunction handler) {
        handlers.computeIfAbsent(event, e -> new ArrayList<>()).add(handler);
    }

    /**
     * @return whether a handler returned true (for cancellable events)
     */
    private boolean fire(String event, Object... args) {
        List<LuaFunction> list = handlers.get(event);
        if (list == null || list.isEmpty()) return false;
        LuaValue[] values = new LuaValue[args.length];
        for (int i = 0; i < args.length; i++) values[i] = CoerceJavaToLua.coerce(args[i]);
        boolean cancel = false;
        for (LuaFunction handler : List.copyOf(list)) {
            try {
                Varargs result = handler.invoke(LuaValue.varargsOf(values));
                cancel |= result.arg1().toboolean();
            } catch (LuaError e) {
                // a broken script turns itself off instead of spamming the same error every tick
                script.error(getName() + " (" + event + ")", e);
                if (isEnabled() && !event.equals("disable")) mc.execute(this::disable);
                return false;
            }
        }
        return cancel;
    }

    @Override
    public void onEnable() {
        fire("enable");
    }

    @Override
    public void onDisable() {
        fire("disable");
    }

    @Subscribe
    public void onPreTick(TickEvent.Pre event) {
        fire("pre_tick", event);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        fire("game_tick", event);
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        fire("render_2d", event);
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        fire("render_3d", event);
    }

    @Subscribe
    public void onAttack(AttackEvent.Pre event) {
        if (fire("attack", event.getTarget())) event.cancel();
    }

    @Subscribe
    public void onSend(PacketEvent.Send event) {
        if (fire("packet_send", event.getPacket())) event.cancel();
    }

    @Subscribe
    public void onReceive(PacketEvent.Receive event) {
        if (fire("packet_receive", event.getPacket())) event.cancel();
    }
}
