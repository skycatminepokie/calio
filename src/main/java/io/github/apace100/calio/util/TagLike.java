package io.github.apace100.calio.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.codec.*;
import io.github.apace100.calio.data.DataException;
import io.github.apace100.calio.data.SerializableDataType;
import io.github.apace100.calio.mixin.RegistryEntryListNamedAccessor;
import io.github.apace100.calio.mixin.TagEntryAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("unused")
public class TagLike<T> {

    private final RegistryKey<? extends Registry<T>> registryRef;

    private final ImmutableSet<T> elements;
    private final ImmutableMap<TagKey<T>, Collection<T>> tags;

    private final ImmutableSet<TagEntry> tagEntries;

    public TagLike(RegistryKey<? extends Registry<T>> registryRef, Collection<T> elements, Map<TagKey<T>, Collection<T>> tags, Collection<TagEntry> tagEntries) {
        this.registryRef = registryRef;
        this.elements = ImmutableSet.copyOf(elements);
        this.tags = ImmutableMap.copyOf(tags);
        this.tagEntries = ImmutableSet.copyOf(tagEntries);
    }

    public ImmutableSet<TagEntry> entries() {
        return tagEntries;
    }

    public boolean contains(@NotNull T element) {
        return elements.contains(element)
            || tags.values().stream().flatMap(Collection::stream).anyMatch(element::equals);
    }

    public void write(RegistryByteBuf buf) {

        DynamicRegistryManager registryManager = buf.getRegistryManager();

        Registry<T> registry = registryManager.get(registryRef);
        buf.writeRegistryKey(registry.getKey());

        List<Identifier> elementIds = new LinkedList<>();
        for (T element : elements) {
            registry.getKey(element)
                .map(RegistryKey::getValue)
                .ifPresent(elementIds::add);
        }

        buf.writeVarInt(elementIds.size());
        elementIds.forEach(buf::writeIdentifier);

        buf.writeVarInt(tags.size());
        for (TagKey<T> tag : tags.keySet()) {
            buf.writeIdentifier(tag.id());
        }

        CalioPacketCodecs
            .collection(HashSet::new, CalioPacketCodecs.TAG_ENTRY)
            .encode(buf, new HashSet<>(tagEntries));

    }

    public static <T> TagLike<T> read(RegistryByteBuf buf) {

        RegistryKey<? extends Registry<T>> registryRef = RegistryKey.ofRegistry(buf.readIdentifier());
        Registry<T> registry = buf.getRegistryManager().get(registryRef);

        Set<T> elements = new ObjectOpenHashSet<>();
        int elementIdsSize = buf.readVarInt();

        for (int i = 0; i < elementIdsSize; i++) {
            registry
                .getOrEmpty(buf.readIdentifier())
                .ifPresent(elements::add);
        }

        Map<TagKey<T>, Collection<T>> tags = new Object2ObjectOpenHashMap<>();
        int tagsSize = buf.readVarInt();

        for (int i = 0; i < tagsSize; i++) {

            TagKey<T> tag = TagKey.of(registryRef, buf.readIdentifier());
            registry
                .streamEntries()
                .filter(reference -> reference.isIn(tag))
                .map(RegistryEntry.Reference::value)
                .forEach(element -> tags
                    .computeIfAbsent(tag, _tag -> new ObjectOpenHashSet<>())
                    .add(element));

        }

        Set<TagEntry> tagEntries = CalioPacketCodecs
            .collection(HashSet::new, CalioPacketCodecs.TAG_ENTRY)
            .decode(buf);

        return new TagLike<>(registryRef, elements, tags, tagEntries);

    }

    public static <T> Builder<T> builder(RegistryKey<? extends Registry<T>> registryKey) {
        return new Builder<>(registryKey);
    }

    public static class Builder<E> {

        private final RegistryKey<? extends Registry<E>> registryRef;
        private final Set<TagEntry> tagEntries;

        public Builder(RegistryKey<? extends Registry<E>> registryRef) {
            this.registryRef = registryRef;
            this.tagEntries = new ObjectOpenHashSet<>();
        }

        public Builder(TagLike<E> tagLike) {
            this.registryRef = tagLike.registryRef;
            this.tagEntries = new ObjectOpenHashSet<>(tagLike.tagEntries);
        }

        public Builder(RegistryKey<? extends Registry<E>> registryRef, Collection<TagEntry> tagEntries) {
            this.registryRef = registryRef;
            this.tagEntries = new ObjectOpenHashSet<>(tagEntries);
        }

        public Builder<E> add(TagEntry tagEntry) {
            this.tagEntries.add(tagEntry);
            return this;
        }

        public Builder<E> addAll(Collection<TagEntry> tagEntries) {
            this.tagEntries.addAll(tagEntries);
            return this;
        }

        public Builder<E> addAll(Builder<E> other) {
            return this.addAll(other.tagEntries);
        }

        public Builder<E> clear() {
            this.tagEntries.clear();
            return this;
        }

        @SuppressWarnings("unchecked")
        public TagLike<E> build(@NotNull RegistryEntryLookup<E> entryLookup) {

            Map<TagKey<E>, Collection<E>> elementsByTag = new HashMap<>();
            Set<TagEntry> tagEntriesToAdd = new HashSet<>();

            Set<E> elements = new HashSet<>();
            for (TagEntry tagEntry : tagEntries) {

                TagEntryAccessor entryAccessor = (TagEntryAccessor) tagEntry;
                boolean required = entryAccessor.isRequired();

                Identifier id = entryAccessor.getId();
                String path = "[" + tagEntriesToAdd.size() + "]";

                if (entryAccessor.isTag()) {

                    TagKey<E> tag = TagKey.of(registryRef, id);
                    List<E> tagValues = Calio.getRegistryEntries(tag)
                        .map(entries -> entries.map(RegistryEntry::value).toList())
                        .orElseGet(() -> entryLookup.getOptional(tag)
                            .map(named -> ((RegistryEntryListNamedAccessor<E>) named).callGetEntries())
                            .map(entries -> entries.stream().map(RegistryEntry::value).toList())
                            .orElse(null));

                    if (required && tagValues == null) {
                        throw new DataException(DataException.Phase.READING, path, "Tag \"" + id + "\" for registry \"" + registryRef.getValue() + "\" doesn't exist");
                    }

                    else if (tagValues != null) {
                        elementsByTag.computeIfAbsent(tag, k -> new ObjectOpenHashSet<>()).addAll(tagValues);
                    }

                }

                else {

                    RegistryKey<E> registryKey = RegistryKey.of(registryRef, id);
                    E element = Calio.getRegistry(registryRef)
                        .flatMap(registry -> registry.getOrEmpty(registryKey))
                        .orElseGet(() -> entryLookup.getOptional(registryKey)
                            .map(RegistryEntry.Reference::value)
                            .orElse(null));

                    if (required && element == null) {
                        throw new DataException(DataException.Phase.READING, path, "Type \"" + id + "\" is not registered in registry \"" + registryRef.getValue() + "\"");
                    }

                    else if (element != null) {
                        elements.add(element);
                    }

                }

                tagEntriesToAdd.add(tagEntry);

            }

            return new TagLike<>(registryRef, elements, elementsByTag, tagEntriesToAdd);

        }

    }

    public static <E> SerializableDataType<TagLike<E>> dataType(RegistryKey<E> registryKey) {
        RegistryKey<? extends Registry<E>> registryRef = registryKey.getRegistryRef();
        return SerializableDataType.of(
            new StrictCodec<>() {

                @Override
                public <T> T strictEncode(TagLike<E> input, DynamicOps<T> ops, T prefix) {
                    return StrictListCodec
                        .of(CalioCodecs.TAG_ENTRY)
                        .strictEncode(new LinkedList<>(input.tagEntries), ops, prefix);
                }

                @Override
                public <T> Pair<TagLike<E>, T> strictDecode(DynamicOps<T> ops, T input) {

                    RegistryEntryLookup<E> entryLookup = Calio
                        .getRegistryEntryLookup(ops, registryRef)
                        .orElseThrow(() -> new IllegalStateException("Couldn't decode tag-like without access to registries!"));

                    var tagEntries = StrictListCodec.of(CalioCodecs.TAG_ENTRY)
                        .xmap(ObjectOpenHashSet::new, LinkedList::new)
                        .strictParse(ops, input);

                    return Pair.of(new Builder<>(registryRef, tagEntries).build(entryLookup), input);

                }

            },
            PacketCodec.of(
                TagLike::write,
                TagLike::read
            )
        );
    }

    public static <E> SerializableDataType<TagLike<E>> dataType(Registry<E> registry) {
        RegistryKey<? extends Registry<E>> registryRef = registry.getKey();
        return SerializableDataType.of(
            new StrictCodec<>() {

                @Override
                public <T> T strictEncode(TagLike<E> input, DynamicOps<T> ops, T prefix) {
                    return StrictListCodec
                        .of(CalioCodecs.TAG_ENTRY)
                        .strictEncode(new LinkedList<>(input.tagEntries), ops, prefix);
                }

                @Override
                public <T> Pair<TagLike<E>, T> strictDecode(DynamicOps<T> ops, T input) {

                    List<TagEntry> tagEntries = StrictListCodec
                        .of(CalioCodecs.TAG_ENTRY)
                        .strictParse(ops, input);

                    return Pair.of(new Builder<>(registryRef, tagEntries).build(registry.getReadOnlyWrapper()), input);

                }

            },
            PacketCodec.of(
                TagLike::write,
                TagLike::read
            )
        );
    }

}
