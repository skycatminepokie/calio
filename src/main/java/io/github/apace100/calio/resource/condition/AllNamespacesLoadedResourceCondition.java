package io.github.apace100.calio.resource.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.util.CalioResourceConditions;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.registry.RegistryWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record AllNamespacesLoadedResourceCondition(List<String> namespaces) implements ResourceCondition {

    public static final MapCodec<AllNamespacesLoadedResourceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.listOf().fieldOf("namespaces").forGetter(AllNamespacesLoadedResourceCondition::namespaces)
    ).apply(instance, AllNamespacesLoadedResourceCondition::new));

    @Override
    public ResourceConditionType<?> getType() {
        return CalioResourceConditions.ALL_NAMESPACES_LOADED;
    }

    @Override
    public boolean test(@Nullable RegistryWrapper.WrapperLookup registryLookup) {
        return CalioResourceConditions.namespacesLoaded(namespaces, true);
    }

}
