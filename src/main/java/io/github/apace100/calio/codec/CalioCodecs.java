package io.github.apace100.calio.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.calio.mixin.IngredientAccessor;
import io.github.apace100.calio.mixin.ItemStackAccessor;
import io.github.apace100.calio.mixin.TagEntryAccessor;
import io.github.apace100.calio.util.DynamicIdentifier;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientImpl;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagEntry;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.function.Function;

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
        SerializableDataTypes.IDENTIFIER.fieldOf("id").forGetter(RecipeEntry::id),
        CalioMapCodecs.RECIPE.forGetter(RecipeEntry::value)
    ).apply(instance, RecipeEntry::new)));

    public static final Codec<RegistryEntry<Item>> ITEM = Registries.ITEM.getEntryCodec();

    public static final Codec<ItemStack> ITEM_STACK = ITEM
        .xmap(ItemStack::new, ItemStack::getRegistryEntry)
        .validate(ItemStackAccessor::callValidate);

    public static final Codec<Ingredient.StackEntry> INGREDIENT_STACK_ENTRY = RecordCodecBuilder.create(instance -> instance.group(
        ITEM_STACK.fieldOf("item").forGetter(Ingredient.StackEntry::stack)
    ).apply(instance, Ingredient.StackEntry::new));

    public static final Codec<Ingredient.TagEntry> INGREDIENT_TAG_ENTRY = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
        SerializableDataTypes.ITEM_TAG.fieldOf("tag").forGetter(Ingredient.TagEntry::tag)
    ).apply(instance, Ingredient.TagEntry::new)));

    public static final Codec<Ingredient.Entry> INGREDIENT_ENTRY = Codec.xor(INGREDIENT_STACK_ENTRY, INGREDIENT_TAG_ENTRY).xmap(
        either -> either.map(
            Function.identity(),
            Function.identity()
        ),
        entry -> switch (entry) {
            case Ingredient.TagEntry tagEntry ->
                Either.right(tagEntry);
            case Ingredient.StackEntry stackEntry ->
                Either.left(stackEntry);
            default ->
                throw new UnsupportedOperationException("Ingredient entry is neither a tag or an item.");
        }
    );

    public static final Codec<Codecs.TagEntryId> TAG_ENTRY_ID = Codec.STRING.comapFlatMap(
        str -> str.startsWith("#")
            ? DynamicIdentifier.ofResult(str.substring(1)).map(id -> new Codecs.TagEntryId(id, true))
            : DynamicIdentifier.ofResult(str).map(id -> new Codecs.TagEntryId(id, false)),
        Codecs.TagEntryId::toString
    );

    public static final Codec<TagEntry> STRICT_TAG_ENTRY = RecordCodecBuilder.create(instance -> instance.group(
        TAG_ENTRY_ID.fieldOf("id").forGetter(tagEntry -> ((TagEntryAccessor) tagEntry).callGetIdForCodec()),
        Codec.BOOL.optionalFieldOf("required", true).forGetter(tagEntry -> ((TagEntryAccessor) tagEntry).isRequired())
    ).apply(instance, TagEntry::new));

    public static final StrictCodec<TagEntry> TAG_ENTRY = StrictCodec.of(Codec.either(TAG_ENTRY_ID, STRICT_TAG_ENTRY).xmap(
        either -> either.map(
            entryId -> new TagEntry(entryId, true),
            Function.identity()
        ),
        tagEntry -> ((TagEntryAccessor) tagEntry).isRequired()
            ? Either.left(((TagEntryAccessor) tagEntry).callGetIdForCodec())
            : Either.right(tagEntry)
    ));

    public static Codec<Ingredient> ingredient(boolean allowEmpty) {

        Codec<Ingredient.Entry[]> entryCodec = INGREDIENT_ENTRY.listOf().comapFlatMap(
            entries -> !allowEmpty && entries.isEmpty()
                ? DataResult.error(() -> "Item array cannot be empty, at least one item must be defined")
                : DataResult.success(entries.toArray(Ingredient.Entry[]::new)),
            List::of
        );

        Codec<Ingredient> vanillaIngredientCodec = Codec.either(entryCodec, INGREDIENT_ENTRY).flatComapMap(
            either ->
                either.map(Ingredient::new, entry -> new Ingredient(new Ingredient.Entry[] {entry})),
            ingredient -> {

                IngredientAccessor ingredientAccessor = (IngredientAccessor) ingredient;
                Ingredient.Entry[] entries = ingredientAccessor.getEntries();

                if (entries.length == 1) {
                    return DataResult.success(Either.right(entries[0]));
                }

                else if (!allowEmpty && entries.length == 0) {
                    return DataResult.error(() -> "Item array cannot be empty, at least one item must be defined");
                }

                else {
                    return DataResult.success(Either.left(entries));
                }

            }
        );

        Codec<CustomIngredient> customIngredientCodec = CustomIngredientImpl.CODEC.dispatch(
            CustomIngredientImpl.TYPE_KEY,
            CustomIngredient::getSerializer,
            serializer -> serializer.getCodec(allowEmpty)
        );

        return Codec.either(customIngredientCodec, vanillaIngredientCodec).xmap(
            either -> either.map(
                CustomIngredient::toVanilla,
                Function.identity()
            ),
            ingredient -> {
                CustomIngredient customIngredient = ingredient.getCustomIngredient();
                return customIngredient == null
                    ? Either.right(ingredient)
                    : Either.left(customIngredient);
            }
        );

    }

    public static void init() {

    }

}
