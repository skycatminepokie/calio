package io.github.apace100.calio.codec;

import com.mojang.serialization.MapCodec;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;

public class CalioMapCodecs {

    public static final MapCodec<Recipe<?>> RECIPE = Registries.RECIPE_SERIALIZER
        .getCodec()
        .dispatchMap(Recipe::getSerializer, RecipeSerializer::codec);

}
