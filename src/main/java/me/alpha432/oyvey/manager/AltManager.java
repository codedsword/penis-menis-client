package me.alpha432.oyvey.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.util.traits.Jsonable;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Stores alts and swaps the game's session to one of them.
 */
public class AltManager implements Jsonable, Util {
    public static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    private final List<Alt> alts = new ArrayList<>();
    private User original;

    public void init() {
        OyVey.configManager.addConfig(this);
    }

    public List<Alt> getAlts() {
        return alts;
    }

    public void add(Alt alt) {
        alts.removeIf(a -> a.name().equalsIgnoreCase(alt.name()) && a.type() == alt.type());
        alts.add(alt);
    }

    public void remove(Alt alt) {
        alts.remove(alt);
    }

    public boolean isCurrent(Alt alt) {
        return mc.getUser().getName().equals(alt.name());
    }

    public void login(Alt alt) {
        if (original == null) original = mc.getUser();

        User user = switch (alt.type()) {
            case OFFLINE -> new User(alt.name(), UUIDUtil.createOfflinePlayerUUID(alt.name()), "0",
                    Optional.empty(), Optional.empty());
            case TOKEN -> new User(alt.name(), alt.uuid() != null ? alt.uuid() : UUIDUtil.createOfflinePlayerUUID(alt.name()),
                    alt.token(), Optional.empty(), Optional.empty());
        };
        setUser(user, alt.type() == Type.TOKEN);
        OyVey.LOGGER.info("Switched session to {} ({})", alt.name(), alt.type());
    }

    public void restore() {
        if (original != null) {
            setUser(original, true);
        }
    }

    private void setUser(User user, boolean online) {
        mc.user = user;
        // chat signing keys belong to the original account, a mismatch gets you kicked
        mc.profileKeyPairManager = online && user == original
                ? ProfileKeyPairManager.create(mc.userApiService, user, mc.gameDirectory.toPath())
                : ProfileKeyPairManager.EMPTY_KEY_MANAGER;
    }

    public static String randomName() {
        String[] first = {"Swift", "Silent", "Dark", "Lucky", "Crispy", "Frosty", "Mighty", "Sneaky", "Rapid", "Cosmic"};
        String[] second = {"Fox", "Wolf", "Pvper", "Ninja", "Tiger", "Ghost", "Raven", "Gamer", "Knight", "Panda"};
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return first[r.nextInt(first.length)] + second[r.nextInt(second.length)] + r.nextInt(10, 999);
    }

    @Override
    public JsonElement toJson() {
        JsonArray array = new JsonArray();
        for (Alt alt : alts) {
            JsonObject object = new JsonObject();
            object.addProperty("name", alt.name());
            object.addProperty("type", alt.type().name());
            if (alt.uuid() != null) object.addProperty("uuid", alt.uuid().toString());
            if (alt.token() != null) object.addProperty("token", alt.token());
            array.add(object);
        }
        JsonObject root = new JsonObject();
        root.add("alts", array);
        return root;
    }

    @Override
    public void fromJson(JsonElement element) {
        alts.clear();
        if (element == null || !element.isJsonObject() || !element.getAsJsonObject().has("alts")) return;
        for (JsonElement e : element.getAsJsonObject().getAsJsonArray("alts")) {
            try {
                JsonObject o = e.getAsJsonObject();
                alts.add(new Alt(o.get("name").getAsString(),
                        Type.valueOf(o.get("type").getAsString()),
                        o.has("uuid") ? UUID.fromString(o.get("uuid").getAsString()) : null,
                        o.has("token") ? o.get("token").getAsString() : null));
            } catch (Exception ex) {
                OyVey.LOGGER.warn("Skipping invalid alt entry {}", e);
            }
        }
    }

    @Override
    public String getFileName() {
        return "alts.json";
    }

    public enum Type {
        OFFLINE,
        TOKEN
    }

    public record Alt(String name, Type type, UUID uuid, String token) {
    }
}
