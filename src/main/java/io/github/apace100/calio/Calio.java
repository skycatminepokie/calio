package io.github.apace100.calio;

import com.mojang.serialization.DataResult;
import io.github.apace100.calio.codec.CalioCodecs;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.calio.network.packet.s2c.SyncDataObjectRegistryS2CPacket;
import io.github.apace100.calio.registry.DataObjectRegistry;
import io.github.apace100.calio.util.CalioResourceConditions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Calio implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger(Calio.class);
	public static final String MOD_NAMESPACE = "calio";

	/**
	 * 	Only to be used in {@link CalioResourceConditions}
	 */
	@ApiStatus.Internal
	public static final Map<Unit, Collection<String>> LOADED_NAMESPACES = new ConcurrentHashMap<>();

	@Override
	public void onInitialize() {

		CalioCodecs.init();
		SerializableDataTypes.init();

        Criteria.register(CodeTriggerCriterion.ID.toString(), CodeTriggerCriterion.INSTANCE);
		CalioResourceConditions.register();

		PayloadTypeRegistry.playS2C().register(SyncDataObjectRegistryS2CPacket.PACKET_ID, SyncDataObjectRegistryS2CPacket.PACKET_CODEC);
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> DataObjectRegistry.performAutoSync(player));

	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_NAMESPACE, path);
	}

	/**
	 * 	<b>Use {@link TagKey#equals(Object)} (or {@link Objects#equals(Object, Object)} if nullability is of concern) instead.</b>
	 */
	@Deprecated(forRemoval = true)
	public static <T> boolean areTagsEqual(RegistryKey<? extends Registry<T>> registryKey, TagKey<T> tag1, TagKey<T> tag2) {
		return areTagsEqual(tag1, tag2);
	}

	/**
	 * 	<b>Use {@link TagKey#equals(Object)} (or {@link Objects#equals(Object, Object)} if nullability is of concern) instead.</b>
	 */
	@Deprecated(forRemoval = true)
	public static <T> boolean areTagsEqual(TagKey<T> tag1, TagKey<T> tag2) {
		return tag1 == tag2
			|| tag1 != null
			&& tag2 != null
			&& tag1.registry().equals(tag2.registry())
			&& tag1.id().equals(tag2.id());
	}

	public static <R> DataResult<R> createMissingRequiredFieldError(String name) {
		return DataResult.error(() -> "Required field \"" + name + "\" is missing!");
	}

}
