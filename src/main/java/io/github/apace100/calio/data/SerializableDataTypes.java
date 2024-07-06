package io.github.apace100.calio.data;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.google.gson.internal.LazilyParsedNumber;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.ClassUtil;
import io.github.apace100.calio.mixin.IngredientAccessor;
import io.github.apace100.calio.util.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.component.ComponentChanges;
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
import net.minecraft.network.PacketByteBuf;
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

    public static final SerializableDataType<Integer> INT = new SerializableDataType<>(
        Integer.class,
        PacketByteBuf::writeInt,
        PacketByteBuf::readInt,
        JsonElement::getAsInt,
        JsonPrimitive::new);

    public static final SerializableDataType<List<Integer>> INTS = SerializableDataType.list(INT);

    public static final SerializableDataType<Integer> POSITIVE_INT = SerializableDataType.boundNumber(
        INT, 1, Integer.MAX_VALUE,
        value -> (min, max) -> {

            if (value < min || value > max) {
                throw new IllegalArgumentException("Expected integer to be greater than 0! (current value: " + value + ")");
            }

            return value;

        }
    );

    public static final SerializableDataType<List<Integer>> POSITIVE_INTS = SerializableDataType.list(POSITIVE_INT);

    public static final SerializableDataType<Boolean> BOOLEAN = new SerializableDataType<>(
        Boolean.class,
        PacketByteBuf::writeBoolean,
        PacketByteBuf::readBoolean,
        JsonElement::getAsBoolean,
        JsonPrimitive::new);

    public static final SerializableDataType<Float> FLOAT = new SerializableDataType<>(
        Float.class,
        PacketByteBuf::writeFloat,
        PacketByteBuf::readFloat,
        JsonElement::getAsFloat,
        JsonPrimitive::new);

    public static final SerializableDataType<List<Float>> FLOATS = SerializableDataType.list(FLOAT);

    public static final SerializableDataType<Float> POSITIVE_FLOAT = SerializableDataType.boundNumber(
        FLOAT, 1F, Float.MAX_VALUE,
        value -> (min, max) -> {

            if (value < min || value > max) {
                throw new IllegalArgumentException("Expected float to be greater than 0! (current value: " + value + ")");
            }

            return value;

        }
    );

    public static final SerializableDataType<List<Float>> POSITIVE_FLOATS = SerializableDataType.list(POSITIVE_FLOAT);

    public static final SerializableDataType<Double> DOUBLE = new SerializableDataType<>(
        Double.class,
        PacketByteBuf::writeDouble,
        PacketByteBuf::readDouble,
        JsonElement::getAsDouble,
        JsonPrimitive::new);

    public static final SerializableDataType<List<Double>> DOUBLES = SerializableDataType.list(DOUBLE);

    public static final SerializableDataType<Double> POSITIVE_DOUBLE = SerializableDataType.boundNumber(
        DOUBLE, 1D, Double.MAX_VALUE,
        value -> (min, max) -> {

            if (value < min || value > max) {
                throw new IllegalArgumentException("Expected double to be greater than 0! (current value: " + value + ")");
            }

            return value;

        }
    );

    public static final SerializableDataType<String> STRING = new SerializableDataType<>(
        String.class,
        PacketByteBuf::writeString,
        (buf) -> buf.readString(32767),
        JsonElement::getAsString,
        JsonPrimitive::new);

    public static final SerializableDataType<List<String>> STRINGS = SerializableDataType.list(STRING);

    public static final SerializableDataType<Number> NUMBER = new SerializableDataType<>(
        Number.class,
        (buf, number) -> {
            if(number instanceof Double) {
                buf.writeByte(0);
                buf.writeDouble(number.doubleValue());
            } else if(number instanceof Float) {
                buf.writeByte(1);
                buf.writeFloat(number.floatValue());
            } else if(number instanceof Integer) {
                buf.writeByte(2);
                buf.writeInt(number.intValue());
            } else if(number instanceof Long) {
                buf.writeByte(3);
                buf.writeLong(number.longValue());
            } else {
                buf.writeByte(4);
                buf.writeString(number.toString());
            }
        },
        buf -> {
            byte type = buf.readByte();
            return switch (type) {
                case 0 -> buf.readDouble();
                case 1 -> buf.readFloat();
                case 2 -> buf.readInt();
                case 3 -> buf.readLong();
                case 4 -> new LazilyParsedNumber(buf.readString());
                default -> throw new RuntimeException("Could not receive number, unexpected type id \"" + type + "\" (allowed range: [0-4])");
            };
        },
        je -> {
            if(je.isJsonPrimitive()) {
                JsonPrimitive primitive = je.getAsJsonPrimitive();
                if(primitive.isNumber()) {
                    return primitive.getAsNumber();
                } else if(primitive.isBoolean()) {
                    return primitive.getAsBoolean() ? 1 : 0;
                }
            }
            throw new JsonParseException("Expected a primitive");
        },
        number -> {
            if(number instanceof Double) {
                return new JsonPrimitive(number.doubleValue());
            } else if(number instanceof Float) {
                return new JsonPrimitive(number.floatValue());
            } else if(number instanceof Integer) {
                return new JsonPrimitive(number.intValue());
            } else if(number instanceof Long) {
                return new JsonPrimitive(number.longValue());
            } else {
                return new JsonPrimitive(number.toString());
            }
        });

    public static final SerializableDataType<List<Number>> NUMBERS = SerializableDataType.list(NUMBER);

    public static final SerializableDataType<Vec3d> VECTOR = new SerializableDataType<>(Vec3d.class,
        (packetByteBuf, vector3d) -> {
            packetByteBuf.writeDouble(vector3d.x);
            packetByteBuf.writeDouble(vector3d.y);
            packetByteBuf.writeDouble(vector3d.z);
        },
        (packetByteBuf -> new Vec3d(
            packetByteBuf.readDouble(),
            packetByteBuf.readDouble(),
            packetByteBuf.readDouble())),
        (jsonElement -> {
            if(jsonElement.isJsonObject()) {
                JsonObject jo = jsonElement.getAsJsonObject();
                return new Vec3d(
                    JsonHelper.getDouble(jo, "x", 0),
                    JsonHelper.getDouble(jo, "y", 0),
                    JsonHelper.getDouble(jo, "z", 0)
                );
            } else {
                throw new JsonParseException("Expected an object with x, y, and z fields.");
            }
        }),
        (vec3d) -> {
            JsonObject jo = new JsonObject();
            jo.addProperty("x", vec3d.x);
            jo.addProperty("y", vec3d.y);
            jo.addProperty("z", vec3d.z);
            return jo;
        });

    public static final SerializableDataType<Identifier> IDENTIFIER = new SerializableDataType<>(
        Identifier.class,
        PacketByteBuf::writeIdentifier,
        PacketByteBuf::readIdentifier,
        DynamicIdentifier::of,
        identifier -> new JsonPrimitive(identifier.toString())
    );

    public static final SerializableDataType<List<Identifier>> IDENTIFIERS = SerializableDataType.list(IDENTIFIER);

    public static final SerializableDataType<RegistryKey<Enchantment>> ENCHANTMENT = SerializableDataType.registryKey(RegistryKeys.ENCHANTMENT);

    private static final Set<RegistryKey<World>> VANILLA_DIMENSIONS = Set.of(
        World.OVERWORLD,
        World.NETHER,
        World.END
    );

    public static SerializableDataType<RegistryKey<World>> DIMENSION = SerializableDataType.registryKey(RegistryKeys.WORLD, VANILLA_DIMENSIONS);

    public static final SerializableDataType<EntityAttribute> ATTRIBUTE = SerializableDataType.registry(EntityAttribute.class, Registries.ATTRIBUTE);

    public static final SerializableDataType<List<EntityAttribute>> ATTRIBUTES = SerializableDataType.list(ATTRIBUTE);

    public static final SerializableDataType<RegistryEntry<EntityAttribute>> ATTRIBUTE_ENTRY = SerializableDataType.registryEntry(Registries.ATTRIBUTE);

    public static final SerializableDataType<List<RegistryEntry<EntityAttribute>>> ATTRIBUTE_ENTRIES = SerializableDataType.list(ATTRIBUTE_ENTRY);

    public static final SerializableDataType<EntityAttributeModifier.Operation> MODIFIER_OPERATION = SerializableDataType.enumValue(EntityAttributeModifier.Operation.class);

    public static final SerializableDataType<EntityAttributeModifier> ATTRIBUTE_MODIFIER = SerializableDataType.compound(
        EntityAttributeModifier.class,
        new SerializableData()
            .add("id", IDENTIFIER)
            .add("value", DOUBLE)
            .add("operation", MODIFIER_OPERATION),
        data -> new EntityAttributeModifier(
            data.getId("id"),
            data.getDouble("value"),
            data.get("operation")
        ),
        (serializableData, entityAttributeModifier) -> {

            SerializableData.Instance data = serializableData.new Instance();

            data.set("id", entityAttributeModifier.id());
            data.set("value", entityAttributeModifier.value());
            data.set("operation", entityAttributeModifier.operation());

            return data;

        }
    );

    public static final SerializableDataType<List<EntityAttributeModifier>> ATTRIBUTE_MODIFIERS =
        SerializableDataType.list(ATTRIBUTE_MODIFIER);

    public static final SerializableDataType<Item> ITEM = SerializableDataType.registry(Item.class, Registries.ITEM);

    public static final SerializableDataType<StatusEffect> STATUS_EFFECT = SerializableDataType.registry(StatusEffect.class, Registries.STATUS_EFFECT);

    public static final SerializableDataType<List<StatusEffect>> STATUS_EFFECTS =
        SerializableDataType.list(STATUS_EFFECT);

    public static final SerializableDataType<RegistryEntry<StatusEffect>> STATUS_EFFECT_ENTRY = SerializableDataType.registryEntry(Registries.STATUS_EFFECT);

    public static final SerializableDataType<List<RegistryEntry<StatusEffect>>> STATUS_EFFECT_ENTRIES = SerializableDataType.list(STATUS_EFFECT_ENTRY);

    public static final SerializableDataType<StatusEffectInstance> STATUS_EFFECT_INSTANCE = SerializableDataType.compound(
        ClassUtil.castClass(StatusEffectInstance.class),
        new SerializableData()
            .add("effect", STATUS_EFFECT_ENTRY)
            .add("duration", INT, 100)
            .add("amplifier", INT, 0)
            .add("is_ambient", BOOLEAN, false)
            .add("show_particles", BOOLEAN, true)
            .add("show_icon", BOOLEAN, true),
        data -> new StatusEffectInstance(
            data.get("effect"),
            data.getInt("duration"),
            data.getInt("amplifier"),
            data.getBoolean("is_ambient"),
            data.getBoolean("show_particles"),
            data.getBoolean("show_icon")
        ),
        (serializableData, statusEffectInstance) -> {

            SerializableData.Instance data = serializableData.new Instance();

            data.set("effect", statusEffectInstance.getEffectType());
            data.set("duration", statusEffectInstance.getDuration());
            data.set("amplifier", statusEffectInstance.getAmplifier());
            data.set("is_ambient", statusEffectInstance.isAmbient());
            data.set("show_particles", statusEffectInstance.shouldShowParticles());
            data.set("show_icon", statusEffectInstance.shouldShowIcon());

            return data;

        }
    );

    public static final SerializableDataType<List<StatusEffectInstance>> STATUS_EFFECT_INSTANCES =
        SerializableDataType.list(STATUS_EFFECT_INSTANCE);

    public static final SerializableDataType<TagKey<Item>> ITEM_TAG = SerializableDataType.tag(RegistryKeys.ITEM);

    public static final SerializableDataType<TagKey<Fluid>> FLUID_TAG = SerializableDataType.tag(RegistryKeys.FLUID);

    public static final SerializableDataType<TagKey<Block>> BLOCK_TAG = SerializableDataType.tag(RegistryKeys.BLOCK);

    public static final SerializableDataType<TagKey<EntityType<?>>> ENTITY_TAG = SerializableDataType.tag(RegistryKeys.ENTITY_TYPE);

    public static final SerializableDataType<Ingredient.Entry> INGREDIENT_ENTRY = SerializableDataType.compound(
        ClassUtil.castClass(Ingredient.Entry.class),
        new SerializableData()
            .add("tag", ITEM_TAG, null)
            .add("item", ITEM, null),
        data -> {

            boolean isTagPresent = data.isPresent("tag");
            boolean isItemPresent = data.isPresent("item");

            if (isTagPresent == isItemPresent) {
                throw new JsonParseException("An ingredient entry is either a tag or an item, " + (isTagPresent ? "not both." : "one has to be provided."));
            }

            if (isTagPresent) {
                TagKey<Item> tag = data.get("tag");
                return new Ingredient.TagEntry(tag);
            } else {

                Item item = data.get("item");
                ItemStack stack = new ItemStack(item);

                return new Ingredient.StackEntry(stack);

            }

        },
        (serializableData, entry) -> {

            SerializableData.Instance data = serializableData.new Instance();

            if (entry instanceof Ingredient.TagEntry tagEntry) {
                data.set("tag", tagEntry.tag());
                data.set("item", null);
            } else if (entry instanceof Ingredient.StackEntry stackEntry) {
                data.set("tag", null);
                data.set("item", stackEntry.stack().getItem());
            } else {
                throw new RuntimeException("Tried to write an ingredient that was not a tag or an item!");
            }

            return data;

        }
    );

    public static final SerializableDataType<List<Ingredient.Entry>> INGREDIENT_ENTRIES = SerializableDataType.list(INGREDIENT_ENTRY);

    // An alternative version of an ingredient deserializer which allows `minecraft:air`
    public static final SerializableDataType<Ingredient> INGREDIENT = new SerializableDataType<>(
        ClassUtil.castClass(Ingredient.class),
        Ingredient.PACKET_CODEC::encode,
        Ingredient.PACKET_CODEC::decode,
        jsonElement -> {
            List<Ingredient.Entry> entries = INGREDIENT_ENTRIES.read(jsonElement);
            return Ingredient.ofEntries(entries.stream());
        },
        ingredient -> {
            List<Ingredient.Entry> entries = Arrays.asList(((IngredientAccessor) ingredient).getEntries());
            return INGREDIENT_ENTRIES.write(entries);
        }
    );

    // The regular vanilla Minecraft ingredient.
    public static final SerializableDataType<Ingredient> VANILLA_INGREDIENT = new SerializableDataType<>(
        ClassUtil.castClass(Ingredient.class),
        Ingredient.PACKET_CODEC::encode,
        Ingredient.PACKET_CODEC::decode,
        jsonElement -> Ingredient.DISALLOW_EMPTY_CODEC
            .parse(JsonOps.INSTANCE, jsonElement)
            .mapError(err -> "Couldn't deserialize ingredient from JSON: " + err)
            .getOrThrow(),
        ingredient -> Ingredient.DISALLOW_EMPTY_CODEC
            .encodeStart(JsonOps.INSTANCE, ingredient)
            .mapError(err -> "Couldn't serialize ingredient to JSON: " + err)
            .getOrThrow()
    );

    public static final SerializableDataType<Block> BLOCK = SerializableDataType.registry(Block.class, Registries.BLOCK);

    public static final SerializableDataType<BlockState> BLOCK_STATE = SerializableDataType.wrap(BlockState.class, STRING,
        BlockArgumentParser::stringifyBlockState,
        string -> {
            try {
                return BlockArgumentParser.block(Registries.BLOCK.getReadOnlyWrapper(), string, false).blockState();
            } catch (CommandSyntaxException e) {
                throw new JsonParseException(e);
            }
        });

    public static final SerializableDataType<RegistryKey<DamageType>> DAMAGE_TYPE = SerializableDataType.registryKey(RegistryKeys.DAMAGE_TYPE);

    public static final SerializableDataType<TagKey<EntityType<?>>> ENTITY_GROUP_TAG = SerializableDataType.mapped(ClassUtil.castClass(TagKey.class), HashBiMap.create(ImmutableMap.of(
        "undead", EntityTypeTags.UNDEAD,
        "arthropod", EntityTypeTags.ARTHROPOD,
        "illager", EntityTypeTags.ILLAGER,
        "aquatic", EntityTypeTags.AQUATIC
    )));

    public static final SerializableDataType<EquipmentSlot> EQUIPMENT_SLOT = SerializableDataType.enumValue(EquipmentSlot.class);

    public static final SerializableDataType<SoundEvent> SOUND_EVENT = SerializableDataType.wrap(
        SoundEvent.class,
        IDENTIFIER,
        SoundEvent::getId,
        SoundEvent::of
    );

    public static final SerializableDataType<EntityType<?>> ENTITY_TYPE = SerializableDataType.registry(ClassUtil.castClass(EntityType.class), Registries.ENTITY_TYPE);

    public static final SerializableDataType<ParticleType<?>> PARTICLE_TYPE = SerializableDataType.registry(ClassUtil.castClass(ParticleType.class), Registries.PARTICLE_TYPE);

    public static final PacketCodec<ByteBuf, NbtCompound> UNLIMITED_NBT_COMPOUND_PACKET_CODEC = PacketCodecs.nbtCompound(NbtSizeTracker::ofUnlimitedBytes);

    public static final SerializableDataType<NbtCompound> NBT = new SerializableDataType<>(
        ClassUtil.castClass(NbtCompound.class),
        UNLIMITED_NBT_COMPOUND_PACKET_CODEC::encode,
        UNLIMITED_NBT_COMPOUND_PACKET_CODEC::decode,
        jsonElement -> Codec.withAlternative(NbtCompound.CODEC, StringNbtReader.NBT_COMPOUND_CODEC)
            .parse(JsonOps.INSTANCE, jsonElement)
            .getOrThrow(),
        nbtCompound -> NbtCompound.CODEC
            .encodeStart(JsonOps.INSTANCE, nbtCompound)
            .mapError(err -> "Couldn't serialize NBT compound to JSON (skipping): " + err)
            .resultOrPartial(Calio.LOGGER::warn)
            .orElseGet(JsonObject::new)
    );

    public static final SerializableDataType<ParticleEffect> PARTICLE_EFFECT = SerializableDataType.compound(
        ClassUtil.castClass(ParticleEffect.class),
        new SerializableData()
            .add("type", PARTICLE_TYPE)
            .add("params", NBT, null),
        data -> {

            ParticleType<? extends ParticleEffect> particleType = data.get("type");
            NbtCompound paramsNbt = data.get("params");

            Identifier particleTypeId = Objects.requireNonNull(Registries.PARTICLE_TYPE.getId(particleType));
            if (particleType instanceof SimpleParticleType simpleType) {
                return simpleType;
            }

            else if (paramsNbt == null || paramsNbt.isEmpty()) {
                throw new JsonSyntaxException("Expected parameters for particle effect \"" + particleTypeId + "\"");
            }

            else {
                paramsNbt.putString("type", particleTypeId.toString());
                return ParticleTypes.TYPE_CODEC
                    .parse(NbtOps.INSTANCE, paramsNbt)
                    .getOrThrow();
            }

        },
        (serializableData, particleEffect) -> {

            SerializableData.Instance data = serializableData.new Instance();
            ParticleType<?> particleType = particleEffect.getType();

            data.set("type", particleType);
            data.set("params", ParticleTypes.TYPE_CODEC
                .encodeStart(NbtOps.INSTANCE, particleEffect)
                .getOrThrow());

            return data;

        }
    );

    public static final SerializableDataType<ParticleEffect> PARTICLE_EFFECT_OR_TYPE = new SerializableDataType<>(ParticleEffect.class,
        PARTICLE_EFFECT::send,
        PARTICLE_EFFECT::receive,
        jsonElement -> {

            if (jsonElement instanceof JsonPrimitive jsonPrimitive && jsonPrimitive.isString()) {

                ParticleType<?> particleType = PARTICLE_TYPE.read(jsonPrimitive);

                if (particleType instanceof SimpleParticleType simpleParticleType) {
                    return simpleParticleType;
                }

            }

            else if (jsonElement instanceof JsonObject jsonObject) {
                return PARTICLE_EFFECT.read(jsonObject);
            }

            throw new IllegalArgumentException("Expected either a string with parameter-less particle effect, or a JSON object");

        },
        PARTICLE_EFFECT::write);

    public static final SerializableDataType<ComponentChanges> COMPONENT_CHANGES = new SerializableDataType<>(
        ClassUtil.castClass(ComponentChanges.class),
        ComponentChanges.PACKET_CODEC::encode,
        ComponentChanges.PACKET_CODEC::decode,
        jsonElement -> ComponentChanges.CODEC
            .parse(JsonOps.INSTANCE, jsonElement)
            .getOrThrow(JsonParseException::new),
        componentChanges -> ComponentChanges.CODEC
            .encodeStart(JsonOps.INSTANCE, componentChanges)
            .mapError(err -> "Failed to serialize component changes to JSON (skipping): " + err)
            .resultOrPartial(Calio.LOGGER::warn)
            .orElseGet(JsonObject::new)
    );

    public static final SerializableDataType<ItemStack> ITEM_STACK = SerializableDataType.compound(
        ItemStack.class,
        new SerializableData()
            .add("item", ITEM)
            .add("amount", INT, 1)
            .add("components", COMPONENT_CHANGES, ComponentChanges.EMPTY),
        data -> {

            Item item = data.get("item");
            ItemStack stack = item.getDefaultStack();

            stack.setCount(data.getInt("amount"));
            stack.applyChanges(data.get("components"));

            return stack;

        },
        (serializableData, stack) -> {

            SerializableData.Instance data = serializableData.new Instance();

            data.set("item", stack.getItem());
            data.set("amount", stack.getCount());
            data.set("components", stack.getComponentChanges());

            return data;

        }
    );

    public static final SerializableDataType<List<ItemStack>> ITEM_STACKS = SerializableDataType.list(ITEM_STACK);

    public static final SerializableDataType<Text> TEXT = new SerializableDataType<>(
        Text.class,
        TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC::encode,
        TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC::decode,
        jsonElement -> TextCodecs.CODEC
            .parse(JsonOps.INSTANCE, jsonElement)
            .getOrThrow(JsonParseException::new),
        text -> TextCodecs.CODEC
            .encodeStart(JsonOps.INSTANCE, text)
            .getOrThrow(JsonParseException::new)
    );

    public static final SerializableDataType<List<Text>> TEXTS = SerializableDataType.list(TEXT);

    public static final SerializableDataType<RecipeEntry<? extends Recipe<?>>> RECIPE = new SerializableDataType<>(
        ClassUtil.castClass(RecipeEntry.class),
        (buf, recipeEntry) -> {

            NbtElement recipeNbt = Recipe.CODEC
                .encodeStart(NbtOps.INSTANCE, recipeEntry.value())
                .getOrThrow(NbtException::new);

            buf.writeIdentifier(recipeEntry.id());
            buf.writeNbt(recipeNbt);

        },
        buf -> {

            Identifier id = buf.readIdentifier();
            Recipe<?> recipe = Recipe.CODEC
                .parse(NbtOps.INSTANCE, buf.readNbt(NbtSizeTracker.ofUnlimitedBytes()))
                .getOrThrow(NbtException::new);

            return new RecipeEntry<>(id, recipe);

        },
        jsonElement -> {

            if (!(jsonElement instanceof JsonObject jsonObject)) {
                throw new JsonSyntaxException("Expected recipe to be a JSON object.");
            }

            Identifier id = IDENTIFIER.read(JsonHelper.getElement(jsonObject, "id"));
            Recipe<?> recipe = Recipe.CODEC
                .parse(JsonOps.INSTANCE, jsonObject)
                .getOrThrow(JsonParseException::new);

            return new RecipeEntry<>(id, recipe);

        },
        recipeEntry -> {

            JsonObject recipeJson = Recipe.CODEC.encodeStart(JsonOps.INSTANCE, recipeEntry.value())
                .mapError(err -> "Failed to serialize recipe to JSON (skipping): " + err)
                .resultOrPartial(Calio.LOGGER::warn)
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject)
                .orElseGet(JsonObject::new);

            recipeJson.addProperty("id", recipeEntry.id().toString());
            return recipeJson;
            
        }
    );

    public static final SerializableDataType<GameEvent> GAME_EVENT = SerializableDataType.registry(GameEvent.class, Registries.GAME_EVENT);

    public static final SerializableDataType<List<GameEvent>> GAME_EVENTS = SerializableDataType.list(GAME_EVENT);

    public static final SerializableDataType<RegistryEntry<GameEvent>> GAME_EVENT_ENTRY = SerializableDataType.registryEntry(Registries.GAME_EVENT);

    public static final SerializableDataType<List<RegistryEntry<GameEvent>>> GAME_EVENT_ENTRIES = SerializableDataType.list(GAME_EVENT_ENTRY);

    public static final SerializableDataType<TagKey<GameEvent>> GAME_EVENT_TAG = SerializableDataType.tag(RegistryKeys.GAME_EVENT);

    public static final SerializableDataType<Fluid> FLUID = SerializableDataType.registry(Fluid.class, Registries.FLUID);

    public static final SerializableDataType<CameraSubmersionType> CAMERA_SUBMERSION_TYPE = SerializableDataType.enumValue(CameraSubmersionType.class);

    public static final SerializableDataType<Hand> HAND = SerializableDataType.enumValue(Hand.class);

    public static final SerializableDataType<EnumSet<Hand>> HAND_SET = SerializableDataType.enumSet(Hand.class, HAND);

    public static final SerializableDataType<EnumSet<EquipmentSlot>> EQUIPMENT_SLOT_SET = SerializableDataType.enumSet(EquipmentSlot.class, EQUIPMENT_SLOT);

    public static final SerializableDataType<ActionResult> ACTION_RESULT = SerializableDataType.enumValue(ActionResult.class);

    public static final SerializableDataType<UseAction> USE_ACTION = SerializableDataType.enumValue(UseAction.class);

    public static final SerializableDataType<StatusEffectChance> STATUS_EFFECT_CHANCE =
        SerializableDataType.compound(StatusEffectChance.class, new SerializableData()
            .add("effect", STATUS_EFFECT_INSTANCE)
            .add("chance", FLOAT, 1.0F),
            (data) -> {
                StatusEffectChance sec = new StatusEffectChance();
                sec.statusEffectInstance = data.get("effect");
                sec.chance = data.getFloat("chance");
                return sec;
            },
            (data, csei) -> {
                SerializableData.Instance inst = data.new Instance();
                inst.set("effect", csei.statusEffectInstance);
                inst.set("chance", csei.chance);
                return inst;
            });

    public static final SerializableDataType<List<StatusEffectChance>> STATUS_EFFECT_CHANCES = SerializableDataType.list(STATUS_EFFECT_CHANCE);

    public static final SerializableDataType<FoodComponent.StatusEffectEntry> FOOD_STATUS_EFFECT_ENTRY = new SerializableDataType<>(
        ClassUtil.castClass(FoodComponent.StatusEffectEntry.class),
        FoodComponent.StatusEffectEntry.PACKET_CODEC::encode,
        FoodComponent.StatusEffectEntry.PACKET_CODEC::decode,
        jsonElement -> FoodComponent.StatusEffectEntry.CODEC
            .parse(JsonOps.INSTANCE, jsonElement)
            .getOrThrow(JsonParseException::new),
        statusEffectEntry -> FoodComponent.StatusEffectEntry.CODEC
            .encodeStart(JsonOps.INSTANCE, statusEffectEntry)
            .mapError(err -> "Failed to serialize status effect entry to JSON (skipping): " + err)
            .resultOrPartial(Calio.LOGGER::warn)
            .orElseGet(JsonObject::new)
    );

    public static final SerializableDataType<List<FoodComponent.StatusEffectEntry>> FOOD_STATUS_EFFECT_ENTRIES = SerializableDataType.list(FOOD_STATUS_EFFECT_ENTRY);

    public static final SerializableDataType<FoodComponent> FOOD_COMPONENT = SerializableDataType.compound(
        ClassUtil.castClass(FoodComponent.class),
        new SerializableData()
            .add("hunger", INT)
            .add("saturation", FLOAT)
            .add("always_edible", BOOLEAN, false)
            .add("snack", BOOLEAN, false)
            .add("effect", FOOD_STATUS_EFFECT_ENTRY, null)
            .add("effects", FOOD_STATUS_EFFECT_ENTRIES, null)
            .add("using_converts_to", ITEM_STACK, null),
        data -> {

            FoodComponent.Builder builder = new FoodComponent.Builder()
                .nutrition(data.getInt("hunger"))
                .saturationModifier(data.getFloat("saturation"));

            if (data.getBoolean("always_edible")) {
                builder.alwaysEdible();
            }

            if (data.getBoolean("snack")) {
                builder.snack();
            }

            data.<FoodComponent.StatusEffectEntry>ifPresent("effect", effectEntry ->
                builder.statusEffect(effectEntry.effect(), effectEntry.probability())
            );

            data.<List<FoodComponent.StatusEffectEntry>>ifPresent("effects", effectEntries -> effectEntries.forEach(effectEntry ->
                builder.statusEffect(effectEntry.effect(), effectEntry.probability())
            ));

            if (data.isPresent("using_converts_to")) {
                builder.usingConvertsTo = Optional.of(data.get("using_converts_to"));
            }

            return builder.build();

        },
        (serializableData, foodComponent) -> {

            SerializableData.Instance data = serializableData.new Instance();

            data.set("hunger", foodComponent.nutrition());
            data.set("saturation", foodComponent.saturation());
            data.set("always_edible", foodComponent.canAlwaysEat());
            data.set("snack", foodComponent.getEatTicks() == 16);
            data.set("effect", null);
            data.set("effects", foodComponent.effects());
            data.set("using_converts_to", foodComponent.usingConvertsTo().orElse(null));

            return data;

        }
    );

    public static final SerializableDataType<Direction> DIRECTION = SerializableDataType.enumValue(Direction.class);

    public static final SerializableDataType<EnumSet<Direction>> DIRECTION_SET = SerializableDataType.enumSet(Direction.class, DIRECTION);

    public static final SerializableDataType<Class<?>> CLASS = SerializableDataType.wrap(ClassUtil.castClass(Class.class), SerializableDataTypes.STRING,
        Class::getName,
        str -> {
            try {
                return Class.forName(str);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Specified class does not exist: \"" + str + "\".");
            }
        });

    public static final SerializableDataType<RaycastContext.ShapeType> SHAPE_TYPE = SerializableDataType.enumValue(RaycastContext.ShapeType.class);

    public static final SerializableDataType<RaycastContext.FluidHandling> FLUID_HANDLING = SerializableDataType.enumValue(RaycastContext.FluidHandling.class);

    public static final SerializableDataType<Explosion.DestructionType> DESTRUCTION_TYPE = SerializableDataType.enumValue(Explosion.DestructionType.class);

    public static final SerializableDataType<Direction.Axis> AXIS = SerializableDataType.enumValue(Direction.Axis.class);

    public static final SerializableDataType<EnumSet<Direction.Axis>> AXIS_SET = SerializableDataType.enumSet(Direction.Axis.class, AXIS);

    public static final SerializableDataType<ArgumentWrapper<NbtPathArgumentType.NbtPath>> NBT_PATH =
        SerializableDataType.argumentType(NbtPathArgumentType.nbtPath());

    public static final SerializableDataType<RaycastContext.ShapeType> RAYCAST_SHAPE_TYPE = SerializableDataType.enumValue(RaycastContext.ShapeType.class);

    public static final SerializableDataType<RaycastContext.FluidHandling> RAYCAST_FLUID_HANDLING = SerializableDataType.enumValue(RaycastContext.FluidHandling.class);

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final SerializableDataType<Stat<?>> STAT = SerializableDataType.compound(
        ClassUtil.castClass(Stat.class),
        new SerializableData()
            .add("type", SerializableDataType.registry(ClassUtil.castClass(StatType.class), Registries.STAT_TYPE))
            .add("id", IDENTIFIER),
        data -> {

            StatType statType = data.get("type");
            Identifier statId = data.get("id");

            Registry statRegistry = statType.getRegistry();
            if (statRegistry.containsId(statId)) {
                return statType.getOrCreateStat(statRegistry.get(statId));
            }

            else {
                throw new IllegalArgumentException("Desired stat \"" + statId + "\" does not exist in stat type \"" + Registries.STAT_TYPE.getId(statType) + "\"");
            }

        },
        (serializableData, stat) -> {

            SerializableData.Instance data = serializableData.new Instance();

            StatType statType = stat.getType();
            Optional<Identifier> statId = Optional.ofNullable(statType.getRegistry().getId(stat.getValue()));

            data.set("type", statType);
            statId.ifPresent(id -> data.set("id", id));

            return data;

        }
    );

    public static final SerializableDataType<TagKey<Biome>> BIOME_TAG = SerializableDataType.tag(RegistryKeys.BIOME);

    public static final SerializableDataType<TagLike<Item>> ITEM_TAG_LIKE = SerializableDataType.tagLike(Registries.ITEM);

    public static final SerializableDataType<TagLike<Block>> BLOCK_TAG_LIKE = SerializableDataType.tagLike(Registries.BLOCK);

    public static final SerializableDataType<TagLike<EntityType<?>>> ENTITY_TYPE_TAG_LIKE = SerializableDataType.tagLike(Registries.ENTITY_TYPE);

    public static final SerializableDataType<PotionContentsComponent> POTION_CONTENTS_COMPONENT = new SerializableDataType<>(
        ClassUtil.castClass(PotionContentsComponent.class),
        PotionContentsComponent.PACKET_CODEC::encode,
        PotionContentsComponent.PACKET_CODEC::decode,
        jsonElement -> PotionContentsComponent.CODEC
            .parse(JsonOps.INSTANCE, jsonElement)
            .getOrThrow(JsonParseException::new),
        potionContentsComponent -> PotionContentsComponent.CODEC
            .encodeStart(JsonOps.INSTANCE, potionContentsComponent)
            .mapError(err -> "Couldn't serialize potion content components to JSON (skipping): " + err)
            .result()
            .orElseGet(JsonObject::new)
    );

    public static final SerializableDataType<RegistryKey<LootFunction>> ITEM_MODIFIER = SerializableDataType.registryKey(RegistryKeys.ITEM_MODIFIER);

    public static final SerializableDataType<RegistryKey<LootCondition>> PREDICATE = SerializableDataType.registryKey(RegistryKeys.PREDICATE);

}
