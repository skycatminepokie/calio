package io.github.apace100.calio.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Ingredient.class)
public interface IngredientAccessor {

    @Accessor
    Ingredient.Entry[] getEntries();

    @Mixin(Ingredient.TagEntry.class)
    interface EntryAccessor {

        @Final
        @Accessor("CODEC")
        static Codec<Ingredient.TagEntry> getCodec() {
            throw new AssertionError();
        }

    }

}
