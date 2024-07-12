package io.github.apace100.calio.util;

import io.github.apace100.calio.Calio;
import io.github.apace100.calio.resource.condition.AllNamespacesLoadedResourceCondition;
import io.github.apace100.calio.resource.condition.AnyNamespaceLoadedResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.Set;

public class CalioResourceConditions {

    public static final IdentifierAlias ALIASES = new IdentifierAlias();

    public static final ResourceConditionType<AnyNamespaceLoadedResourceCondition> ANY_NAMESPACE_LOADED = ResourceConditionType.create(Calio.identifier("any_namespace_loaded"), AnyNamespaceLoadedResourceCondition.CODEC);
    public static final ResourceConditionType<AllNamespacesLoadedResourceCondition> ALL_NAMESPACES_LOADED = ResourceConditionType.create(Calio.identifier("all_namespaces_loaded"), AllNamespacesLoadedResourceCondition.CODEC);

    public static void register() {
        ResourceConditions.register(ANY_NAMESPACE_LOADED);
        ResourceConditions.register(ALL_NAMESPACES_LOADED);
    }

    public static boolean namespacesLoaded(List<String> namespaces, boolean and) {

        Set<String> loadedNamespaces = Calio.LOADED_NAMESPACES.get(Unit.INSTANCE);
        if (loadedNamespaces == null) {
            Calio.LOGGER.warn("Failed to retrieve loaded namespaces!");
            return false;
        }

        for (String namespace : namespaces) {

            if (loadedNamespaces.contains(namespace) != and) {
                return !and;
            }

        }

        return and;

    }

}
