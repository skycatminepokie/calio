package io.github.apace100.calio;

import com.mojang.serialization.DynamicOps;
import io.github.apace100.calio.codec.CalioCodecs;
import io.github.apace100.calio.data.DataException;
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
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.DataPackContents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.Supplier;
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

	public static <T> RegistryOps<T> getRegistryOps(DynamicOps<T> ops, Supplier<RuntimeException> exception) {

		if (ops instanceof RegistryOps<T> regOps) {
			return regOps;
		}

		else {
			return getDynamicRegistries()
				.map(drm -> drm.getOps(ops))
				.orElseThrow(exception);
		}

	}

    public static <F, T> RegistryOps<T> convertToRegistryOps(DynamicOps<F> sourceOps, DynamicOps<T> ops, Supplier<RuntimeException> exception) {

		RegistryOps<F> registryOps = getRegistryOps(sourceOps, exception);
		if (((RegistryOpsAccessor) registryOps).getRegistryInfoGetter() instanceof RegistryOps.CachedRegistryInfoGetter infoGetter) {
			RegistryWrapper.WrapperLookup registriesLookup = ((CachedRegistryInfoGetterAccessor) (Object) infoGetter).getRegistriesLookup();
			return registriesLookup.getOps(ops);
		}

		else {
			throw exception.get();
		}

	}

	public static <R, I> RegistryEntryLookup<R> getRegistryEntryLookup(DynamicOps<I> ops, RegistryKey<? extends Registry<R>> registryRef, Supplier<RuntimeException> exception) {

		if (ops instanceof RegistryOps<I> registryOps) {
			return registryOps.getEntryLookup(registryRef)
				.or(() -> getRegistryEntryLookup(registryRef))
				.orElseThrow(exception);
		}

		else {
			return getRegistryEntryLookup(registryRef).orElseThrow(exception);
		}

	}

	public static <R, I> Optional<RegistryEntryLookup<R>> getRegistryEntryLookup(DynamicOps<I> ops, RegistryKey<? extends Registry<R>> registryRef) {

		if (ops instanceof RegistryOps<I> registryOps) {
			return registryOps.getEntryLookup(registryRef).or(() -> getRegistryEntryLookup(registryRef));
		}

		else {
			return getRegistryEntryLookup(registryRef);
		}

	}

	public static <R> Optional<RegistryEntryLookup<R>> getRegistryEntryLookup(RegistryKey<? extends Registry<R>> registryRef) {
		return Optional.ofNullable(DYNAMIC_REGISTRIES.get(Unit.INSTANCE)).flatMap(drm -> drm.getOptionalWrapper(registryRef));
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

	public static DataException createMissingRequiredFieldError(String name) {
		return new DataException(DataException.Phase.READING, name, "Field is required, but is missing!");
	}

}
