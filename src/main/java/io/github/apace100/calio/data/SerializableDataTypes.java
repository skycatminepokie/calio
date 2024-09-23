package io.github.apace100.calio.data;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import io.github.apace100.calio.codec.CalioCodecs;
import io.github.apace100.calio.codec.CalioPacketCodecs;
import io.github.apace100.calio.mixin.IngredientAccessor;
import io.github.apace100.calio.mixin.ItemStackAccessor;
import io.github.apace100.calio.mixin.TagEntryAccessor;
import io.github.apace100.calio.util.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientImpl;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientPacketCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.nbt.*;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.registry.tag.*;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.*;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;

import java.util.*;

@SuppressWarnings("unused")
public final class SerializableDataTypes {

    public static final SerializableDataType<Integer> INT = SerializableDataType.of(Codec.INT, PacketCodecs.INTEGER.cast());

    public static final SerializableDataType<List<Integer>> INTS = INT.list();

    public static final SerializableDataType<Integer> POSITIVE_INT = SerializableDataType.boundNumber(INT, 1, Integer.MAX_VALUE);

    public static final SerializableDataType<List<Integer>> POSITIVE_INTS = POSITIVE_INT.list();

    public static final SerializableDataType<Integer> NON_NEGATIVE_INT = SerializableDataType.boundNumber(INT, 0, Integer.MAX_VALUE);

    public static final SerializableDataType<List<Integer>> NON_NEGATIVE_INTS = NON_NEGATIVE_INT.list();

    public static final SerializableDataType<Boolean> BOOLEAN = SerializableDataType.of(Codec.BOOL, PacketCodecs.BOOL.cast());

    public static final SerializableDataType<Float> FLOAT = SerializableDataType.of(Codec.FLOAT, PacketCodecs.FLOAT.cast());

    public static final SerializableDataType<List<Float>> FLOATS = FLOAT.list();

    public static final SerializableDataType<Float> POSITIVE_FLOAT = SerializableDataType.boundNumber(FLOAT, 1F, Float.MAX_VALUE);

    public static final SerializableDataType<List<Float>> POSITIVE_FLOATS = POSITIVE_FLOAT.list();

    public static final SerializableDataType<Float> NON_NEGATIVE_FLOAT = SerializableDataType.boundNumber(FLOAT, 0F, Float.MAX_VALUE);

    public static final SerializableDataType<List<Float>> NON_NEGATIVE_FLOATS = NON_NEGATIVE_FLOAT.list();

    public static final SerializableDataType<Double> DOUBLE = SerializableDataType.of(Codec.DOUBLE, PacketCodecs.DOUBLE.cast());

    public static final SerializableDataType<List<Double>> DOUBLES = DOUBLE.list();

    public static final SerializableDataType<Double> POSITIVE_DOUBLE = SerializableDataType.boundNumber(DOUBLE, 1D, Double.MAX_VALUE);

    public static final SerializableDataType<List<Double>> POSITIVE_DOUBLES = POSITIVE_DOUBLE.list();

    public static final SerializableDataType<Double> NON_NEGATIVE_DOUBLE = SerializableDataType.boundNumber(DOUBLE, 0D, Double.MAX_VALUE);

    public static final SerializableDataType<List<Double>> NON_NEGATIVE_DOUBLES = NON_NEGATIVE_DOUBLE.list();

    public static final SerializableDataType<String> STRING = SerializableDataType.of(Codec.STRING, PacketCodecs.STRING.cast());

    public static final SerializableDataType<List<String>> STRINGS = STRING.list();

    public static final SerializableDataType<Number> NUMBER = SerializableDataType.lazy(() -> SerializableDataType.of(CalioCodecs.NUMBER, CalioPacketCodecs.NUMBER.cast()));

    public static final SerializableDataType<List<Number>> NUMBERS = NUMBER.list();

    public static final CompoundSerializableDataType<Vec3d> VECTOR = SerializableDataType.compound(
        new SerializableData()
            .add("x", DOUBLE, 0.0)
            .add("y", DOUBLE, 0.0)
            .add("z", DOUBLE, 0.0),
        data -> new Vec3d(
            data.getDouble("x"),
            data.getDouble("y"),
            data.getDouble("z")
        ),
        (vec3d, serializableData) -> serializableData.instance()
            .set("x", vec3d.getX())
            .set("y", vec3d.getY())
            .set("z", vec3d.getZ())
    );

    public static final SerializableDataType<Identifier> IDENTIFIER = SerializableDataType.of(
        Codec.STRING.comapFlatMap(DynamicIdentifier::ofResult, Identifier::toString),
        Identifier.PACKET_CODEC.cast()
    );

    public static final SerializableDataType<List<Identifier>> IDENTIFIERS = IDENTIFIER.list();

    public static final SerializableDataType<RegistryKey<Enchantment>> ENCHANTMENT = SerializableDataType.registryKey(RegistryKeys.ENCHANTMENT);

    public static SerializableDataType<RegistryKey<World>> DIMENSION = SerializableDataType.registryKey(RegistryKeys.WORLD, Set.of(
        World.OVERWORLD,
        World.NETHER,
        World.END
    ));

    public static final SerializableDataType<EntityAttribute> ATTRIBUTE = SerializableDataType.registry(Registries.ATTRIBUTE);

    public static final SerializableDataType<List<EntityAttribute>> ATTRIBUTES = ATTRIBUTE.list();

    public static final SerializableDataType<RegistryEntry<EntityAttribute>> ATTRIBUTE_ENTRY = SerializableDataType.registryEntry(Registries.ATTRIBUTE);

    public static final SerializableDataType<List<RegistryEntry<EntityAttribute>>> ATTRIBUTE_ENTRIES = ATTRIBUTE_ENTRY.list();

    public static final SerializableDataType<EntityAttributeModifier.Operation> MODIFIER_OPERATION = SerializableDataType.enumValue(EntityAttributeModifier.Operation.class);

    public static final CompoundSerializableDataType<EntityAttributeModifier> ATTRIBUTE_MODIFIER = SerializableDataType.compound(
        new SerializableData()
            .add("id", IDENTIFIER)
            .add("amount", DOUBLE)
            .add("operation", MODIFIER_OPERATION),
        data -> new EntityAttributeModifier(
            data.getId("id"),
            data.getDouble("amount"),
            data.get("operation")
        ),
        (entityAttributeModifier, serializableData) -> serializableData.instance()
            .set("id", entityAttributeModifier.id())
            .set("amount", entityAttributeModifier.value())
            .set("operation", entityAttributeModifier.operation())
    );

    public static final SerializableDataType<List<EntityAttributeModifier>> ATTRIBUTE_MODIFIERS = ATTRIBUTE_MODIFIER.list();

    public static final SerializableDataType<Item> ITEM = SerializableDataType.registry(Registries.ITEM);

    public static final SerializableDataType<RegistryEntry<Item>> ITEM_ENTRY = SerializableDataType.registryEntry(Registries.ITEM);

    public static final SerializableDataType<StatusEffect> STATUS_EFFECT = SerializableDataType.registry(Registries.STATUS_EFFECT);

    public static final SerializableDataType<List<StatusEffect>> STATUS_EFFECTS = STATUS_EFFECT.list();

    public static final SerializableDataType<RegistryEntry<StatusEffect>> STATUS_EFFECT_ENTRY = SerializableDataType.registryEntry(Registries.STATUS_EFFECT);

    public static final SerializableDataType<List<RegistryEntry<StatusEffect>>> STATUS_EFFECT_ENTRIES = STATUS_EFFECT_ENTRY.list();

    public static final CompoundSerializableDataType<StatusEffectInstance> STATUS_EFFECT_INSTANCE = SerializableDataType.compound(
        new SerializableData()
            .add("id", STATUS_EFFECT_ENTRY)
            .add("duration", INT, 100)
            .add("amplifier", INT, 0)
            .add("ambient", BOOLEAN, false)
            .add("show_particles", BOOLEAN, true)
            .add("show_icon", BOOLEAN, true),
        data -> new StatusEffectInstance(
            data.get("id"),
            data.getInt("duration"),
            data.getInt("amplifier"),
            data.getBoolean("ambient"),
            data.getBoolean("show_particles"),
            data.getBoolean("show_icon")
        ),
        (effectInstance, serializableData) -> serializableData.instance()
            .set("id", effectInstance.getEffectType())
            .set("duration", effectInstance.getDuration())
            .set("amplifier", effectInstance.getAmplifier())
            .set("ambient", effectInstance.isAmbient())
            .set("show_particles", effectInstance.shouldShowParticles())
            .set("show_icon", effectInstance.shouldShowIcon())
    );

    public static final SerializableDataType<List<StatusEffectInstance>> STATUS_EFFECT_INSTANCES = STATUS_EFFECT_INSTANCE.list();

    public static final SerializableDataType<TagKey<Item>> ITEM_TAG = SerializableDataType.tagKey(RegistryKeys.ITEM);

    public static final SerializableDataType<TagKey<Fluid>> FLUID_TAG = SerializableDataType.tagKey(RegistryKeys.FLUID);

    public static final SerializableDataType<TagKey<Block>> BLOCK_TAG = SerializableDataType.tagKey(RegistryKeys.BLOCK);

    public static final SerializableDataType<TagKey<EntityType<?>>> ENTITY_TAG = SerializableDataType.tagKey(RegistryKeys.ENTITY_TYPE);

	private static final SerializableDataType<Ingredient.StackEntry> INLINE_INGREDIENT_STACK_ENTRY = ITEM
		.xmap(ItemStack::new, ItemStack::getItem)
		.xmap(Ingredient.StackEntry::new, Ingredient.StackEntry::stack);

	private static final SerializableDataType<Ingredient.TagEntry> INLINE_INGREDIENT_TAG_ENTRY = SerializableDataType.of(
		new Codec<>() {

			@Override
			public <T> DataResult<Pair<Ingredient.TagEntry, T>> decode(DynamicOps<T> ops, T input) {
				return ops.getStringValue(input)
					.flatMap(str -> str.startsWith("#")
						? DataResult.success(str.substring(1))
						: DataResult.error(() -> "Item tags must start with '#'!"))
					.flatMap(str -> ITEM_TAG.codec().decode(ops, ops.createString(str))
						.map(tagAndInput -> tagAndInput
							.mapFirst(Ingredient.TagEntry::new)));
			}

			@Override
			public <T> DataResult<T> encode(Ingredient.TagEntry input, DynamicOps<T> ops, T prefix) {
				return DataResult.success(ops.createString("#" + input.tag().id()));
			}

		},
		ITEM_TAG.packetCodec().xmap(Ingredient.TagEntry::new, Ingredient.TagEntry::tag)
	);

	/**
	 * 	<p>A data type for decoding/encoding a {@link Ingredient.Entry} formatted as a string, either with a {@code #} prefix
	 * 	 (to define an item tag, e.g: {@code #minecraft:meat}) or no prefix (to define an item,
	 * 	 e.g: {@code minecraft:diamond}).</p>
	 *
	 * 	 <p>This data type also allows for defining an empty item stack entry (e.g: {@code minecraft:air}), compared to
	 * 	 {@link Ingredient.Entry#CODEC}, which doesn't.</p>
	 */
	public static final SerializableDataType<Ingredient.Entry> INLINE_INGREDIENT_ENTRY = SerializableDataType.of(
		new Codec<>() {

			@Override
			public <T> DataResult<Pair<Ingredient.Entry, T>> decode(DynamicOps<T> ops, T input) {
				return ops.getStringValue(input).flatMap(stringInput -> {

					DataResult<Pair<Ingredient.Entry, T>> stackResult = INLINE_INGREDIENT_STACK_ENTRY
						.read(ops, input)
						.map(entry -> Pair.of(entry, input));

					if (stackResult.isSuccess()) {
						return stackResult;
					}

					DataResult<Pair<Ingredient.Entry, T>> tagResult = INLINE_INGREDIENT_TAG_ENTRY
						.read(ops, input)
						.map(entry -> Pair.of(entry, input));

					if (tagResult.isSuccess()) {
						return tagResult;
					}

					StringBuilder errorBuilder = new StringBuilder("Couldn't decode ingredient entry");

					stackResult.ifError(error -> errorBuilder
						.append(" as an item (").append(error.message()).append(")"));
					tagResult.ifError(error -> errorBuilder
						.append(" or as a tag (").append(error.message()).append(")"));

					return DataResult.error(errorBuilder::toString);

				});
			}

			@Override
			public <T> DataResult<T> encode(Ingredient.Entry input, DynamicOps<T> ops, T prefix) {
				return switch (input) {
					case Ingredient.StackEntry stackEntry ->
						INLINE_INGREDIENT_STACK_ENTRY.codec().encode(stackEntry, ops, prefix);
					case Ingredient.TagEntry tagEntry ->
						INLINE_INGREDIENT_TAG_ENTRY.codec().encode(tagEntry, ops, prefix);
					default ->
						DataResult.error(() -> "Ingredient entry is not an item or tag!");
				};
			}
		},
		PacketCodec.ofStatic(
			(buf, value) -> {
				switch (value) {
					case Ingredient.StackEntry stackEntry -> {
						buf.writeByte(0);
						INLINE_INGREDIENT_STACK_ENTRY.send(buf, stackEntry);
					}
					case Ingredient.TagEntry tagEntry -> {
						buf.writeByte(1);
						INLINE_INGREDIENT_TAG_ENTRY.send(buf, tagEntry);
					}
					default ->
						throw new UnsupportedOperationException("Ingredient entry is not an item or a tag!");
				}
			},
			buf -> {
				byte type = buf.readByte();
				return switch (type) {
					case 0 ->
						INLINE_INGREDIENT_STACK_ENTRY.receive(buf);
					case 1 ->
						INLINE_INGREDIENT_TAG_ENTRY.receive(buf);
					default ->
						throw new UnsupportedOperationException("Ingredient entry is not an item or a tag!");
				};
			}
		)
	);

	/**
	 * 	<p>A data type for decoding/encoding a {@link Ingredient.Entry} formatted as an object with a {@code tag} key
	 * 	(to define an item tag, e.g: {@code {"tag": "minecraft:meat"}}) or an {@code item} key (to define an item, e.g:
	 * 	{@code {"item": "minecraft:diamond"}}</p>
	 *
	 * 	 <p>This data type also allows for defining an empty item stack entry (e.g: {@code minecraft:air}), compared to
	 * 	 {@link Ingredient.Entry#CODEC}, which doesn't. This is <b>deprecated</b> in favor of using
	 * 	 {@link #INLINE_INGREDIENT_ENTRY} since vanilla will use a similar format in the future.</p>
	 */
	@Deprecated(since = "1.14.0-alpha.7")
	public static final SerializableDataType<Ingredient.Entry> OBJECT_INGREDIENT_ENTRY = SerializableDataType.compound(
		new SerializableData()
			.add("item", ITEM, null)
			.add("tag", ITEM_TAG, null)
			.validate(data -> {

				boolean hasItem = data.isPresent("item");
				boolean hasTag = data.isPresent("tag");

				if (hasItem == hasTag) {
					return DataResult.error(() -> (hasItem ? "Both" : "Any of") + " \"item\" and \"tag\" fields " + (hasItem ? "shouldn't" : "must") + " be defined!");
				}

				else {
					return DataResult.success(data);
				}

			}),
		data -> {

			if (data.isPresent("item")) {

				Item item = data.get("item");
				ItemStack stack = new ItemStack(item);

				return new Ingredient.StackEntry(stack);

			}

			else {
				return new Ingredient.TagEntry(data.get("tag"));
			}

		},
		(entry, serializableData) -> {
			SerializableData.Instance data = serializableData.instance();
			return switch (entry) {
				case Ingredient.StackEntry stackEntry ->
					data.set("item", stackEntry.stack().getItem());
				case Ingredient.TagEntry tagEntry ->
					data.set("tag", tagEntry.tag());
				default ->
					throw new UnsupportedOperationException("Ingredient entry is not an item or a tag!");
			};
		}
	);

	/**
	 * 	A data type for decoding/encoding an {@link Ingredient.Entry} either as an object or a string.
	 * 	@see #INLINE_INGREDIENT_ENTRY
	 *  @see #OBJECT_INGREDIENT_ENTRY
	 */
    public static final SerializableDataType<Ingredient.Entry> INGREDIENT_ENTRY = SerializableDataType.recursive(dataType -> SerializableDataType.of(
		new Codec<>() {

			@Override
			public <T> DataResult<Pair<Ingredient.Entry, T>> decode(DynamicOps<T> ops, T input) {

				if (ops.getMap(input).isSuccess()) {
					return OBJECT_INGREDIENT_ENTRY.setRoot(dataType.isRoot()).codec().decode(ops, input);
				}

				else {
					return INLINE_INGREDIENT_ENTRY.codec().decode(ops, input);
				}

			}

			@Override
			public <T> DataResult<T> encode(Ingredient.Entry input, DynamicOps<T> ops, T prefix) {
				return INLINE_INGREDIENT_ENTRY.codec().encode(input, ops, prefix);
			}

		},
		INLINE_INGREDIENT_ENTRY.packetCodec()
	));

    public static final SerializableDataType<List<Ingredient.Entry>> INGREDIENT_ENTRIES = INGREDIENT_ENTRY.list(1, Integer.MAX_VALUE);

	private static final SerializableDataType<Ingredient.Entry[]> INGREDIENT_ENTRIES_ARRAY = INGREDIENT_ENTRIES.xmap(entries -> entries.toArray(Ingredient.Entry[]::new), ObjectArrayList::new);

    @SuppressWarnings("UnstableApiUsage")
	private static final Codec<CustomIngredient> CUSTOM_INGREDIENT_CODEC = CustomIngredientImpl.CODEC.dispatch(
        CustomIngredientImpl.TYPE_KEY,
        CustomIngredient::getSerializer,
        serializer -> serializer.getCodec(false)
    );

    /**
	 *  <p>A data type version of {@link Ingredient#DISALLOW_EMPTY_CODEC} that allows for
	 *  empty item stacks (e.g: {@code "minecraft:air"}), and inline references for items/item tags.</p>
	 *
	 * 	@see #INLINE_INGREDIENT_ENTRY
	 * 	@see #OBJECT_INGREDIENT_ENTRY
     */
    @SuppressWarnings("UnstableApiUsage")
	public static final SerializableDataType<Ingredient> INGREDIENT = SerializableDataType.recursive(dataType -> SerializableDataType.of(
		new Codec<>() {

			@Override
			public <T> DataResult<Pair<Ingredient, T>> decode(DynamicOps<T> ops, T input) {

				//	Check if the input has the type key used by Fabric's custom ingredients
				boolean hasFabricTypeKey = ops.getMap(input)
					.map(map -> map.get(CustomIngredientImpl.TYPE_KEY))
					.mapOrElse(Objects::nonNull, err -> false);

				//	...and if it does, decode the input as a Fabric custom ingredient
				if (hasFabricTypeKey) {
					return CUSTOM_INGREDIENT_CODEC.decode(ops, input)
						.map(customIngredientAndInput -> customIngredientAndInput
							.mapFirst(CustomIngredient::toVanilla));
				}

				//	Otherwise, decode the input as a vanilla ingredient
				else {
					return INGREDIENT_ENTRIES_ARRAY.setRoot(dataType.isRoot()).codec().decode(ops, input)
						.map(entriesAndInput -> entriesAndInput
							.mapFirst(Ingredient::new));
				}

			}

			@Override
			public <T> DataResult<T> encode(Ingredient input, DynamicOps<T> ops, T prefix) {

                if (input.getCustomIngredient() != null) {
                    return CUSTOM_INGREDIENT_CODEC.encode(input.getCustomIngredient(), ops, prefix);
                }

                else {
					return INGREDIENT_ENTRIES_ARRAY.setRoot(dataType.isRoot()).codec().encode(((IngredientAccessor) input).getEntries(), ops, prefix);
                }

			}

		},
        new CustomIngredientPacketCodec(ItemStack.OPTIONAL_LIST_PACKET_CODEC.xmap(
            itemStacks -> Ingredient.ofEntries(itemStacks
                .stream()
                .map(Ingredient.StackEntry::new)),
            ingredient ->
                Arrays.asList(ingredient.getMatchingStacks())
        ))
    ));

    /**
     *  A data type version of {@link Ingredient#DISALLOW_EMPTY_CODEC}
     */
    public static final SerializableDataType<Ingredient> VANILLA_INGREDIENT = SerializableDataType.of(Ingredient.DISALLOW_EMPTY_CODEC, Ingredient.PACKET_CODEC);

    public static final SerializableDataType<Block> BLOCK = SerializableDataType.registry(Registries.BLOCK);

    public static final SerializableDataType<BlockState> BLOCK_STATE = STRING.comapFlatMap(
        str -> {

            try {
                return DataResult.success(BlockArgumentParser.block(Registries.BLOCK.getReadOnlyWrapper(), str, false).blockState());
            }

            catch (Exception e) {
                return DataResult.error(e::getMessage);
            }

        },
        BlockArgumentParser::stringifyBlockState
    );

    public static final SerializableDataType<RegistryKey<DamageType>> DAMAGE_TYPE = SerializableDataType.registryKey(RegistryKeys.DAMAGE_TYPE);

    public static final SerializableDataType<TagKey<EntityType<?>>> ENTITY_GROUP_TAG = SerializableDataType.mapped(ImmutableBiMap.of(
        "undead", EntityTypeTags.UNDEAD,
        "arthropod", EntityTypeTags.ARTHROPOD,
        "illager", EntityTypeTags.ILLAGER,
        "aquatic", EntityTypeTags.AQUATIC
    ));

    public static final SerializableDataType<EquipmentSlot> EQUIPMENT_SLOT = SerializableDataType.enumValue(EquipmentSlot.class);

    public static final SerializableDataType<EnumSet<EquipmentSlot>> EQUIPMENT_SLOT_SET = SerializableDataType.enumSet(EQUIPMENT_SLOT);

    public static final SerializableDataType<AttributeModifierSlot> ATTRIBUTE_MODIFIER_SLOT = SerializableDataType.enumValue(AttributeModifierSlot.class);

    public static final SerializableDataType<EnumSet<AttributeModifierSlot>> ATTRIBUTE_MODIFIER_SLOT_SET = SerializableDataType.enumSet(ATTRIBUTE_MODIFIER_SLOT);

    public static final SerializableDataType<SoundEvent> SOUND_EVENT = IDENTIFIER.xmap(SoundEvent::of, SoundEvent::getId);

    public static final SerializableDataType<EntityType<?>> ENTITY_TYPE = SerializableDataType.registry(Registries.ENTITY_TYPE);

    public static final SerializableDataType<ParticleType<?>> PARTICLE_TYPE = SerializableDataType.registry(Registries.PARTICLE_TYPE);

    public static final SerializableDataType<NbtElement> NBT_ELEMENT = SerializableDataType.of(
        Codec.PASSTHROUGH.xmap(dynamic -> dynamic.convert(NbtOps.INSTANCE).getValue(), nbtElement -> new Dynamic<>(NbtOps.INSTANCE, nbtElement.copy())),
        PacketCodecs.nbt(NbtSizeTracker::ofUnlimitedBytes).cast()
    );

    public static final SerializableDataType<NbtCompound> NBT_COMPOUND = SerializableDataType.of(Codec.withAlternative(NbtCompound.CODEC, StringNbtReader.NBT_COMPOUND_CODEC), PacketCodecs.nbtCompound(NbtSizeTracker::ofUnlimitedBytes).cast());

    public static final SerializableDataType<ArgumentWrapper<NbtPathArgumentType.NbtPath>> NBT_PATH = SerializableDataType.argumentType(NbtPathArgumentType.nbtPath());

    public static final CompoundSerializableDataType<ParticleEffect> PARTICLE_EFFECT = SerializableDataType.compound(
        new SerializableData()
            .add("type", PARTICLE_TYPE)
            .addSupplied("params", NBT_COMPOUND, NbtCompound::new),
        (ops, data) -> {

            ParticleType<? extends ParticleEffect> particleType = data.get("type");
            NbtCompound paramsNbt = data.get("params");

            Identifier particleTypeId = Objects.requireNonNull(Registries.PARTICLE_TYPE.getId(particleType));
            if (particleType instanceof SimpleParticleType simpleParticleType) {
                return simpleParticleType;
            }

            else if (paramsNbt.isEmpty()) {
                throw new IllegalArgumentException("Particle effect \"" + particleTypeId + "\" requires parameters!");
            }

            else {

                RegistryOps<NbtElement> nbtOps = RegistryOpsUtil.getWrapperLookup(ops)
                    .map(wrapperLookup -> wrapperLookup.getOps(NbtOps.INSTANCE))
                    .orElseThrow(() -> new IllegalStateException("Couldn't decode particle effects without registry ops!"));

				paramsNbt.putString("type", particleTypeId.toString());
                return ParticleTypes.TYPE_CODEC
                    .parse(nbtOps, paramsNbt)
                    .getOrThrow(NbtException::new);

            }

        },
		(particleEffect, ops, serializableData) -> {

			RegistryOps<NbtElement> nbtOps = RegistryOpsUtil.getWrapperLookup(ops)
				.map(wrapperLookup -> wrapperLookup.getOps(NbtOps.INSTANCE))
				.orElseThrow(() -> new IllegalStateException("Couldn't encode particle effects without registry ops!"));
			NbtCompound paramsNbt = ParticleTypes.TYPE_CODEC.encodeStart(nbtOps, particleEffect)
				.flatMap(nbtElement -> nbtElement instanceof NbtCompound nbtCompound ? DataResult.success(nbtCompound) : DataResult.error(() -> "Not a compound tag: " + nbtElement))
				.ifSuccess(nbtCompound -> nbtCompound.remove("type"))
				.getOrThrow(NbtException::new);

			return serializableData.instance()
				.set("type", particleEffect.getType())
				.set("params", paramsNbt);

		}
    );

    public static final SerializableDataType<ParticleEffect> PARTICLE_EFFECT_OR_TYPE = SerializableDataType.recursive(dataType -> SerializableDataType.of(
		new Codec<>() {

			@Override
			public <T> DataResult<Pair<ParticleEffect, T>> decode(DynamicOps<T> ops, T input) {

				if (ops.getStringValue(input).isSuccess()) {
					return PARTICLE_TYPE.codec().parse(ops, input)
						.flatMap(type -> type instanceof SimpleParticleType simpleType
							? DataResult.success(simpleType)
							: DataResult.error(() -> "Particle effect \"" + Registries.PARTICLE_TYPE.getId(type) + "\" requires parameters!"))
						.map(type -> Pair.of(type, input));
				}

				else {
					return PARTICLE_EFFECT.setRoot(dataType.isRoot()).codec().decode(ops, input);
				}

			}

			@Override
			public <T> DataResult<T> encode(ParticleEffect input, DynamicOps<T> ops, T prefix) {
				return PARTICLE_EFFECT.setRoot(dataType.isRoot()).codec().encode(input, ops, prefix);
			}

		},
		PARTICLE_EFFECT.packetCodec()
	));

    public static final SerializableDataType<ComponentChanges> COMPONENT_CHANGES = SerializableDataType.of(ComponentChanges.CODEC, ComponentChanges.PACKET_CODEC);

    public static final CompoundSerializableDataType<ItemStack> UNCOUNTED_ITEM_STACK = SerializableDataType.compound(
        new SerializableData()
            .add("id", ITEM_ENTRY)
            .add("components", COMPONENT_CHANGES, ComponentChanges.EMPTY),
        data -> new ItemStack(
            data.get("id"), 1,
            data.get("components")
        ),
        (stack, serializableData) -> serializableData.instance()
            .set("id", stack.getRegistryEntry())
            .set("components", stack.getComponentChanges())
    );

    public static final CompoundSerializableDataType<ItemStack> ITEM_STACK = SerializableDataType.compound(
        UNCOUNTED_ITEM_STACK.serializableData().copy()
            .add("count", SerializableDataType.boundNumber(INT, 1, 99), 1),
        (ops, data) -> {

            ItemStack stack = UNCOUNTED_ITEM_STACK.fromData(ops, data);
            stack.setCount(data.getInt("count"));

            return stack;

        },
        (stack, ops, serializableData) -> UNCOUNTED_ITEM_STACK
			.toData(stack, ops, serializableData)
            .set("count", ((ItemStackAccessor) (Object) stack).getCountOverride())
    );

    public static final SerializableDataType<List<ItemStack>> ITEM_STACKS = ITEM_STACK.list();

    public static final SerializableDataType<Text> TEXT = SerializableDataType.of(TextCodecs.CODEC, TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC);

    public static final SerializableDataType<List<Text>> TEXTS = TEXT.list();

    public static final SerializableDataType<RecipeEntry<? extends Recipe<?>>> RECIPE = SerializableDataType.lazy(() -> SerializableDataType.of(CalioCodecs.RECIPE_ENTRY, CalioPacketCodecs.RECIPE_ENTRY));

    public static final SerializableDataType<GameEvent> GAME_EVENT = SerializableDataType.registry(Registries.GAME_EVENT);

    public static final SerializableDataType<List<GameEvent>> GAME_EVENTS = GAME_EVENT.list();

    public static final SerializableDataType<RegistryEntry<GameEvent>> GAME_EVENT_ENTRY = SerializableDataType.registryEntry(Registries.GAME_EVENT);

    public static final SerializableDataType<List<RegistryEntry<GameEvent>>> GAME_EVENT_ENTRIES = GAME_EVENT_ENTRY.list();

    public static final SerializableDataType<TagKey<GameEvent>> GAME_EVENT_TAG = SerializableDataType.tagKey(RegistryKeys.GAME_EVENT);

    public static final SerializableDataType<Fluid> FLUID = SerializableDataType.registry(Registries.FLUID);

    public static final SerializableDataType<CameraSubmersionType> CAMERA_SUBMERSION_TYPE = SerializableDataType.enumValue(CameraSubmersionType.class);

    public static final SerializableDataType<Hand> HAND = SerializableDataType.enumValue(Hand.class, ImmutableMap.of(
        "mainhand", Hand.MAIN_HAND,
        "offhand", Hand.OFF_HAND
    ));

    public static final SerializableDataType<EnumSet<Hand>> HAND_SET = SerializableDataType.enumSet(HAND);

    public static final SerializableDataType<ActionResult> ACTION_RESULT = SerializableDataType.enumValue(ActionResult.class);

    public static final SerializableDataType<UseAction> USE_ACTION = SerializableDataType.enumValue(UseAction.class);

    public static final CompoundSerializableDataType<StatusEffectChance> STATUS_EFFECT_CHANCE = SerializableDataType.compound(
        new SerializableData()
            .add("effect", STATUS_EFFECT_INSTANCE)
            .add("chance", FLOAT, 1.0F),
        data -> new StatusEffectChance(
            data.get("effect"),
            data.getFloat("chance")
        ),
        (effectChance, serializableData) -> serializableData.instance()
            .set("effect", effectChance.statusEffectInstance())
            .set("chance", effectChance.chance())
    );

    public static final SerializableDataType<List<StatusEffectChance>> STATUS_EFFECT_CHANCES = STATUS_EFFECT_CHANCE.list();

    public static final SerializableDataType<FoodComponent.StatusEffectEntry> FOOD_STATUS_EFFECT_ENTRY = SerializableDataType.of(FoodComponent.StatusEffectEntry.CODEC, FoodComponent.StatusEffectEntry.PACKET_CODEC);

    public static final SerializableDataType<List<FoodComponent.StatusEffectEntry>> FOOD_STATUS_EFFECT_ENTRIES = FOOD_STATUS_EFFECT_ENTRY.list();

    public static final CompoundSerializableDataType<FoodComponent> FOOD_COMPONENT = SerializableDataType.compound(
        new SerializableData()
            .add("nutrition", NON_NEGATIVE_INT)
            .add("saturation", FLOAT)
            .add("can_always_eat", BOOLEAN, false)
            .add("eat_seconds", NON_NEGATIVE_FLOAT, 1.6F)
            .addSupplied("using_converts_to", SerializableDataType.optional(UNCOUNTED_ITEM_STACK, false), Optional::empty)
            .add("effect", FOOD_STATUS_EFFECT_ENTRY, null)
            .add("effects", FOOD_STATUS_EFFECT_ENTRIES, null),
        data -> {

            List<FoodComponent.StatusEffectEntry> effects = new ArrayList<>();

            data.<FoodComponent.StatusEffectEntry>ifPresent("effect", effects::add);
            data.<List<FoodComponent.StatusEffectEntry>>ifPresent("effects", effects::addAll);

            return new FoodComponent(
                data.getInt("nutrition"),
                data.getFloat("saturation"),
                data.getBoolean("can_always_eat"),
                data.getFloat("eat_seconds"),
                data.get("using_converts_to"),
                effects
            );

        },
        (foodComponent, serializableData) -> serializableData.instance()
            .set("nutrition", foodComponent.nutrition())
            .set("saturation", foodComponent.saturation())
            .set("can_always_eat", foodComponent.canAlwaysEat())
            .set("eat_seconds", foodComponent.eatSeconds())
            .set("using_converts_to", foodComponent.usingConvertsTo())
            .set("effects", foodComponent.effects())
    );

    public static final SerializableDataType<Direction> DIRECTION = SerializableDataType.enumValue(Direction.class);

    public static final SerializableDataType<EnumSet<Direction>> DIRECTION_SET = SerializableDataType.enumSet(DIRECTION);

    public static final SerializableDataType<Class<?>> CLASS = STRING.comapFlatMap(
        str -> {

            try {
                return DataResult.success(Class.forName(str));
            }

            catch (ClassNotFoundException ignored) {
                return DataResult.error(() -> "Specified class does not exist: \"" + str + "\"");
            }

        },
        Class::getName
    );

    public static final SerializableDataType<RaycastContext.ShapeType> SHAPE_TYPE = SerializableDataType.enumValue(RaycastContext.ShapeType.class);

    public static final SerializableDataType<RaycastContext.FluidHandling> FLUID_HANDLING = SerializableDataType.enumValue(RaycastContext.FluidHandling.class);

    public static final SerializableDataType<Explosion.DestructionType> DESTRUCTION_TYPE = SerializableDataType.enumValue(Explosion.DestructionType.class);

    public static final SerializableDataType<Direction.Axis> AXIS = SerializableDataType.enumValue(Direction.Axis.class);

    public static final SerializableDataType<EnumSet<Direction.Axis>> AXIS_SET = SerializableDataType.enumSet(AXIS);

    public static final SerializableDataType<StatType<?>> STAT_TYPE = SerializableDataType.registry(Registries.STAT_TYPE);

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final CompoundSerializableDataType<Stat<?>> STAT = SerializableDataType.compound(
        new SerializableData()
            .add("type", STAT_TYPE)
            .add("id", IDENTIFIER),
        data -> {

            StatType statType = data.get("type");
            Identifier statId = data.getId("id");

            Registry statRegistry = statType.getRegistry();
            Identifier statTypeId = Objects.requireNonNull(Registries.STAT_TYPE.getId(statType));

            try {
                return (Stat<?>) statRegistry.getOrEmpty(statId)
                    .map(statType::getOrCreateStat)
                    .orElseThrow();
            }

            catch (Exception e) {
                throw new IllegalArgumentException("Desired stat \"" + statId + "\" does not exist in stat type \"" + statTypeId + "\"");
            }

        },
        (stat, serializableData) -> {

            SerializableData.Instance data = serializableData.instance();

            StatType statType = stat.getType();
            Optional<Identifier> optId = Optional.ofNullable(statType.getRegistry().getId(stat.getValue()));

            data.set("type", statType);
            optId.ifPresent(id -> data.set("id", id));

            return data;

        }
    );

    public static final SerializableDataType<TagKey<Biome>> BIOME_TAG = SerializableDataType.tagKey(RegistryKeys.BIOME);

	public static final SerializableDataType<Codecs.TagEntryId> TAG_ENTRY_ID = STRING.comapFlatMap(
		str -> str.startsWith("#")
			? DynamicIdentifier.ofResult(str.substring(1)).map(id -> new Codecs.TagEntryId(id, true))
			: DynamicIdentifier.ofResult(str).map(id -> new Codecs.TagEntryId(id, false)),
		Codecs.TagEntryId::toString
	);

	public static final CompoundSerializableDataType<TagEntry> OBJECT_TAG_ENTRY = SerializableDataType.compound(
		new SerializableData()
			.add("id", TAG_ENTRY_ID)
			.add("required", BOOLEAN, true),
		data -> new TagEntry(
			data.get("id"),
			data.get("required")
		),
		(tagEntry, serializableData) -> serializableData.instance()
			.set("id", ((TagEntryAccessor) tagEntry).callGetIdForCodec())
			.set("required", ((TagEntryAccessor) tagEntry).isRequired())
	);

    public static final SerializableDataType<TagEntry> TAG_ENTRY = SerializableDataType.recursive(dataType -> SerializableDataType.of(
		new Codec<>() {

			@Override
			public <T> DataResult<Pair<TagEntry, T>> decode(DynamicOps<T> ops, T input) {

				DataResult<Pair<TagEntry, T>> entryIdResult = TAG_ENTRY_ID.codec().decode(ops, input)
					.map(entryIdAndInput -> entryIdAndInput
						.mapFirst(entryId -> new TagEntry(entryId, true)));
				if (entryIdResult.isSuccess()) {
					return entryIdResult;
				}

				DataResult<Pair<TagEntry, T>> entryResult = OBJECT_TAG_ENTRY.setRoot(dataType.isRoot()).codec().decode(ops, input);
				if (entryResult.isSuccess()) {
					return OBJECT_TAG_ENTRY.codec().decode(ops, input);
				}

				StringBuilder errorBuilder = new StringBuilder("Couldn't decode tag entry");

				entryIdResult.ifError(error -> errorBuilder.append(" as an ID (").append(error.message()).append(")"));
				entryResult.ifError(error -> errorBuilder.append(" or as an object (").append(error.message()).append(")"));

				return DataResult.error(errorBuilder::toString);

			}

			@Override
			public <T> DataResult<T> encode(TagEntry input, DynamicOps<T> ops, T prefix) {

				if (((TagEntryAccessor) input).isRequired()) {
					return TAG_ENTRY_ID.codec().encode(((TagEntryAccessor) input).callGetIdForCodec(), ops, prefix);
				}

				else {
					return OBJECT_TAG_ENTRY.setRoot(dataType.isRoot()).codec().encode(input, ops, prefix);
				}

			}

		},
		CalioPacketCodecs.TAG_ENTRY.cast()
	));

    public static final SerializableDataType<List<TagEntry>> TAG_ENTRIES = TAG_ENTRY.list();

    public static final SerializableDataType<TagLike<Item>> ITEM_TAG_LIKE = SerializableDataType.tagLike(Registries.ITEM);

    public static final SerializableDataType<TagLike<Block>> BLOCK_TAG_LIKE = SerializableDataType.tagLike(Registries.BLOCK);

    public static final SerializableDataType<TagLike<EntityType<?>>> ENTITY_TYPE_TAG_LIKE = SerializableDataType.tagLike(Registries.ENTITY_TYPE);

    public static final SerializableDataType<PotionContentsComponent> POTION_CONTENTS_COMPONENT = SerializableDataType.of(PotionContentsComponent.CODEC, PotionContentsComponent.PACKET_CODEC);

    public static final SerializableDataType<RegistryKey<LootFunction>> ITEM_MODIFIER = SerializableDataType.registryKey(RegistryKeys.ITEM_MODIFIER);

    public static final SerializableDataType<RegistryKey<LootCondition>> PREDICATE = SerializableDataType.registryKey(RegistryKeys.PREDICATE);

    public static void init() {

    }

}
