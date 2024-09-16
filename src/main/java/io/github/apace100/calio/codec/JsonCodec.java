package io.github.apace100.calio.codec;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import java.util.function.Function;

public class JsonCodec<T> implements Codec<T> {

    private final Function<JsonElement, T> fromJson;
    private final Function<T, JsonElement> toJson;

    public JsonCodec(Function<JsonElement, T> fromJson, Function<T, JsonElement> toJson) {
        this.fromJson = fromJson;
        this.toJson = toJson;
    }

    @Override
    public <I> DataResult<Pair<T, I>> decode(DynamicOps<I> ops, I input) {

        try {

            JsonElement json = ops.convertTo(JsonOps.INSTANCE, input);
            T result = fromJson.apply(json);

            return DataResult.success(Pair.of(result, input));

        }

        catch (Exception e) {
            return DataResult.error(e::getMessage);
        }

    }

    @Override
    public <I> DataResult<I> encode(T input, DynamicOps<I> ops, I prefix) {

        try {

            JsonElement jsonElement = toJson.apply(input);
            I result = JsonOps.INSTANCE.convertTo(ops, jsonElement);

            return DataResult.success(result);

        }

        catch (Exception e) {
            return DataResult.error(e::getMessage);
        }

    }

}
