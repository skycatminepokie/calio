package io.github.apace100.calio.data;

import com.mojang.serialization.*;
import io.github.apace100.calio.serialization.StrictMapCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public class CompoundSerializableDataType<T> extends SerializableDataType<T> {

    private final MapCodec<T> mapCodec;

    private final BiFunction<SerializableData.Instance, DynamicOps<?>, T> fromData;
    private final BiConsumer<T, SerializableData.Instance> toData;

    public CompoundSerializableDataType(MapCodec<T> mapCodec, BiFunction<SerializableData.Instance, DynamicOps<?>, T> fromData, BiConsumer<T, SerializableData.Instance> toData, PacketCodec<RegistryByteBuf, T> packetCodec) {
        super(mapCodec.codec(), packetCodec);
        this.mapCodec = mapCodec;
        this.fromData = fromData;
        this.toData = toData;
    }

    public SerializableData serializableData() {
        return this.mapCodec().serializableData();
    }

    public MapCodec<T> mapCodec() {
        return mapCodec;
    }

    public SerializableData.Instance toData(T value) {
        return this.writeTo(value, this.serializableData().instance());
    }

    public SerializableData.Instance writeTo(T value, SerializableData.Instance data) {
        toData.accept(value, data);
        return data;
    }

    public T fromData(SerializableData.Instance data, DynamicOps<?> ops) {
        return fromData.apply(data, ops);
    }

    public static <T> CompoundSerializableDataType<T> of(SerializableData serializableData, Function<SerializableData.Instance, T> fromData, BiConsumer<T, SerializableData.Instance> toData, PacketCodec<RegistryByteBuf, T> packetCodec) {
        BiFunction<SerializableData.Instance, DynamicOps<?>, T> newfromData = (data, ops) -> fromData.apply(data);
        return new CompoundSerializableDataType<>(new MapCodec<>(serializableData, newfromData, toData), newfromData, toData, packetCodec);
    }

    public static <T> CompoundSerializableDataType<T> of(SerializableData serializableData, Function<SerializableData.Instance, T> fromData, BiConsumer<T, SerializableData.Instance> toData) {
        BiFunction<SerializableData.Instance, DynamicOps<?>, T> newfromData = (data, ops) -> fromData.apply(data);
        return new CompoundSerializableDataType<>(
            new MapCodec<>(serializableData, newfromData, toData),
            newfromData, toData,
            new PacketCodec<>() {

                @Override
                public T decode(RegistryByteBuf buf) {
                    return fromData.apply(serializableData.receive(buf));
                }

                @Override
                public void encode(RegistryByteBuf buf, T value) {

                    SerializableData.Instance data = serializableData.instance();
                    toData.accept(value, data);

                    serializableData.send(buf, data);

                }

            }
        );

    }

    public static <T> CompoundSerializableDataType<T> of(SerializableData serializableData, BiFunction<SerializableData.Instance, DynamicOps<?>, T> fromData, BiConsumer<T, SerializableData.Instance> toData) {
        return new CompoundSerializableDataType<>(
            new MapCodec<>(serializableData, fromData, toData),
            fromData, toData,
            new PacketCodec<>() {

                @Override
                public T decode(RegistryByteBuf buf) {
                    return fromData.apply(serializableData.receive(buf), buf.getRegistryManager().getOps(JavaOps.INSTANCE));
                }

                @Override
                public void encode(RegistryByteBuf buf, T value) {

                    SerializableData.Instance data = serializableData.instance();
                    toData.accept(value, data);

                    serializableData.send(buf, data);

                }

            }
        );

    }

    public static class MapCodec<T> extends StrictMapCodec<T> {

        private final SerializableData serializableData;

        private final BiFunction<SerializableData.Instance, DynamicOps<?>, T> fromData;
        private final BiConsumer<T, SerializableData.Instance> toData;

        public MapCodec(SerializableData serializableData, BiFunction<SerializableData.Instance, DynamicOps<?>, T> fromData, BiConsumer<T, SerializableData.Instance> toData) {
            this.serializableData = serializableData;
            this.fromData = fromData;
            this.toData = toData;
        }

        @Override
        public <I> Stream<I> keys(DynamicOps<I> ops) {
            return serializableData.keys(ops);
        }

        @Override
        public <I> T strictDecode(DynamicOps<I> ops, MapLike<I> input) {
            return fromData.apply(serializableData.strictDecode(ops, input), ops);
        }

        @Override
        public <I> RecordBuilder<I> encode(T input, DynamicOps<I> ops, RecordBuilder<I> prefix) {

            SerializableData.Instance data = serializableData.instance();
            toData.accept(input, data);

            return serializableData.encode(data, ops, prefix);

        }

        public SerializableData serializableData() {
            return serializableData;
        }

    }

}
