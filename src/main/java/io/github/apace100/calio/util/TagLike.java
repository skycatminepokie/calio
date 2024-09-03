package io.github.apace100.calio.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.apace100.calio.codec.*;
import io.github.apace100.calio.data.DataException;
import io.github.apace100.calio.mixin.RegistryEntryListBackedAccessor;
import io.github.apace100.calio.mixin.TagEntryAccessor;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("unused")
public class TagLike<T> {

    private final RegistryKey<? extends Registry<T>> registryRef;
    private final ImmutableSet<TagEntry> tagEntries;

    private final ImmutableMap<RegistryKey<T>, T> elementsByKey;
    private final ImmutableMap<TagKey<T>, Collection<T>> elementsByTag;

    protected TagLike(RegistryKey<? extends Registry<T>> registryRef, Map<RegistryKey<T>, T> elementsByKey, Map<TagKey<T>, Collection<T>> elementsByTag, Collection<TagEntry> tagEntries) {
        this.registryRef = registryRef;
        this.elementsByKey = ImmutableMap.copyOf(elementsByKey);
        this.elementsByTag = ImmutableMap.copyOf(elementsByTag);
        this.tagEntries = ImmutableSet.copyOf(tagEntries);
    }

    public ImmutableSet<TagEntry> entries() {
        return tagEntries;
    }

    public boolean contains(@NotNull T element) {
        return elementsByKey.containsValue(element)
            || elementsByTag.values().stream().flatMap(Collection::stream).anyMatch(element::equals);
    }

    public void write(RegistryByteBuf buf) {

        buf.writeRegistryKey(registryRef);
        CalioPacketCodecs.TAG_ENTRY_SET.encode(buf, entries());

        buf.writeBoolean(SerializableRegistries.SYNCED_REGISTRIES.contains(registryRef));

    }

    public static <T> TagLike<T> read(RegistryByteBuf buf) {

        RegistryKey<? extends Registry<T>> registryRef = buf.readRegistryRefKey();
        Set<TagEntry> tagEntries = CalioPacketCodecs.TAG_ENTRY_SET.decode(buf);

        boolean syncedRegistry = buf.readBoolean();
        return syncedRegistry
            ? builder(registryRef).build(buf.getRegistryManager().getWrapperOrThrow(registryRef))
            : new TagLike<>(registryRef, Map.of(), Map.of(), tagEntries);

    }

    public static <T> Builder<T> builder(RegistryKey<? extends Registry<T>> registryRef) {
        return new Builder<>(registryRef);
    }

    public static <T> Builder<T> builder(RegistryKey<? extends Registry<T>> registryRef, Collection<TagEntry> tagEntries) {
        return new Builder<>(registryRef, tagEntries);
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
            this.tagEntries = new ObjectOpenHashSet<>(tagLike.entries());
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

            Map<RegistryKey<E>, E> elementsByKey = new HashMap<>();
            Map<TagKey<E>, Collection<E>> elementsByTag = new HashMap<>();

            Set<TagEntry> tagEntries = new HashSet<>();
            for (TagEntry tagEntry : this.tagEntries) {

                TagEntryAccessor tagEntryAccess = (TagEntryAccessor) tagEntry;

                Identifier id = tagEntryAccess.getId();
                String path = "[" + tagEntries.size() + "]";

                boolean isTag = tagEntryAccess.isTag();
                boolean required = tagEntryAccess.isRequired();

                if (isTag) {

                    TagKey<E> tag = TagKey.of(registryRef, id);
                    Optional<RegistryEntryList.Named<E>> entries = entryLookup.getOptional(tag);

                    if (required && entries.isEmpty()) {
                        throw new DataException(DataException.Phase.READING, path, "Tag \"" + id + "\" for registry \"" + registryRef.getValue() + "\" doesn't exist!");
                    }

                    else if (entries.isPresent()) {
                        Collection<E> elements = elementsByTag.computeIfAbsent(tag, k -> new ObjectOpenHashSet<>());
                        entries.stream()
                            .map(registryEntries -> ((RegistryEntryListBackedAccessor<E>) registryEntries).callGetEntries())
                            .flatMap(Collection::stream)
                            .map(RegistryEntry::value)
                            .forEach(elements::add);
                    }

                }

                else {

                    RegistryKey<E> key = RegistryKey.of(registryRef, id);
                    Optional<RegistryEntry.Reference<E>> entry = entryLookup.getOptional(key);

                    if (required && entry.isEmpty()) {
                        throw new DataException(DataException.Phase.READING, path, "Type \"" + id + "\" is not registered in registry \"" + key.getRegistry() + "\"!");
                    }

                    else {
                        entry.ifPresent(reference -> elementsByKey.put(key, reference.value()));
                    }

                }

                tagEntries.add(tagEntry);

            }

            return new TagLike<>(registryRef, elementsByKey, elementsByTag, tagEntries);

        }

    }

}
