package io.github.apace100.calio.data;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.codec.*;
import io.github.apace100.calio.mixin.WeightedListAccessor;
import io.github.apace100.calio.util.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.WeightedList;
import net.minecraft.util.function.ValueLists;
import org.apache.commons.lang3.EnumUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unused", "unchecked"})
public class SerializableDataType<T> implements StrictCodec<T> {

    protected final StrictCodec<T> baseCodec;
    protected final PacketCodec<RegistryByteBuf, T> packetCodec;

    protected final String name;

    public SerializableDataType(StrictCodec<T> baseCodec, PacketCodec<RegistryByteBuf, T> packetCodec, String name) {
        this.baseCodec = baseCodec;
        this.packetCodec = packetCodec;
        this.name = name;
    }

    public SerializableDataType(StrictCodec<T> baseCodec) {
        this(baseCodec, PacketCodecs.codec(baseCodec).cast());
    }

    public SerializableDataType(StrictCodec<T> baseCodec, PacketCodec<RegistryByteBuf, T> packetCodec) {
        this(baseCodec, packetCodec, "SerializableDataType[" + baseCodec + "]");
    }

    /**
     *  <p>Use {@link #of(Codec)}, {@link #of(Codec, PacketCodec)}, or {@link #jsonBacked(BiConsumer, Function, Function, Function)}
     *  (if you want to keep using the JSON codec for encoding/decoding the type) instead.</p>
     */
    @Deprecated
    public SerializableDataType(Class<?> dataClass, BiConsumer<RegistryByteBuf, T> send, Function<RegistryByteBuf, T> receive, Function<JsonElement, T> fromJson, Function<T, JsonElement> toJson) {
        this(new JsonCodec<>(fromJson, toJson), PacketCodec.of((value, buf) -> send.accept(buf, value), receive::apply));
    }

    @Override
    public <T1> Pair<T, T1> strictDecode(DynamicOps<T1> ops, T1 input) {
        return this.baseCodec().strictDecode(ops, input);
    }

    @Override
    public <T1> T1 strictEncode(T input, DynamicOps<T1> ops, T1 prefix) {
        return this.baseCodec().strictEncode(input, ops, prefix);
    }

    @Override
    public SerializableDataType<List<T>> listOf() {
        return this.listOf(0, Integer.MAX_VALUE);
    }

    @Override
    public Codec<List<T>> sizeLimitedListOf(int maxSize) {
        return this.listOf(0, maxSize);
    }

    @Override
    public SerializableDataType<List<T>> listOf(int minSize, int maxSize) {
        StrictListCodec<T> listCodec = new StrictListCodec<>(this.baseCodec(), minSize, maxSize);
        return new SerializableDataType<>(
            new StrictCodec<>() {

                @Override
                public <I> Pair<List<T>, I> strictDecode(DynamicOps<I> ops, I input) {

                    ImmutableList.Builder<T> listBuilder = ImmutableList.builder();
                    if (ops.getList(input).result().isPresent()) {
                        listBuilder.addAll(listCodec.strictParse(ops, input));
                    }

                    else {
                        listBuilder.add(SerializableDataType.this.strictParse(ops, input));
                    }

                    return Pair.of(listBuilder.build(), input);

                }

                @Override
                public <I> I strictEncode(List<T> input, DynamicOps<I> ops, I prefix) {
                    return listCodec.strictEncode(input, ops, prefix);
                }

            },
            CalioPacketCodecs.collection(ObjectArrayList::new, this.packetCodec())
        );
    }

    @Override
    public <S> SerializableDataType<S> xmap(Function<? super T, ? extends S> to, Function<? super S, ? extends T> from) {
        return new SerializableDataType<>(
            StrictCodec.super.xmap(to, from),
            this.packetCodec().xmap(to, from)
        );
    }

    @Override
    public <S> SerializableDataType<S> comapFlatMap(Function<? super T, ? extends DataResult<? extends S>> to, Function<? super S, ? extends T> from) {
        return new SerializableDataType<>(
            StrictCodec.super.comapFlatMap(to, from),
            this.packetCodec().xmap(t -> to.apply(t).getOrThrow(), from)
        );
    }

    @Override
    public <S> SerializableDataType<S> flatComapMap(Function<? super T, ? extends S> to, Function<? super S, ? extends DataResult<? extends T>> from) {
        return new SerializableDataType<>(
            StrictCodec.super.flatComapMap(to, from),
            this.packetCodec().xmap(to, s -> from.apply(s).getOrThrow())
        );
    }

    @Override
    public <S> SerializableDataType<S> flatXmap(Function<? super T, ? extends DataResult<? extends S>> to, Function<? super S, ? extends DataResult<? extends T>> from) {
        return new SerializableDataType<>(
            StrictCodec.super.flatXmap(to, from),
            this.packetCodec().xmap(t -> to.apply(t).getOrThrow(), s -> from.apply(s).getOrThrow())
        );
    }

    @Override
    public String toString() {
        return name;
    }

    protected StrictCodec<T> baseCodec() {
        return baseCodec;
    }

    public PacketCodec<RegistryByteBuf, T> packetCodec() {
        return packetCodec;
    }

    public T receive(RegistryByteBuf buf) {
        return this.packetCodec().decode(buf);
    }

    public void send(RegistryByteBuf buf, T value) {
        this.packetCodec().encode(buf, value);
    }

    /**
     *  Use {@link #strictDecode(DynamicOps, Object)} with {@link JsonOps#INSTANCE} instead.
     */
    @Deprecated
    public T read(JsonElement jsonElement) {
        return this.strictParse(JsonOps.INSTANCE, jsonElement);
    }

    /**
     *  Use {@link #strictEncodeStart(DynamicOps, T)} with {@link JsonOps#INSTANCE} instead.
     */
    @Deprecated
    public JsonElement writeUnsafely(Object value) throws Exception {

        try {
            return this.write(this.cast(value));
        }

        catch (ClassCastException e) {
            throw new Exception(e);
        }

    }

    /**
     *  Use {@link #strictEncodeStart(DynamicOps, T)} with {@link JsonOps#INSTANCE} instead.
     */
    @Deprecated
    public JsonElement write(T value) {
        return this.strictEncodeStart(JsonOps.INSTANCE, value);
    }

    public T cast(Object data) {
        return (T) data;
    }

    public static <T> SerializableDataType<T> of(Codec<T> codec) {
        return switch (codec) {
            case SerializableDataType<T> selfDataType ->
                selfDataType;
            case StrictCodec<T> selfStrictCodec ->
                new SerializableDataType<>(selfStrictCodec);
            default ->
                new SerializableDataType<>(StrictCodec.of(codec));
        };
    }

    public static <T> SerializableDataType<T> of(Codec<T> codec, PacketCodec<RegistryByteBuf, T> packetCodec) {

        StrictCodec<T> strictCodec = codec instanceof StrictCodec<T> selfStrictCodec
            ? selfStrictCodec
            : StrictCodec.of(codec);

        return new SerializableDataType<>(strictCodec, packetCodec);

    }

    public static <T> SerializableDataType<T> jsonBacked(BiConsumer<RegistryByteBuf, T> send, Function<RegistryByteBuf, T> receive, Function<T, JsonElement> toJson, Function<JsonElement, T> fromJson) {
        return new SerializableDataType<>(
            new JsonCodec<>(fromJson, toJson),
            new PacketCodec<>() {

                @Override
                public T decode(RegistryByteBuf buf) {
                    return receive.apply(buf);
                }

                @Override
                public void encode(RegistryByteBuf buf, T value) {
                    send.accept(buf, value);
                }

            }
        );
    }

    public static <T> RecursiveSerializableDataType<T> recursive(Function<SerializableDataType<T>, SerializableDataType<T>> wrapped) {
        return new RecursiveSerializableDataType<>(wrapped);
    }

    public static <T> RecursiveSerializableDataType<T> lazy(Supplier<SerializableDataType<T>> delegate) {
        return recursive(self -> delegate.get());
    }

    /**
     *  Use any of {@link #listOf()}, {@link #listOf(int, int)}, or {@link #sizeLimitedListOf(int)} instead.
     */
    @Deprecated
    public static <T> SerializableDataType<List<T>> list(SerializableDataType<T> singleDataType) {
        return singleDataType.listOf(0, Integer.MAX_VALUE);
    }

    public static <T> SerializableDataType<WeightedList<T>> weightedList(SerializableDataType<T> singleDataType) {

        CompoundSerializableDataType<WeightedList.Entry<T>> entryDataType = compound(
            new SerializableData()
                .add("element", singleDataType)
                .add("weight", SerializableDataTypes.INT, 1),
            data -> new WeightedList.Entry<>(
                data.get("element"),
                data.get("weight")
            ),
            (entry, data) -> data
                .set("element", entry.getElement())
                .set("weight", entry.getWeight())
        );

        return new SerializableDataType<>(
            entryDataType.listOf().xmap(
                WeightedList::new,
                weightedList -> new ArrayList<>(((WeightedListAccessor<T>) weightedList).getEntries())
            ),
            CalioPacketCodecs.collection(ArrayList::new, entryDataType.packetCodec()).xmap(
                WeightedList::new,
                weightedList -> new ArrayList<>(((WeightedListAccessor<T>) weightedList).getEntries())
            )
        );

    }

    /**
     *  Use {@link #registry(Registry)} instead.
     */
    @Deprecated
    public static <T> SerializableDataType<T> registry(Class<T> dataClass, Registry<T> registry) {
        return registry(dataClass, registry, false);
    }

    /**
     *  Use {@link #registry(Registry, String)} instead.
     */
    @Deprecated
    public static <T> SerializableDataType<T> registry(Class<T> dataClass, Registry<T> registry, String defaultNamespace) {
        return registry(dataClass, registry, defaultNamespace, false);
    }

    /**
     *  Use {@link #registry(Registry, boolean)} instead.
     */
    @Deprecated
    public static <T> SerializableDataType<T> registry(Class<T> dataClass, Registry<T> registry, boolean showPossibleValues) {
        return registry(dataClass, registry, Identifier.DEFAULT_NAMESPACE, showPossibleValues);
    }

    /**
     *  Use {@link #registry(Registry, String, boolean)} instead.
     */
    @Deprecated
    public static <T> SerializableDataType<T> registry(Class<T> dataClass, Registry<T> registry, String defaultNamespace, boolean showPossibleValues) {
        return registry(dataClass, registry, defaultNamespace, null, (reg, id) -> {
            String possibleValues = showPossibleValues ? " Expected value to be any of " + String.join(", ", reg.getIds().stream().map(Identifier::toString).toList()) : "";
            return new RuntimeException("Type \"%s\" is not registered in registry \"%s\".%s".formatted(id, registry.getKey().getValue(), possibleValues));
        });
    }

    /**
     *  Use {@link #registry(Registry, BiFunction)} instead.
     */
    @Deprecated
    public static <T> SerializableDataType<T> registry(Class<T> dataClass, Registry<T> registry, BiFunction<Registry<T>, Identifier, RuntimeException> exception) {
        return registry(dataClass, registry, Identifier.DEFAULT_NAMESPACE, null, exception);
    }

    /**
     *  Use {@link #registry(Registry, String, IdentifierAlias, BiFunction)} instead.
     */
    @Deprecated
    public static <T> SerializableDataType<T> registry(Class<T> dataClass, Registry<T> registry, String defaultNamespace, @Nullable IdentifierAlias aliases, BiFunction<Registry<T>, Identifier, RuntimeException> exception) {
        return registry(registry, defaultNamespace, aliases, exception);
    }

    public static <T> SerializableDataType<T> registry(Registry<T> registry) {
        return registry(registry, false);
    }

    public static <T> SerializableDataType<T> registry(Registry<T> registry, String defaultNamespace) {
        return registry(registry, defaultNamespace, false);
    }

    public static <T> SerializableDataType<T> registry(Registry<T> registry, boolean showPossibleValues) {
        return registry(registry, Identifier.DEFAULT_NAMESPACE, showPossibleValues);
    }

    public static <T> SerializableDataType<T> registry(Registry<T> registry, String defaultNamespace, boolean showPossibleValues) {
        return registry(registry, defaultNamespace, null, (reg, id) -> {
            String possibleValues = showPossibleValues ? " Expected value to be any of " + String.join(", ", reg.getIds().stream().map(Identifier::toString).toList()) : "";
            return new IllegalArgumentException("Type \"%s\" is not registered in registry \"%s\".%s".formatted(id, registry.getKey().getValue(), possibleValues));
        });
    }

    public static <T> SerializableDataType<T> registry(Registry<T> registry, BiFunction<Registry<T>, Identifier, RuntimeException> exception) {
        return registry(registry, Identifier.DEFAULT_NAMESPACE, null, exception);
    }

    public static <T> SerializableDataType<T> registry(Registry<T> registry, String defaultNamespace, @Nullable IdentifierAlias aliases, BiFunction<Registry<T>, Identifier, RuntimeException> exception) {
        return lazy(() -> new SerializableDataType<>(
            new StrictCodec<>() {

                @Override
                public <T1> Pair<T, T1> strictDecode(DynamicOps<T1> ops, T1 input) {

                    Identifier id = ops.getStringValue(input)
                        .map(str -> DynamicIdentifier.of(str, defaultNamespace))
                        .getOrThrow();

                    return registry
                        .getOrEmpty(aliases == null ? id : aliases.resolveAlias(id, registry::containsId))
                        .map(t -> Pair.of(t, input))
                        .orElseThrow(() -> exception.apply(registry, id));

                }

                @Override
                public <T1> T1 strictEncode(T input, DynamicOps<T1> ops, T1 prefix) {
                    return Optional.ofNullable(registry.getId(input))
                        .map(Identifier::toString)
                        .map(ops::createString)
                        .orElseThrow(() -> new IllegalStateException("Entry '" + input + "' is not registered to registry \"" + registry.getKey().getValue() + "\""));
                }

            },
            PacketCodecs.registryValue(registry.getKey())
        ));
    }

    /**
     *
     *  <p>Use any of the following methods instead:</p>
     *
     *  <ul>
     *      <li>{@link #compound(SerializableData, Function, BiConsumer)} for normal processing</li>
     *      <li>{@link #compound(SerializableData, BiFunction, BiConsumer)} for processing with an unknown {@link DynamicOps}</li>
     *  </ul>
     */
    @Deprecated
    public static <T> SerializableDataType<T> compound(Class<T> dataClass, SerializableData serializableData, Function<SerializableData.Instance, T> fromData, BiFunction<SerializableData, T, SerializableData.Instance> toData) {
        return new SerializableDataType<>(
            new StrictCodec<>() {

                @Override
                public <I> Pair<T, I> strictDecode(DynamicOps<I> ops, I input) {

                    MapLike<I> mapInput = ops.getMap(input).getOrThrow();
                    SerializableData.Instance data = serializableData.strictDecode(ops, mapInput);

                    return Pair.of(fromData.apply(data), input);

                }

                @Override
                public <I> I strictEncode(T input, DynamicOps<I> ops, I prefix) {
                    SerializableData.Instance data = toData.apply(serializableData, input);
                    return serializableData.codec().strictEncodeStart(ops, data);
                }

            },
            new PacketCodec<>() {

                @Override
                public T decode(RegistryByteBuf buf) {
                    return fromData.apply(serializableData.receive(buf));
                }

                @Override
                public void encode(RegistryByteBuf buf, T value) {
                    serializableData.send(buf, toData.apply(serializableData, value));
                }

            }
        );
    }

    public static <T> CompoundSerializableDataType<T> compound(SerializableData serializableData, Function<SerializableData.Instance, T> fromData, BiConsumer<T, SerializableData.Instance> toData) {
        return CompoundSerializableDataType.of(serializableData, fromData, toData);
    }

    public static <T> CompoundSerializableDataType<T> compound(SerializableData serializableData, BiFunction<SerializableData.Instance, DynamicOps<?>, T> fromData, BiConsumer<T, SerializableData.Instance> toData) {
        return CompoundSerializableDataType.of(serializableData, fromData, toData);
    }

    public static <V> SerializableDataType<Map<String, V>> map(SerializableDataType<V> valueDataType) {
        return map(
            of(Codec.STRING, PacketCodecs.STRING.cast()),
            valueDataType
        );
    }

    public static <K, V> SerializableDataType<Map<K, V>> map(SerializableDataType<K> keyDataType, SerializableDataType<V> valueDataType) {
        return new SerializableDataType<>(
            new StrictUnboundedMapCodec<>(keyDataType, valueDataType),
            PacketCodec.ofStatic(
                (buf, map) -> {

                    buf.writeVarInt(map.size());
                    map.forEach((key, value) -> {
                        keyDataType.send(buf, key);
                        valueDataType.send(buf, value);
                    });

                },
                buf -> {

                    Map<K, V> map = new Object2ObjectLinkedOpenHashMap<>();
                    int size = buf.readVarInt();

                    for (int i = 0; i < size; i++) {

                        K key = keyDataType.receive(buf);
                        V value = valueDataType.receive(buf);

                        map.put(key, value);

                    }

                    return map;

                }
            )
        );
    }

    /**
     *  Use {@link #mapped(Supplier)} <b>(recommended)</b> or {@link #mapped(BiMap)} instead.
     */
    @Deprecated
    public static <T> SerializableDataType<T> mapped(Class<T> dataClass, @NotNull BiMap<String, T> biMap) {
        return mapped(biMap);
    }

    public static <V> SerializableDataType<V> mapped(BiMap<String, V> biMap) {
        return mapped(Suppliers.memoize(() -> biMap));
    }

    public static <V> SerializableDataType<V> mapped(Supplier<BiMap<String, V>> biMapSupplier) {
        return new SerializableDataType<>(
            new StrictCodec<>() {

                @Override
                public <T> Pair<V, T> strictDecode(DynamicOps<T> ops, T input) {

                    String inputString = ops
                        .getStringValue(input)
                        .getOrThrow();

                    BiMap<String, V> biMap = biMapSupplier.get();
                    if (biMap.containsKey(inputString)) {
                        return Pair.of(biMap.get(inputString), input);
                    } else {
                        throw new IllegalArgumentException("Expected value to be any of " + String.join(", ", biMap.keySet()));
                    }

                }

                @Override
                public <T> T strictEncode(V input, DynamicOps<T> ops, T prefix) {

                    BiMap<String, V> biMap = biMapSupplier.get();
                    String inputString = biMap.inverse().get(input);

                    return ops.createString(inputString);

                }

            },
            PacketCodec.ofStatic(
                (buf, value) -> {
                    BiMap<String, V> biMap = biMapSupplier.get();
                    buf.writeString(biMap.inverse().get(value));
                },
                buf -> {
                    BiMap<String, V> biMap = biMapSupplier.get();
                    return biMap.get(buf.readString());
                }
            )
        );
    }

    /**
     *  Use {@link #xmap(Function, Function)} instead.
     */
    @Deprecated
    public static <T, U> SerializableDataType<T> wrap(Class<T> dataClass, SerializableDataType<U> base, Function<T, U> toFunction, Function<U, T> fromFunction) {
        return base.xmap(fromFunction, toFunction);
    }

    /**
     *  Use {@link #tagKey(RegistryKey)} instead.
     */
    @Deprecated
    public static <T> SerializableDataType<TagKey<T>> tag(RegistryKey<? extends Registry<T>> registryRef) {
        return tagKey(registryRef);
    }

    public static <T> SerializableDataType<TagKey<T>> tagKey(RegistryKey<? extends Registry<T>> registryRef) {
        return lazy(() -> new SerializableDataType<>(
            new StrictCodec<>() {

                @Override
                public <I> Pair<TagKey<T>, I> strictDecode(DynamicOps<I> ops, I input) {

                    Pair<TagKey<T>, I> tagAndInput = this.createTagKey(ops, input);
                    TagKey<T> tag = tagAndInput.getFirst();

                    if (Calio.getRegistryTags().map(tags -> tags.containsKey(tag)).orElse(false)) {
                        return tagAndInput;
                    }

                    else {
                        return Calio.getOptionalEntries(ops, tag)
                            .map(registryEntries -> tagAndInput)
                            .orElseThrow(() -> createError(tag));
                    }

                }

                @Override
                public <I> DataResult<Pair<TagKey<T>, I>> decode(DynamicOps<I> ops, I input) {

                    Pair<TagKey<T>, I> tagAndInput = this.createTagKey(ops, input);
                    TagKey<T> tag = tagAndInput.getFirst();

                    if (Calio.getRegistryTags().map(tags -> tags.containsKey(tag)).orElse(false)) {
                        return DataResult.success(tagAndInput);
                    }

                    else {
                        return Calio.getOptionalEntries(ops, tag)
                            .map(registryEntries -> tagAndInput)
                            .map(DataResult::success)
                            .orElseThrow(() -> createError(tag))
                            .setPartial(tagAndInput);
                    }

                }

                @Override
                public <I> I strictEncode(TagKey<T> input, DynamicOps<I> ops, I prefix) {
                    return SerializableDataTypes.IDENTIFIER.strictEncode(input.id(), ops, prefix);
                }

                private <I> Pair<TagKey<T>, I> createTagKey(DynamicOps<I> ops, I input) {
                    return SerializableDataTypes.IDENTIFIER
                        .strictDecode(ops, input)
                        .mapFirst(id -> TagKey.of(registryRef, id));
                }

                private IllegalArgumentException createError(TagKey<T> tagKey) {
                    return new IllegalArgumentException("Tag \"" + tagKey.id() + "\" for registry \"" + registryRef.getValue() + "\" doesn't exist!");
                }

            },
            PacketCodec.ofStatic(
                (buf, value) -> buf.writeIdentifier(value.id()),
                buf -> TagKey.of(registryRef, buf.readIdentifier())
            )
        ));
    }

    public static <T> SerializableDataType<RegistryEntry<T>> registryEntry(Registry<T> registry) {
        return lazy(() -> new SerializableDataType<>(
            new StrictCodec<>() {

                @Override
                public <I> Pair<RegistryEntry<T>, I> strictDecode(DynamicOps<I> ops, I input) {
                    Identifier id = SerializableDataTypes.IDENTIFIER.strictParse(ops, input);
                    return registry.getEntry(id)
                        .map(entry -> Pair.of((RegistryEntry<T>) entry, input))
                        .orElseThrow(() -> new IllegalArgumentException("Type \"" + id + "\" is not registered in registry \"" + registry.getKey().getValue() + "\""));
                }

                @Override
                public <I> I strictEncode(RegistryEntry<T> input, DynamicOps<I> ops, I prefix) {
                    return input.getKey()
                        .map(RegistryKey::getValue)
                        .map(Identifier::toString)
                        .map(ops::createString)
                        .orElseThrow(() -> new IllegalStateException("Entry \"" + input + "\" is not registered in registry \"" + registry.getKey().getValue() + "\";"));
                }

            },
            PacketCodecs.registryEntry(registry.getKey())
        ));
    }

    public static <T> SerializableDataType<RegistryKey<T>> registryKey(RegistryKey<? extends Registry<T>> registryRef) {
        return registryKey(registryRef, Set.of());
    }

    public static <T> SerializableDataType<RegistryKey<T>> registryKey(RegistryKey<? extends Registry<T>> registryRef, Collection<RegistryKey<T>> exemptions) {
        return lazy(() -> new SerializableDataType<>(
            new StrictCodec<>() {

                @Override
                public <I> Pair<RegistryKey<T>, I> strictDecode(DynamicOps<I> ops, I input) {

                    Pair<RegistryKey<T>, I> keyAndInput = this.createRegistryKey(ops, input);
                    RegistryKey<T> key = keyAndInput.getFirst();

                    if (exemptions.contains(key)) {
                        return keyAndInput;
                    }

                    else {
                        return Calio.getOptionalEntry(ops, key)
                            .map(reference -> keyAndInput)
                            .orElseThrow(() -> createError(key));
                    }

                }

                @Override
                public <I> DataResult<Pair<RegistryKey<T>, I>> decode(DynamicOps<I> ops, I input) {

                    Pair<RegistryKey<T>, I> keyAndInput = createRegistryKey(ops, input);
                    RegistryKey<T> key = keyAndInput.getFirst();

                    if (exemptions.contains(key)) {
                        return DataResult.success(keyAndInput);
                    }

                    else {
                        return Calio.getOptionalEntry(ops, key)
                            .map(reference -> keyAndInput)
                            .map(DataResult::success)
                            .orElseThrow(() -> createError(key))
                            .setPartial(keyAndInput);
                    }

                }

                @Override
                public <I> I strictEncode(RegistryKey<T> input, DynamicOps<I> ops, I prefix) {
                    return SerializableDataTypes.IDENTIFIER.strictEncode(input.getValue(), ops, prefix);
                }

                private <I> Pair<RegistryKey<T>, I> createRegistryKey(DynamicOps<I> ops, I input) {
                    return SerializableDataTypes.IDENTIFIER
                        .strictDecode(ops, input)
                        .mapFirst(id -> RegistryKey.of(registryRef, id));
                }

                private IllegalArgumentException createError(RegistryKey<T> registryKey) {
                    return new IllegalArgumentException("Type \"" + registryKey.getValue() + "\" is not registered in registry \"" + registryRef.getValue() + "\"!");
                }

            },
            PacketCodec.ofStatic(
                PacketByteBuf::writeRegistryKey,
                buf -> buf.readRegistryKey(registryRef)
            )
        ));
    }

    public static <E extends Enum<E>> SerializableDataType<E> enumValue(Class<E> enumClass) {
        return enumValueInternal(enumClass, HashMap::new);
    }

    public static <E extends Enum<E>> SerializableDataType<E> enumValue(Class<E> enumClass, Map<String, E> additionalMap) {
        return enumValue(enumClass, () -> additionalMap);
    }

    public static <E extends Enum<E>> SerializableDataType<E> enumValue(Class<E> enumClass, Supplier<Map<String, E>> additionalMapSupplier) {
        return enumValueInternal(enumClass, Suppliers.memoize(() -> additionalMapSupplier.get().entrySet()
            .stream()
            .map(e -> Map.entry(e.getKey().toUpperCase(Locale.ROOT), e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (prev, curr) -> curr, HashMap::new))));
    }

    @ApiStatus.Internal
    private static <E extends Enum<E>> SerializableDataType<E> enumValueInternal(Class<E> enumClass, Supplier<Map<String, E>> additionalMapSupplier) {
        IntFunction<E> ordinalToEnum = ValueLists.createIdToValueFunction((ToIntFunction<E>) Enum::ordinal, enumClass.getEnumConstants(), ValueLists.OutOfBoundsHandling.WRAP);
        return new SerializableDataType<>(
            new StrictCodec<>() {

                @Override
                public <I> I strictEncode(E input, DynamicOps<I> ops, I prefix) {
                    return ops.createString(input.name());
                }

                @Override
                public <I> Pair<E, I> strictDecode(DynamicOps<I> ops, I input) {

                    Map<String, E> additionalMap = additionalMapSupplier.get();
                    E[] enumValues = enumClass.getEnumConstants();

                    int enumsSize = enumValues.length;
                    DataResult<Number> ordinalInput = ops.getNumberValue(input);

                    if (ordinalInput.isSuccess()) {

                        int ordinal = ordinalInput
                            .map(Number::intValue)
                            .getOrThrow();

                        if (ordinal < 0 || ordinal >= enumsSize) {
                            throw new IllegalStateException("Expected ordinal to be within the range of 0 to " + (enumsSize - 1) + " (current value: " + ordinal + ")");
                        }

                        else {
                            return Pair.of(enumValues[ordinal], input);
                        }

                    }

                    else {

                        String name = ops.getStringValue(input)
                            .getOrThrow()
                            .toUpperCase(Locale.ROOT);

                        if (additionalMap.containsKey(name)) {
                            return Pair.of(additionalMap.get(name), input);
                        }

                        try {
                            E enumValue = EnumUtils.getEnumIgnoreCase(enumClass, name);
                            return Pair.of(Objects.requireNonNull(enumValue), input);
                        }

                        catch (Exception ignored) {

                            Set<String> validValues = new LinkedHashSet<>();

                            Stream.of(enumValues).map(Enum::name).forEach(validValues::add);
                            validValues.addAll(additionalMap.keySet());

                            throw new IllegalArgumentException("Expected value to be any of: " + String.join(", ", validValues) + " (case-insensitive)");

                        }

                    }

                }

            },
            PacketCodecs.indexed(ordinalToEnum, Enum::ordinal).cast()
        );
    }

    /**
     *  Use {@link #enumSet(SerializableDataType)} instead.
     */
    @Deprecated
    public static <E extends Enum<E>> SerializableDataType<EnumSet<E>> enumSet(Class<E> enumClass, SerializableDataType<E> enumDataType) {
        return enumSet(enumDataType);
    }

    public static <E extends Enum<E>> SerializableDataType<EnumSet<E>> enumSet(SerializableDataType<E> enumDataType) {
        StrictCodec<EnumSet<E>> setCodec = new StrictListCodec<>(enumDataType, 1, Integer.MAX_VALUE).xmap(EnumSet::copyOf, LinkedList::new);
        return new SerializableDataType<>(
            new StrictCodec<>() {

                @Override
                public <T> Pair<EnumSet<E>, T> strictDecode(DynamicOps<T> ops, T input) {

                    Set<E> values = new HashSet<>();
                    if (ops.getList(input).result().isPresent()) {
                        values.addAll(setCodec.strictParse(ops, input));
                    }

                    else {
                        values.add(enumDataType.strictParse(ops, input));
                    }

                    return Pair.of(EnumSet.copyOf(values), input);

                }

                @Override
                public <T> T strictEncode(EnumSet<E> input, DynamicOps<T> ops, T prefix) {
                    return setCodec.strictEncode(input, ops, prefix);
                }

            },
            CalioPacketCodecs.collection(HashSet::new, enumDataType.packetCodec()).xmap(
                EnumSet::copyOf,
                HashSet::new
            )
        );
    }

    /**
     *  Use either {@link #boundNumber(SerializableDataType, Number, BiFunction, Number, BiFunction)} or the built-in methods for ranged numbers, like {@link Codec#intRange(int, int)} and pass it to {@link #of(Codec)}
     *  (or {@link #of(Codec, PacketCodec)} if you want to pass its packet codec as well.)
     */
    @Deprecated
    public static <N extends Number & Comparable<N>> SerializableDataType<N> boundNumber(SerializableDataType<N> numberDataType, N min, N max, Function<N, BiFunction<N, N, N>> read) {
        return numberDataType.comapFlatMap(
            number -> {

                try {
                    return DataResult.success(read.apply(number).apply(min, max));
                }

                catch (Exception e) {
                    return DataResult.error(e::getMessage);
                }

            },
            Function.identity()
        );
    }

    public static <N extends Number & Comparable<N>> SerializableDataType<N> boundNumber(SerializableDataType<N> numberDataType, N min, N max) {
        return boundNumber(numberDataType,
            min, (value, _min) -> "Expected value to be at least " + _min + "! (current value: " + value + ")",
            max, (value, _max) -> "Expected value to be at most " + _max + "! (current value: " + value + ")"
        );
    }

    public static <N extends Number & Comparable<N>> SerializableDataType<N> boundNumber(SerializableDataType<N> numberDataType, N min, BiFunction<N, N, String> underMinError, N max, BiFunction<N, N, String> overMaxError) {
        return numberDataType.comapFlatMap(
            number -> {

                if (number.compareTo(min) < 0) {
                    return DataResult.error(() -> underMinError.apply(number, min));
                }

                else if (number.compareTo(max) > 0) {
                    return DataResult.error(() -> overMaxError.apply(number, max));
                }

                else {
                    return DataResult.success(number);
                }

            },
            Function.identity()
        );
    }

    public static <T, U extends ArgumentType<T>> SerializableDataType<ArgumentWrapper<T>> argumentType(U argumentType) {
        return lazy(() -> SerializableDataTypes.STRING.comapFlatMap(
            str -> {

                try {
                    return DataResult.success(new ArgumentWrapper<>(argumentType.parse(new StringReader(str)), str));
                }

                catch (Exception e) {
                    return DataResult.error(e::getMessage);
                }

            },
            ArgumentWrapper::input
        ));
    }

    public static <T> SerializableDataType<TagLike<T>> tagLike(Registry<T> registry) {
        return TagLike.dataType(registry);
    }

    public static <T> SerializableDataType<TagLike<T>> tagLike(RegistryKey<T> registryKey) {
        return TagLike.dataType(registryKey);
    }

    public static <T> SerializableDataType<Optional<T>> optional(SerializableDataType<T> dataType, boolean lenient) {
        return new SerializableDataType<>(
            new StrictCodec<>() {

                @Override
                public <I> Pair<Optional<T>, I> strictDecode(DynamicOps<I> ops, I input) {

                    try {
                        return dataType.strictDecode(ops, input).mapFirst(Optional::of);
                    }

                    catch (Exception e) {

                        if (lenient) {
                            return Pair.of(Optional.empty(), input);
                        }

                        else {
                            throw e;
                        }

                    }

                }

                @Override
                public <I> I strictEncode(Optional<T> input, DynamicOps<I> ops, I prefix) {
                    return input.map(t -> dataType.strictEncodeStart(ops, t)).orElse(prefix);
                }

            },
            PacketCodec.ofStatic(
                (buf, optional) -> {
                    buf.writeBoolean(optional.isPresent());
                    optional.ifPresent(t -> dataType.send(buf, t));
                },
                buf -> buf.readBoolean()
                    ? Optional.of(dataType.receive(buf))
                    : Optional.empty()
            )
        );
    }

}
