package io.github.apace100.calio.data;

import com.mojang.serialization.*;
import io.github.apace100.calio.codec.CompoundMapCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CompoundSerializableDataType<T> extends SerializableDataType<T> {

    private final SerializableData serializableData;

    private final Function<SerializableData, CompoundMapCodec<T>> compoundCodecGetter;
    private final BiFunction<SerializableData, CompoundMapCodec<T>, PacketCodec<RegistryByteBuf, T>> packetCodecGetter;

    public CompoundSerializableDataType(SerializableData serializableData, Function<SerializableData, CompoundMapCodec<T>> compoundCodecGetter, BiFunction<SerializableData, CompoundMapCodec<T>, PacketCodec<RegistryByteBuf, T>> packetCodecGetter, Optional<String> name, boolean root) {
        super(null, null, name, root);
        this.serializableData = serializableData;
        this.compoundCodecGetter = compoundCodecGetter;
        this.packetCodecGetter = packetCodecGetter;
    }

    public CompoundSerializableDataType(SerializableData serializableData, Function<SerializableData, CompoundMapCodec<T>> compoundCodecGetter, BiFunction<SerializableData, CompoundMapCodec<T>, PacketCodec<RegistryByteBuf, T>> packetCodecGetter) {
        this(serializableData, compoundCodecGetter, packetCodecGetter, Optional.empty(), true);
    }

    @Override
    public Codec<T> codec() {
        return mapCodec().codec();
    }

    @Override
    public PacketCodec<RegistryByteBuf, T> packetCodec() {
        return packetCodecGetter.apply(serializableData(), mapCodec());
    }

    @Override
    public <S> CompoundSerializableDataType<S> xmap(Function<? super T, ? extends S> to, Function<? super S, ? extends T> from) {
        return new CompoundSerializableDataType<>(
            serializableData(),
            serializableData1 -> compoundCodecGetter
                .apply(serializableData)
                .xmap(to, from),
            (serializableData1, compoundMapCodec) -> packetCodecGetter
                .apply(serializableData1, compoundMapCodec.xmap(from, to))
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
            serializableData1 -> compoundCodecGetter
                .apply(serializableData1)
                .flatXmap(to, fromWrapped),
            (serializableData1, compoundMapCodec) -> packetCodecGetter
                .apply(serializableData1, compoundMapCodec.flatXmap(fromWrapped, to))
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
            serializableData1 -> compoundCodecGetter
                .apply(serializableData1)
                .flatXmap(toWrapped, from),
            (serializableData1, compoundMapCodec) -> packetCodecGetter
                .apply(serializableData1, compoundMapCodec.flatXmap(from, toWrapped))
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
            serializableData1 -> compoundCodecGetter
                .apply(serializableData1)
                .flatXmap(to, from),
            (serializableData1, compoundMapCodec) -> packetCodecGetter
                .apply(serializableData1, compoundMapCodec.flatXmap(from, to))
                .xmap(toUnwrapped, fromUnwrapped),
            this.getName(),
            this.isRoot()
        );

    }

    @Override
    public CompoundSerializableDataType<T> setRoot(boolean root) {
        return new CompoundSerializableDataType<>(serializableData().setRoot(root), this.compoundCodecGetter, this.packetCodecGetter, this.getName(), root);
    }

    public SerializableData serializableData() {
        return serializableData;
    }

    public CompoundMapCodec<T> mapCodec() {
        return compoundCodecGetter.apply(serializableData());
    }

    public T fromData(DynamicOps<?> ops, SerializableData.Instance data) {
        return mapCodec().fromData(ops, data);
    }

    public SerializableData.Instance toData(T value, DynamicOps<?> ops) {
        return toData(value, ops, serializableData());
    }

    public SerializableData.Instance toData(T value, DynamicOps<?> ops, SerializableData serializableData) {
        return mapCodec().toData(value, ops, serializableData);
    }

}
