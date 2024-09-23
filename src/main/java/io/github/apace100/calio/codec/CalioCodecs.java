package io.github.apace100.calio.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.recipe.RecipeEntry;

public class CalioCodecs {

    public static final Codec<Number> NUMBER = new PrimitiveCodec<>() {

        @Override
        public <T> DataResult<Number> read(DynamicOps<T> ops, T input) {
            return ops.getNumberValue(input);
        }

        @Override
        public <T> T write(DynamicOps<T> ops, Number value) {
            return ops.createNumeric(value);
        }

    };

    public static final Codec<RecipeEntry<?>> RECIPE_ENTRY = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
        SerializableDataTypes.IDENTIFIER.codec().fieldOf("id").forGetter(RecipeEntry::id),
        CalioMapCodecs.RECIPE.forGetter(RecipeEntry::value)
    ).apply(instance, RecipeEntry::new)));

    public static void init() {

    }

}
