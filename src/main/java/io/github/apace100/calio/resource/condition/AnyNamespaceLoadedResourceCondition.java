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

public record AnyNamespaceLoadedResourceCondition(List<String> namespaces) implements ResourceCondition {

    public static final MapCodec<AnyNamespaceLoadedResourceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.listOf().fieldOf("namespaces").forGetter(AnyNamespaceLoadedResourceCondition::namespaces)
    ).apply(instance, AnyNamespaceLoadedResourceCondition::new));

    @Override
    public ResourceConditionType<?> getType() {
        return CalioResourceConditions.ANY_NAMESPACE_LOADED;
    }

    @Override
    public boolean test(@Nullable RegistryWrapper.WrapperLookup registryLookup) {
        return CalioResourceConditions.namespacesLoaded(namespaces, false);
    }

}
