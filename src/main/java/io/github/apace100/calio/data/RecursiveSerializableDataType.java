package io.github.apace100.calio.data;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class RecursiveSerializableDataType<T> extends SerializableDataType<T> {

    private final Supplier<SerializableDataType<T>> dataType;
    private final Function<SerializableDataType<T>, SerializableDataType<T>> wrapper;

    public RecursiveSerializableDataType(Function<SerializableDataType<T>, SerializableDataType<T>> wrapper, Optional<String> name, boolean root) {
        super(null, null, name, root);
        this.dataType = Suppliers.memoize(() -> wrapper.apply(this));
        this.wrapper = wrapper;
    }

    public RecursiveSerializableDataType(Function<SerializableDataType<T>, SerializableDataType<T>> wrapper) {
        this(wrapper, Optional.empty(), true);
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
        return new RecursiveSerializableDataType<>(dt -> wrapper.apply(dt).setRoot(root), this.getName(), root);
    }

    @SuppressWarnings("unchecked")
    public <DT extends SerializableDataType<T>> DT cast() {
        return (DT) dataType.get();
    }

}
