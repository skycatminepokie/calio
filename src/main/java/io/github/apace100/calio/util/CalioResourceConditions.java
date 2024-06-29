package io.github.apace100.calio.util;

import com.google.gson.*;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.data.DataException;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;
import java.util.function.Predicate;

public class CalioResourceConditions {

    @ApiStatus.Internal
    public static final ThreadLocal<Set<String>> NAMESPACES = new ThreadLocal<>();
    public static final IdentifierAlias ALIASES = new IdentifierAlias();

    public static void register() {
        ResourceConditions.register(Calio.identifier("any_namespace_loaded"), jsonObject -> namespacesLoaded(jsonObject, false));
        ResourceConditions.register(Calio.identifier("all_namespace_loaded"), jsonObject -> namespacesLoaded(jsonObject, true));
    }

    private static boolean namespacesLoaded(JsonObject jsonObject, boolean and) {

        JsonArray namespacesJson = JsonHelper.getArray(jsonObject, "namespaces");
        Set<String> loadedNamespaces = NAMESPACES.get();

        if (loadedNamespaces == null) {
            Calio.LOGGER.warn("Failed to retrieve loaded namespaces!");
            return false;
        }

        for (int i = 0; i < namespacesJson.size(); i++) {

            JsonElement namespaceJson = namespacesJson.get(i);
            if (namespaceJson.isJsonPrimitive()) {

                if (loadedNamespaces.contains(namespaceJson.getAsString()) != and) {
                    return !and;
                }

            }

            else {
                throw new DataException(DataException.Phase.READING, "[" + i + "]", new JsonSyntaxException("Expected a JSON string, was " + JsonHelper.getType(namespaceJson)));
            }

        }

        return and;

    }

    public static boolean objectMatchesConditions(Identifier objectId, JsonObject objectJson) {

        JsonArray conditionsJson = JsonHelper.getArray(objectJson, ResourceConditions.CONDITIONS_KEY, null);
        if (conditionsJson == null) {
            return true;
        }

        try {
            return conditionsMatch(conditionsJson, true);
        }

        catch (Exception e) {
            ResourceConditionsImpl.LOGGER.warn("Skipping object \"{}\". Failed to parse resource conditions", objectId, e);
            return false;
        }

    }

    public static boolean conditionsMatch(JsonArray conditionsJson, boolean and) {

        for (int i = 0; i < conditionsJson.size(); i++) {

            JsonElement conditionJson = conditionsJson.get(i);
            if (conditionJson instanceof JsonObject conditionObject) {

                if (conditionMatches(conditionObject) != and) {
                    return !and;
                }

            }

            else {
                throw new DataException(DataException.Phase.READING, "[" + i + "]", new JsonSyntaxException("Expected a JSON object, was " + JsonHelper.getType(conditionJson)));
            }

        }

        return and;

    }

    public static boolean conditionMatches(JsonObject conditionObject) {

        Identifier conditionId = new Identifier(JsonHelper.getString(conditionObject, ResourceConditions.CONDITION_ID_KEY));
        Predicate<JsonObject> condition = ResourceConditions.get(ALIASES.resolveAlias(conditionId, id -> ResourceConditions.get(id) != null));

        if (condition == null) {
            throw new JsonParseException("Unknown resource condition: " + conditionId);
        }

        else {
            return condition.test(conditionObject);
        }

    }

}
