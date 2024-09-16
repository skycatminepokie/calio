package io.github.apace100.calio.codec;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import io.github.apace100.calio.data.SerializableData;

/**
 * 	A subtype of {@link MapCodec<A> MapCodec&lt;A&gt;} with additional methods for decoding/encoding the element from/to
 * 	a {@link SerializableData.Instance}; solely to be used in/with {@link io.github.apace100.calio.data.CompoundSerializableDataType
 * 	CompoundSerializableDataType} and {@link SerializableData}
 */
public abstract class CompoundMapCodec<A> extends MapCodec<A> {

	public abstract <T> A fromData(DynamicOps<T> ops, SerializableData.Instance data);

	public abstract <T> SerializableData.Instance toData(A input, DynamicOps<T> ops, SerializableData serializableData);

}
