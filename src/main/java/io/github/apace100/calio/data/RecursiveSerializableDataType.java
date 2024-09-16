package io.github.apace100.calio.data;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.function.Function;
import java.util.function.Supplier;

public class RecursiveSerializableDataType<T> extends SerializableDataType<T> {

    private final Supplier<SerializableDataType<T>> dataType;
    private final Function<SerializableDataType<T>, SerializableDataType<T>> wrapper;

    public RecursiveSerializableDataType(Function<SerializableDataType<T>, SerializableDataType<T>> wrapper) {
        super(null, null);
        this.dataType = Suppliers.memoize(() -> wrapper.apply(this));
        this.wrapper = wrapper;
    }

    @Override
    public Codec<T> codec() {
        return this.dataType.get().codec();
    }

    @Override
    public PacketCodec<RegistryByteBuf, T> packetCodec() {
        return dataType.get().packetCodec();
    }

    @Override
    public RecursiveSerializableDataType<T> setRoot(boolean root) {
        return new RecursiveSerializableDataType<>(dt -> wrapper.apply(dt).setRoot(root));
    }

    @Override
    public SerializableData.Field<T> field(String name) {
        return dataType.get().field(name);
    }

    @Override
    public SerializableData.Field<T> field(String name, Supplier<T> defaultSupplier) {
        return dataType.get().field(name, defaultSupplier);
    }

    @Override
    public SerializableData.Field<T> functionedField(String name, Function<SerializableData.Instance, T> defaultFunction) {
        return dataType.get().functionedField(name, defaultFunction);
    }

    @SuppressWarnings("unchecked")
    public <DT extends SerializableDataType<T>> DT get() {
        return (DT) dataType.get();
    }

}
