package io.github.apace100.calio;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.github.apace100.calio.codec.CalioCodecs;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.calio.mixin.CachedRegistryInfoGetterAccessor;
import io.github.apace100.calio.mixin.RegistryOpsAccessor;
import io.github.apace100.calio.network.packet.s2c.SyncDataObjectRegistryS2CPacket;
import io.github.apace100.calio.registry.DataObjectRegistry;
import io.github.apace100.calio.util.CalioResourceConditions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.DataPackContents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.stream.Stream;

public class Calio implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger(Calio.class);
	public static final String MOD_NAMESPACE = "calio";

	@ApiStatus.Internal
	public static final Map<Unit, Set<String>> LOADED_NAMESPACES = new WeakHashMap<>();
	@ApiStatus.Internal
	public static final Map<Unit, DataPackContents> DATA_PACK_CONTENTS = new WeakHashMap<>();
	@ApiStatus.Internal
	public static final Map<Unit, DynamicRegistryManager.Immutable> DYNAMIC_REGISTRIES = new WeakHashMap<>();

	@ApiStatus.Internal
	public static final ThreadLocal<Map<TagKey<?>, Collection<RegistryEntry<?>>>> REGISTRY_TAGS = new ThreadLocal<>();

    public static Identifier identifier(String path) {
		return Identifier.of(MOD_NAMESPACE, path);
	}

    @Override
	public void onInitialize() {

		CalioCodecs.init();
		SerializableDataTypes.init();

        Criteria.register(CodeTriggerCriterion.ID.toString(), CodeTriggerCriterion.INSTANCE);
		CalioResourceConditions.register();

		PayloadTypeRegistry.playS2C().register(SyncDataObjectRegistryS2CPacket.PACKET_ID, SyncDataObjectRegistryS2CPacket.PACKET_CODEC);
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> DataObjectRegistry.performAutoSync(player));

	}

	public static <T> boolean areTagsEqual(RegistryKey<? extends Registry<T>> registryKey, TagKey<T> tag1, TagKey<T> tag2) {
		return areTagsEqual(tag1, tag2);
	}

	public static <T> boolean areTagsEqual(TagKey<T> tag1, TagKey<T> tag2) {
		return tag1 == tag2
			|| tag1 != null
			&& tag2 != null
			&& tag1.registry().equals(tag2.registry())
			&& tag1.id().equals(tag2.id());
	}

	public static <T> DynamicOps<T> wrapRegistryOps(DynamicOps<T> ops) {

		if (ops instanceof RegistryOps<T> registryOps) {
			return registryOps;
		}

		else {
			return getDynamicRegistries()
				.map(drm -> (DynamicOps<T>) drm.getOps(ops))
				.orElse(ops);
		}

	}

	public static <F, T> DynamicOps<T> wrapRegistryOps(DynamicOps<F> fromOps, DynamicOps<T> toOps) {
		return getWrapperLookup(fromOps)
			.map(lookup -> (DynamicOps<T>) lookup.getOps(toOps))
			.orElse(toOps);
	}

	public static <T> Optional<RegistryOps<T>> getRegistryOps(DynamicOps<T> ops) {

		if (ops instanceof RegistryOps<T> regOps) {
			return Optional.of(regOps);
		}

		else {
			return getDynamicRegistries().map(drm -> drm.getOps(ops));
		}

	}

	public static <T> Optional<RegistryWrapper.WrapperLookup> getWrapperLookup(DynamicOps<T> ops) {
		return getDynamicRegistries()
			.map(RegistryWrapper.WrapperLookup.class::cast)
			.or(() -> {

				RegistryOps.RegistryInfoGetter infoGetter = getRegistryOps(ops)
					.map(registryOps -> ((RegistryOpsAccessor) registryOps).getRegistryInfoGetter())
					.orElse(null);

				return infoGetter instanceof RegistryOps.CachedRegistryInfoGetter cachedInfoGetter
					? Optional.of(((CachedRegistryInfoGetterAccessor) (Object) cachedInfoGetter).getRegistriesLookup())
					: Optional.empty();

			});
	}

	public static <R, I> Optional<RegistryEntryLookup<R>> getRegistryEntryLookup(DynamicOps<I> ops, RegistryKey<? extends Registry<R>> registryRef) {
		return getRegistryEntryLookup(registryRef)
			.or(() -> getRegistryOps(ops)
				.flatMap(registryOps -> registryOps.getEntryLookup(registryRef)));
	}

	public static <R> Optional<RegistryEntryLookup<R>> getRegistryEntryLookup(RegistryKey<? extends Registry<R>> registryRef) {
		return Optional.ofNullable(DYNAMIC_REGISTRIES.get(Unit.INSTANCE)).flatMap(drm -> drm.getOptionalWrapper(registryRef));
	}

	public static <R, I> Optional<RegistryEntry.Reference<R>> getOptionalEntry(DynamicOps<I> ops, RegistryKey<R> registryKey) {
		return getRegistryEntryLookup(ops, registryKey.getRegistryRef())
			.flatMap(entryLookup -> entryLookup.getOptional(registryKey));
	}

	public static <R, I> Optional<RegistryEntryList.Named<R>> getOptionalEntries(DynamicOps<I> ops, TagKey<R> tagKey) {
		return getRegistryEntryLookup(ops, tagKey.registry())
			.flatMap(entryLookup -> entryLookup.getOptional(tagKey));
	}

	@SuppressWarnings("unchecked")
	public static <T> Optional<Stream<RegistryEntry<T>>> getRegistryEntries(TagKey<T> tag) {
		return getRegistryTags()
			.filter(tags -> tags.containsKey(tag))
			.map(tags -> tags.get(tag)
				.stream()
				.map(entry -> (RegistryEntry<T>) entry));
	}

	public static <T> Optional<Registry<T>> getRegistry(RegistryKey<? extends Registry<T>> registryRef) {
		return getDynamicRegistries().flatMap(drm -> drm.getOptional(registryRef));
	}

	public static Optional<DynamicRegistryManager.Immutable> getDynamicRegistries() {
		return Optional.ofNullable(DYNAMIC_REGISTRIES.get(Unit.INSTANCE));
	}

	public static Optional<Map<TagKey<?>, Collection<RegistryEntry<?>>>> getRegistryTags() {
		return Optional.ofNullable(REGISTRY_TAGS.get());
	}

	public static <R> DataResult<R> createMissingRequiredFieldError(String name) {
		return DataResult.error(() -> "Required field \"" + name + "\" is missing!");
	}

}
