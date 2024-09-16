package io.github.apace100.calio.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import io.github.apace100.calio.codec.CalioPacketCodecs;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SerializableDataTypeList<E> extends SerializableDataType<List<E>> {

	private final SerializableDataType<E> dataType;

	private final int min;
	private final int max;

	protected SerializableDataTypeList(Codec<List<E>> codec, PacketCodec<RegistryByteBuf, List<E>> packetCodec, SerializableDataType<E> dataType, int min, int max) {
		super(codec, packetCodec);
		this.dataType = dataType;
		this.min = min;
		this.max = max;
	}

	@Override
	public SerializableDataTypeList<E> setRoot(boolean root) {
		return of(dataType.setRoot(root), root, min, max);
	}

	@Override
	public SerializableData.Field<List<E>> field(String name) {
		return new SerializableData.FieldImpl<>(name, setRoot(false));
	}

	@Override
	public SerializableData.Field<List<E>> field(String name, Supplier<List<E>> defaultSupplier) {
		return new SerializableData.OptionalFieldImpl<>(name, setRoot(false), defaultSupplier);
	}

	@Override
	public SerializableData.Field<List<E>> functionedField(String name, Function<SerializableData.Instance, List<E>> defaultFunction) {
		return new SerializableData.FunctionedFieldImpl<>(name, setRoot(false), defaultFunction);
	}

	public static <E> SerializableDataTypeList<E> of(SerializableDataType<E> dataType, int min, int max) {
		return of(dataType, true, min, max);
	}

	private static <E> SerializableDataTypeList<E> of(SerializableDataType<E> dataType, boolean root, int min, int max) {
		return new SerializableDataTypeList<>(
			new Codec<>() {

				@Override
				public <I> DataResult<Pair<List<E>, I>> decode(DynamicOps<I> ops, I input) {
					return ops.getList(input)
						.mapOrElse(
							listInput -> {

								Stream.Builder<I> inputsBuilder = Stream.builder();
								List<E> elements = new ObjectArrayList<>();

								try {

									try {

										listInput.accept(i -> {

											if (elements.size() > max) {
												throw createTooLongError(elements.size(), max);
											}

											E element = dataType.codec()
												.parse(ops, i)
												.getOrThrow();

											elements.add(element);
											inputsBuilder.add(i);

										});

										if (elements.size() < min) {
											throw createTooShortError(elements.size(), min);
										}

										I inputs = ops.createList(inputsBuilder.build());
										List<E> immutableElements = List.copyOf(elements);

										return DataResult.success(Pair.of(immutableElements, inputs));

									}

									catch (DataException de) {
										throw de.prependArray(elements.size());
									}

									catch (Exception e) {
										throw new DataException(DataException.Phase.READING, DataException.Type.ARRAY, "[" + elements.size() + "]", e.getMessage());
									}

								}

								catch (Exception e) {

									if (root) {
										return DataResult.error(e::getMessage);
									}

									else {
										throw e;
									}

								}

							},
							error -> dataType.codec().decode(ops, input)
								.map(elementAndInput -> elementAndInput
									.mapFirst(List::of)));
				}

				@Override
				public <I> DataResult<I> encode(List<E> input, DynamicOps<I> ops, I prefix) {

					ListBuilder<I> listBuilder = ops.listBuilder();
					if (input.size() < min) {
						throw createTooShortError(input.size(), min);
					}

					else if (input.size() > max) {
						throw createTooLongError(input.size(), max);
					}

					else {

						try {

							int index = 0;
							for (E element : input) {

								try {

									listBuilder.add(dataType.codec()
										.encodeStart(ops, element)
										.getOrThrow());

									index++;

								}

								catch (DataException de) {
									throw de.prependArray(index);
								}

								catch (Exception e) {
									throw new DataException(DataException.Phase.WRITING, "[" + index + "]", e.getMessage());
								}

							}

							return listBuilder.build(prefix);

						}

						catch (Exception e) {

							if (root) {
								return DataResult.error(e::getMessage);
							}

							else {
								throw e;
							}

						}

					}

				}

			},
			new PacketCodec<>() {

				private final Supplier<PacketCodec<RegistryByteBuf, List<E>>> packetCodecSupplier = () -> CalioPacketCodecs.collection(ObjectArrayList::new, dataType.packetCodec());

				@Override
				public List<E> decode(RegistryByteBuf buf) {
					return packetCodecSupplier.get().decode(buf);
				}

				@Override
				public void encode(RegistryByteBuf buf, List<E> value) {
					packetCodecSupplier.get().encode(buf, value);
				}

			},
			dataType, min, max);
	}

	private static RuntimeException createTooLongError(int size, int max) {
		return new IllegalStateException("Expected collection to have at most " + max + " element(s), but found " + size + " element(s)!");
	}

	private static RuntimeException createTooShortError(int size, int min) {
		return new IllegalStateException("Expected collection to have at least " + min + " element(s), but only found " + size + " element(s)!");
	}

}
