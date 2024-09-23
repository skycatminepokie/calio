package io.github.apace100.calio.codec;

import com.mojang.serialization.*;
import io.github.apace100.calio.data.SerializableData;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 	A subtype of {@link MapCodec<A> MapCodec&lt;A&gt;} with additional methods for decoding/encoding the element from/to
 * 	a {@link SerializableData.Instance}; solely to be used in/with {@link io.github.apace100.calio.data.CompoundSerializableDataType
 * 	CompoundSerializableDataType} and {@link SerializableData}
 */
public abstract class CompoundMapCodec<A> extends MapCodec<A> {

	public abstract <T> A fromData(DynamicOps<T> ops, SerializableData.Instance data);

	public abstract <T> SerializableData.Instance toData(A input, DynamicOps<T> ops, SerializableData serializableData);

	@Override
	public <S> CompoundMapCodec<S> xmap(Function<? super A, ? extends S> to, Function<? super S, ? extends A> from) {
		CompoundMapCodec<A> self = CompoundMapCodec.this;
		return new CompoundMapCodec<>() {

			@Override
			public <T> S fromData(DynamicOps<T> ops, SerializableData.Instance data) {
				return to.apply(self.fromData(ops, data));
			}

			@Override
			public <T> SerializableData.Instance toData(S input, DynamicOps<T> ops, SerializableData serializableData) {
				return self.toData(from.apply(input), ops, serializableData);
			}

			@Override
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return self.keys(ops);
			}

			@Override
			public <T> DataResult<S> decode(DynamicOps<T> ops, MapLike<T> input) {
				return self.decode(ops, input).map(to);
			}

			@Override
			public <T> RecordBuilder<T> encode(S input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
				return self.encode(from.apply(input), ops, prefix);
			}

		};
	}

	@Override
	public <S> CompoundMapCodec<S> flatXmap(Function<? super A, ? extends DataResult<? extends S>> to, Function<? super S, ? extends DataResult<? extends A>> from) {
		CompoundMapCodec<A> self = CompoundMapCodec.this;
		return new CompoundMapCodec<>() {

			@Override
			public <T> S fromData(DynamicOps<T> ops, SerializableData.Instance data) {
				return to.apply(self.fromData(ops, data)).getOrThrow();
			}

			@Override
			public <T> SerializableData.Instance toData(S input, DynamicOps<T> ops, SerializableData serializableData) {
				return self.toData(from.apply(input).getOrThrow(), ops, serializableData);
			}

			@Override
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return self.keys(ops);
			}

			@Override
			public <T> DataResult<S> decode(DynamicOps<T> ops, MapLike<T> input) {
				return self.decode(ops, input).flatMap(a -> to.apply(a).map(s -> (S) s));
			}

			@Override
			public <T> RecordBuilder<T> encode(S input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
				return from.apply(input).mapOrElse(a -> self.encode(a, ops, prefix), prefix::withErrorsFrom);
			}

		};
	}

}
