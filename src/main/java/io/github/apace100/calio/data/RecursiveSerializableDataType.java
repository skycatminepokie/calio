package io.github.apace100.calio.data;

import com.google.common.base.Suppliers;
import io.github.apace100.calio.codec.StrictCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.function.Function;
import java.util.function.Supplier;

public class RecursiveSerializableDataType<T> extends SerializableDataType<T> {

    private final Supplier<SerializableDataType<T>> dataType;

    public RecursiveSerializableDataType(Function<SerializableDataType<T>, SerializableDataType<T>> wrapped) {
        super(null, null);
        this.dataType = Suppliers.memoize(() -> wrapped.apply(this));
    }

    @Override
    protected StrictCodec<T> baseCodec() {
        return this.dataType.get().baseCodec();
    }

    @Override
    public PacketCodec<RegistryByteBuf, T> packetCodec() {
        return dataType.get().packetCodec();
    }

    @SuppressWarnings("unchecked")
    public <DT extends SerializableDataType<T>> DT get() {
        return (DT) dataType.get();
    }

}
