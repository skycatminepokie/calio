package io.github.apace100.calio.data;

import com.mojang.serialization.*;
import io.github.apace100.calio.codec.CompoundMapCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class CompoundSerializableDataType<T> extends SerializableDataType<T> {

    private final SerializableData serializableData;

    private final Function<SerializableData, CompoundMapCodec<T>> compoundCodecGetter;
    private final BiFunction<SerializableData, CompoundMapCodec<T>, PacketCodec<RegistryByteBuf, T>> packetCodecGetter;

    public CompoundSerializableDataType(SerializableData serializableData, Function<SerializableData, CompoundMapCodec<T>> compoundCodecGetter, BiFunction<SerializableData, CompoundMapCodec<T>, PacketCodec<RegistryByteBuf, T>> packetCodecGetter) {
        super(null, null);
        this.serializableData = serializableData;
        this.compoundCodecGetter = compoundCodecGetter;
        this.packetCodecGetter = packetCodecGetter;
    }

    @Override
    public Codec<T> codec() {
        return mapCodec().codec();
    }

    @Override
    public PacketCodec<RegistryByteBuf, T> packetCodec() {
        return packetCodecGetter.apply(serializableData(), mapCodec());
    }

    public CompoundSerializableDataType<T> setRoot(boolean root) {
        return new CompoundSerializableDataType<>(serializableData().setRoot(root), this.compoundCodecGetter, this.packetCodecGetter);
    }

    @Override
    public SerializableData.Field<T> field(String name) {
        return new SerializableData.FieldImpl<>(name, setRoot(false));
    }

    @Override
    public SerializableData.Field<T> field(String name, Supplier<T> defaultSupplier) {
        return new SerializableData.OptionalFieldImpl<>(name, setRoot(false), defaultSupplier);
    }

    @Override
    public SerializableData.Field<T> functionedField(String name, Function<SerializableData.Instance, T> defaultFunction) {
        return new SerializableData.FunctionedFieldImpl<>(name, setRoot(false), defaultFunction);
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
