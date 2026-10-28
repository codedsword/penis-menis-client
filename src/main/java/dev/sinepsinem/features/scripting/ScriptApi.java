package dev.sinepsinem.features.scripting;

import dev.sinepsinem.SinepSinem;
import dev.sinepsinem.event.impl.render.Render2DEvent;
import dev.sinepsinem.features.modules.Module;
import dev.sinepsinem.features.settings.Setting;
import dev.sinepsinem.util.BuildConfig;
import dev.sinepsinem.util.traits.Util;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

/**
 * The globals a script sees on top of the Lua standard library:
 * <pre>
 * modules.register(name, description) -> module
 *   module:on(event, function(...) end)   enable, disable, pre_tick, game_tick, render_2d, render_3d,
 *                                         attack (target), packet_send / packet_receive (packet)
 *   module:bool(name, default)            -> setting with :get() and :set(value)
 *   module:number(name, default, min, max)
 *   module:text(name, default)
 *   module:enabled(), module:toggle()
 * java.import(className)                  a Java class, call statics with a dot: Minecraft.getInstance()
 * client.username(), client.chat(text), client.command(text), client.module(name) -> enabled
 * client.text(event, text, x, y, color), client.rect(event, x1, y1, x2, y2, color)   in render_2d
 * print(...)                              to chat, debug(...) to the log
 * </pre>
 * {@code ambient} is an alias of {@code client}, so scripts written for Ambient's API keep working.
 */
final class ScriptApi implements Util {
    private ScriptApi() {
    }

    static void install(Script script, Globals globals) {
        LuaTable modules = new LuaTable();
        modules.set("register", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue name, LuaValue description) {
                String moduleName = name.checkjstring();
                if (SinepSinem.moduleManager.getModuleByName(moduleName) != null) {
                    throw new LuaError("a module called " + moduleName + " already exists");
                }
                return module(script.register(moduleName, description.optjstring("Script module")));
            }
        });
        globals.set("modules", modules);

        LuaTable java = new LuaTable();
        java.set("import", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue className) {
                try {
                    return staticAccess(CoerceJavaToLua.coerce(Class.forName(className.checkjstring(), true, ScriptApi.class.getClassLoader())));
                } catch (ClassNotFoundException e) {
                    throw new LuaError("no class " + className.tojstring());
                }
            }
        });
        globals.set("java", java);

        LuaTable client = client();
        globals.set("client", client);
        globals.set("ambient", client);

        globals.set("print", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                script.print(join(args));
                return NONE;
            }
        });
        globals.set("debug", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                SinepSinem.LOGGER.info("[{}] {}", script.getName(), join(args));
                return NONE;
            }
        });
    }

    /**
     * LuaJ only calls static methods with a colon ({@code Minecraft:getInstance()}). This passes the
     * class as the instance itself, so a dot works too, the way scripts for other clients write it.
     */
    private static LuaValue staticAccess(LuaValue javaClass) {
        LuaTable proxy = new LuaTable();
        LuaTable meta = new LuaTable();
        meta.set(LuaValue.INDEX, new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue self, LuaValue key) {
                LuaValue value = javaClass.get(key);
                // constructors and fields are fine as they are
                if (!value.isfunction() || key.tojstring().equals("new")) return value;
                return new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs args) {
                        // a colon call already passes the class (or the proxy) first
                        if (args.arg1() == javaClass || args.arg1() == proxy) {
                            return value.invoke(LuaValue.varargsOf(javaClass, args.subargs(2)));
                        }
                        return value.invoke(LuaValue.varargsOf(javaClass, args));
                    }
                };
            }
        });
        meta.set(LuaValue.NEWINDEX, new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue self, LuaValue key, LuaValue value) {
                javaClass.set(key, value);
                return NONE;
            }
        });
        proxy.setmetatable(meta);
        return proxy;
    }

    private static String join(Varargs args) {
        StringBuilder text = new StringBuilder();
        for (int i = 1; i <= args.narg(); i++) {
            if (i > 1) text.append('\t');
            text.append(args.arg(i).tojstring());
        }
        return text.toString();
    }

    private static LuaTable module(ScriptModule module) {
        LuaTable table = new LuaTable();
        table.set("on", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue self, LuaValue event, LuaValue handler) {
                module.on(event.checkjstring(), (LuaFunction) handler.checkfunction());
                return self;
            }
        });
        table.set("bool", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue self, LuaValue name, LuaValue value) {
                return setting(module.register(new Setting<>(name.checkjstring(), value.optboolean(false))));
            }
        });
        table.set("number", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                double value = args.checkdouble(3), min = args.optdouble(4, 0), max = args.optdouble(5, Math.max(10, value * 2));
                return setting(module.register(new Setting<>(args.checkjstring(2), (float) value, (float) min, (float) max)));
            }
        });
        table.set("text", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue self, LuaValue name, LuaValue value) {
                return setting(module.register(new Setting<>(name.checkjstring(), value.optjstring(""))));
            }
        });
        table.set("enabled", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue self) {
                return valueOf(module.isEnabled());
            }
        });
        table.set("toggle", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue self) {
                module.toggle();
                return NONE;
            }
        });
        table.set("name", LuaValue.valueOf(module.getName()));
        return table;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static LuaTable setting(Setting setting) {
        LuaTable table = new LuaTable();
        table.set("get", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue self) {
                return CoerceJavaToLua.coerce(setting.getValue());
            }
        });
        table.set("set", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue self, LuaValue value) {
                Object current = setting.getValue();
                if (current instanceof Boolean) setting.setValue(value.checkboolean());
                else if (current instanceof Float) setting.setValue((float) value.checkdouble());
                else setting.setValue(value.checkjstring());
                return NONE;
            }
        });
        return table;
    }

    private static LuaTable client() {
        LuaTable client = new LuaTable();
        client.set("username", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return valueOf(mc.getUser().getName());
            }
        });
        client.set("name", LuaValue.valueOf(BuildConfig.NAME));
        client.set("chat", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue text) {
                if (mc.getConnection() != null) mc.getConnection().sendChat(text.checkjstring());
                return NONE;
            }
        });
        client.set("command", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue text) {
                if (mc.getConnection() != null) mc.getConnection().sendCommand(text.checkjstring());
                return NONE;
            }
        });
        client.set("module", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue name) {
                Module module = SinepSinem.moduleManager.getModuleByName(name.checkjstring());
                return valueOf(module != null && module.isEnabled());
            }
        });
        client.set("text", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Render2DEvent event = (Render2DEvent) args.checkuserdata(1, Render2DEvent.class);
                int color = args.isnoneornil(5) ? -1 : (int) args.checklong(5);
                event.getContext().text(mc.font, args.checkjstring(2), args.checkint(3), args.checkint(4), color);
                return NONE;
            }
        });
        client.set("rect", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Render2DEvent event = (Render2DEvent) args.checkuserdata(1, Render2DEvent.class);
                event.getContext().fill(args.checkint(2), args.checkint(3), args.checkint(4), args.checkint(5), (int) args.checklong(6));
                return NONE;
            }
        });
        return client;
    }
}
