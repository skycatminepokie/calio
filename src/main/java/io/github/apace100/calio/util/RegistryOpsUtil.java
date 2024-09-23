package io.github.apace100.calio.util;

import com.mojang.serialization.DynamicOps;
import io.github.apace100.calio.CalioServer;
import io.github.apace100.calio.mixin.CachedRegistryInfoGetterAccessor;
import io.github.apace100.calio.mixin.RegistryOpsAccessor;
import net.minecraft.registry.*;

import java.util.Optional;

public class RegistryOpsUtil {

	public static <I> Optional<RegistryOps<I>> getOrCreate(DynamicOps<I> ops) {

		if (ops instanceof RegistryOps<I> regOps) {
			return Optional.of(regOps);
		}

		else {
			return CalioServer.getDynamicRegistries().map(drm -> drm.getOps(ops));
		}

	}

	public static <I> Optional<RegistryWrapper.WrapperLookup> getWrapperLookup(DynamicOps<I> ops) {
		return CalioServer.getDynamicRegistries()
			.map(RegistryWrapper.WrapperLookup.class::cast)
			.or(() -> {

				RegistryOps.RegistryInfoGetter infoGetter = getOrCreate(ops)
					.map(RegistryOpsAccessor.class::cast)
					.map(RegistryOpsAccessor::getRegistryInfoGetter)
					.orElse(null);

				return infoGetter instanceof CachedRegistryInfoGetterAccessor cachedInfoGetter
					? Optional.of(cachedInfoGetter.getRegistriesLookup())
					: Optional.empty();

			});
	}

	public static <T, I> Optional<RegistryEntryLookup<T>> getEntryLookup(DynamicOps<I> ops, RegistryKey<? extends Registry<T>> registryRef) {
		return CalioServer.getDynamicRegistries()
			.flatMap(registries -> registries.getOptionalWrapper(registryRef))
			.map(impl -> (RegistryEntryLookup<T>) impl)
			.or(() -> getOrCreate(ops)
				.flatMap(registryOps -> registryOps.getEntryLookup(registryRef)));
	}

}
