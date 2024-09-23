package io.github.apace100.calio;

import com.google.common.collect.ImmutableMap;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.DataPackContents;
import net.minecraft.util.Unit;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CalioServer {

	@ApiStatus.Internal
	public static final Map<TagKey<?>, Collection<RegistryEntry<?>>> REGISTRY_TAGS = new ConcurrentHashMap<>();
	@ApiStatus.Internal
	public static final Map<Unit, DynamicRegistryManager> DYNAMIC_REGISTRIES = new ConcurrentHashMap<>();
	@ApiStatus.Internal
	public static final Map<Unit, DataPackContents> DATA_PACK_CONTENTS = new ConcurrentHashMap<>();

	public static ImmutableMap<TagKey<?>, Collection<RegistryEntry<?>>> getRegistryTags() {
		return ImmutableMap.copyOf(REGISTRY_TAGS);
	}

	public static Optional<DynamicRegistryManager> getDynamicRegistries() {
		return Optional.ofNullable(DYNAMIC_REGISTRIES.get(Unit.INSTANCE));
	}

	public static Optional<DataPackContents> getDataPackContents() {
		return Optional.ofNullable(DATA_PACK_CONTENTS.get(Unit.INSTANCE));
	}

}
