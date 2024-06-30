package io.github.apace100.calio.data;

import com.google.gson.JsonElement;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MultiJsonDataContainer extends LinkedHashMap<String, Map<Identifier, List<JsonElement>>> {

    public void forEach(Processor processor) {
        this.forEach((packName, idAndData) ->
            idAndData.forEach((id, data) ->
                data.forEach(jsonElement -> processor.process(packName, id, jsonElement))));
    }

    @FunctionalInterface
    public interface Processor {
        void process(String packName, Identifier id, JsonElement jsonElement);
    }

}
