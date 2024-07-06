package io.github.apace100.calio;

import java.util.HashMap;
import java.util.function.Function;

public class SerializationHelper {

    public static <T extends Enum<T>> HashMap<String, T> buildEnumMap(Class<T> enumClass, Function<T, String> enumToString) {
        HashMap<String, T> map = new HashMap<>();
        for (T enumConstant : enumClass.getEnumConstants()) {
            map.put(enumToString.apply(enumConstant), enumConstant);
        }
        return map;
    }

}
