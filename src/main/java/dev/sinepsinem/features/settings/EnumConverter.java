package dev.sinepsinem.features.settings;

import com.google.common.base.Converter;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

@SuppressWarnings("unchecked")
public class EnumConverter<T extends Enum<T>> extends Converter<T, JsonElement> {
    private final Class<T> clazz;

    public EnumConverter(Class<T> clazz) {
        this.clazz = clazz;
    }

    public static <T extends Enum<?>> int currentEnum(T clazz) {
        for (int i = 0; i < clazz.getDeclaringClass().getEnumConstants().length; ++i) {
            T e = (T) clazz.getDeclaringClass().getEnumConstants()[i];
            if (!e.name().equalsIgnoreCase(clazz.name())) continue;
            return i;
        }
        return -1;
    }

    public static <T extends Enum<?>> T increaseEnum(T clazz) {
        int index = EnumConverter.currentEnum(clazz);
        for (int i = 0; i < clazz.getDeclaringClass().getEnumConstants().length; ++i) {
            T e = (T) clazz.getDeclaringClass().getEnumConstants()[i];
            if (i != index + 1) continue;
            return e;
        }
        return (T) clazz.getDeclaringClass().getEnumConstants()[0];
    }

    // W_TAP -> W Tap
    public static <T extends Enum<?>> String getProperName(T clazz) {
        StringBuilder name = new StringBuilder();
        for (String word : clazz.name().split("_")) {
            if (word.isEmpty()) continue;
            if (!name.isEmpty()) name.append(' ');
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
        }
        return name.toString();
    }

    public JsonElement doForward(Enum anEnum) {
        return new JsonPrimitive(anEnum.name());
    }

    public T doBackward(JsonElement jsonElement) {
        String name = jsonElement.getAsString();
        for (T constant : this.clazz.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(name) || constant.toString().equalsIgnoreCase(name)) {
                return constant;
            }
        }
        return null;
    }
}