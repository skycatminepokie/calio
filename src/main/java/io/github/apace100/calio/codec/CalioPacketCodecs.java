package io.github.apace100.calio.codec;

import com.google.gson.internal.LazilyParsedNumber;
import io.github.apace100.calio.data.DataException;
import io.github.apace100.calio.mixin.TagEntryAccessor;
import io.github.apace100.calio.util.DynamicIdentifier;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.tag.TagEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;

public class CalioPacketCodecs {

    public static final PacketCodec<PacketByteBuf, Number> NUMBER = PacketCodec.of(
        (number, buf) -> {
            switch (number) {
                case null ->
                    buf.writeByte(-1);
                case Double d -> {
                    buf.writeByte(0);
                    buf.writeDouble(d);
                }
                case Float f -> {
                    buf.writeByte(1);
                    buf.writeFloat(f);
                }
                case Integer i -> {
                    buf.writeByte(2);
                    buf.writeInt(i);
                }
                case Long l -> {
                    buf.writeByte(3);
                    buf.writeLong(l);
                }
                default -> {
                    buf.writeByte(4);
                    buf.writeString(number.toString());
                }
            }
        },
        buf -> {

            byte type = buf.readByte();
            return switch (type) {
                case 0 ->
                    buf.readDouble();
                case 1 ->
                    buf.readFloat();
                case 2 ->
                    buf.readInt();
                case 3 ->
                    buf.readLong();
                case 4 ->
                    new LazilyParsedNumber(buf.readString());
                default ->
                    throw new IllegalStateException("Unexpected type ID \"" + type + "\" (allowed range: [0-4]");
            };

        }
    );

    public static final PacketCodec<RegistryByteBuf, RecipeEntry<?>> RECIPE_ENTRY = PacketCodec.tuple(
        Identifier.PACKET_CODEC, RecipeEntry::id,
        Recipe.PACKET_CODEC, RecipeEntry::value,
        RecipeEntry::new
    );

    public static final PacketCodec<ByteBuf, Codecs.TagEntryId> TAG_ENTRY_ID = PacketCodecs.STRING.xmap(
        str -> {

            boolean isTag = str.startsWith("#");
            String idStr = isTag
                ? str.substring(1)
                : str;

            Identifier id = DynamicIdentifier.of(idStr);
            return new Codecs.TagEntryId(id, isTag);

        },
        Codecs.TagEntryId::toString
    );

    public static final PacketCodec<ByteBuf, TagEntry> TAG_ENTRY = PacketCodec.tuple(
        TAG_ENTRY_ID, tagEntry -> ((TagEntryAccessor) tagEntry).callGetIdForCodec(),
        PacketCodecs.BOOL, tagEntry -> ((TagEntryAccessor) tagEntry).isRequired(),
        TagEntry::new
    );

    public static final PacketCodec<ByteBuf, Set<TagEntry>> TAG_ENTRY_SET = collection(HashSet::new, CalioPacketCodecs.TAG_ENTRY);

    public static <B extends ByteBuf, E, C extends Collection<E>> PacketCodec<B, C> collection(IntFunction<C> collectionFactory, PacketCodec<? super B, E> elementCodec) {
        return collection(collectionFactory, elementCodec, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, E, C extends Collection<E>> PacketCodec<B, C> collection(IntFunction<C> collectionFactory, PacketCodec<? super B, E> elementCodec, int maxSize) {
        return PacketCodec.ofStatic(
            (buf, elements) -> {

                PacketCodecs.writeCollectionSize(buf, elements.size(), maxSize);
                int index = 0;

                for (E element : elements) {

                    try {
                        elementCodec.encode(buf, element);
                        index++;
                    }

                    catch (DataException de) {
                        throw de.prependArray(index);
                    }

                    catch (Exception e) {
                        throw new DataException(DataException.Phase.SENDING, DataException.Type.ARRAY, "[" + index + "]", e.getMessage());
                    }

                }

            },
            buf -> {

                int size = PacketCodecs.readCollectionSize(buf, maxSize);
                C elements = collectionFactory.apply(Math.min(size, 65536));

                for (int index = 0; index < size; index++) {

                    try {
                        elements.add(elementCodec.decode(buf));
                    }

                    catch (DataException de) {
                        throw de.prependArray(index);
                    }

                    catch (Exception e) {
                        throw new DataException(DataException.Phase.RECEIVING, DataException.Type.ARRAY, "[" + index + "]", e.getMessage());
                    }

                }

                return elements;

            }
        );
    }

}
