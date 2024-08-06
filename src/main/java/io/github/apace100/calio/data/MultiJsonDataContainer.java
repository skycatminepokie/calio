package io.github.apace100.calio.data;

import com.google.gson.JsonElement;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Set;

public class MultiJsonDataContainer extends LinkedHashMap<Identifier, Set<MultiJsonDataContainer.Entry>> {

    public void forEach(Processor processor) {
        this.forEach((id, entries) -> entries.forEach(entry -> processor.process(entry.source(), id, entry.jsonData())));
    }

    public static Entry entry(String source, JsonElement jsonData) {
        return new Entry(source, jsonData);
    }

    @FunctionalInterface
    public interface Processor {
        void process(String packName, Identifier id, JsonElement jsonElement);
    }

    public record Entry(String source, JsonElement jsonData) {

    }

}
