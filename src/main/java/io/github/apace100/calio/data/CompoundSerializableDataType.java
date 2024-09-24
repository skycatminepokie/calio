package io.github.apace100.calio.data;

import com.mojang.serialization.*;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.Optional;
import java.util.function.Function;

public class CompoundSerializableDataType<T> extends SerializableDataType<T> {

    private final SerializableData serializableData;

    private final Function<SerializableData, MapCodec<T>> mapCodecGetter;
    private final Function<SerializableData, PacketCodec<RegistryByteBuf, T>> packetCodecGetter;

    public CompoundSerializableDataType(SerializableData serializableData, Function<SerializableData, MapCodec<T>> mapCodecGetter, Function<SerializableData, PacketCodec<RegistryByteBuf, T>> packetCodecGetter, Optional<String> name, boolean root) {
        super(null, null, name, root);
        this.serializableData = serializableData;
        this.mapCodecGetter = mapCodecGetter;
        this.packetCodecGetter = packetCodecGetter;
    }

    public CompoundSerializableDataType(SerializableData serializableData, Function<SerializableData, MapCodec<T>> mapCodecGetter, Function<SerializableData, PacketCodec<RegistryByteBuf, T>> packetCodecGetter) {
        this(serializableData, mapCodecGetter, packetCodecGetter, Optional.empty(), true);
    }

    @Override
    public Codec<T> codec() {
        return mapCodec().codec();
    }

    @Override
    public PacketCodec<RegistryByteBuf, T> packetCodec() {
        return packetCodecGetter.apply(serializableData());
    }

    @Override
    public <S> CompoundSerializableDataType<S> xmap(Function<? super T, ? extends S> to, Function<? super S, ? extends T> from) {
        return new CompoundSerializableDataType<>(
            serializableData(),
            _serializableData -> mapCodecGetter
                .apply(_serializableData)
                .xmap(to, from),
            _serializableData -> packetCodecGetter
                .apply(_serializableData)
                .xmap(to, from),
            this.getName(),
            this.isRoot()
        );
    }

    @Override
    public <S> CompoundSerializableDataType<S> comapFlatMap(Function<? super T, ? extends DataResult<? extends S>> to, Function<? super S, ? extends T> from) {

        Function<? super T, ? extends S> toUnwrapped = t -> to.apply(t).getOrThrow();
        Function<? super S, ? extends DataResult<? extends T>> fromWrapped = s -> DataResult.success(from.apply(s));

        return new CompoundSerializableDataType<>(
            serializableData(),
            _serializableData -> mapCodecGetter
                .apply(_serializableData)
                .flatXmap(to, fromWrapped),
            _serializableData -> packetCodecGetter
                .apply(_serializableData)
                .xmap(toUnwrapped, from),
            this.getName(),
            this.isRoot()
        );

    }

    @Override
    public <S> CompoundSerializableDataType<S> flatComapMap(Function<? super T, ? extends S> to, Function<? super S, ? extends DataResult<? extends T>> from) {

        Function<? super T, ? extends DataResult<? extends S>> toWrapped = t -> DataResult.success(to.apply(t));
        Function<? super S, ? extends T> fromUnwrapped = s -> from.apply(s).getOrThrow();

        return new CompoundSerializableDataType<>(
            serializableData(),
            _serializableData -> mapCodecGetter
                .apply(_serializableData)
                .flatXmap(toWrapped, from),
            _serializableData -> packetCodecGetter
                .apply(_serializableData)
                .xmap(to, fromUnwrapped),
            this.getName(),
            this.isRoot()
        );

    }

    @Override
    public <S> CompoundSerializableDataType<S> flatXmap(Function<? super T, ? extends DataResult<? extends S>> to, Function<? super S, ? extends DataResult<? extends T>> from) {

        Function<? super T, ? extends S> toUnwrapped = t -> to.apply(t).getOrThrow();
        Function<? super S, ? extends T> fromUnwrapped = s -> from.apply(s).getOrThrow();

        return new CompoundSerializableDataType<>(
            serializableData(),
            _serializableData -> mapCodecGetter
                .apply(_serializableData)
                .flatXmap(to, from),
            _serializableData -> packetCodecGetter
                .apply(_serializableData)
                .xmap(toUnwrapped, fromUnwrapped),
            this.getName(),
            this.isRoot()
        );

    }

    @Override
    public CompoundSerializableDataType<T> setRoot(boolean root) {
        return new CompoundSerializableDataType<>(serializableData().setRoot(root), this.mapCodecGetter, this.packetCodecGetter, this.getName(), root);
    }

    public SerializableData serializableData() {
        return serializableData;
    }

    public MapCodec<T> mapCodec() {
        return mapCodecGetter.apply(serializableData());
    }

}
