package io.github.apace100.calio.util;

import com.mojang.serialization.MapLike;

public final class MapLikeUtil {

    public static <I> I getOrDefault(MapLike<I> mapLike, String key, I defaultValue) {
        I value = mapLike.get(key);
        return value != null ? value : defaultValue;
    }

}
