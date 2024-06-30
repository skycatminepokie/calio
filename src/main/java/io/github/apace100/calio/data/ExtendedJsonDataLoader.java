package io.github.apace100.calio.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.apace100.calio.util.CalioResourceConditions;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.apache.commons.io.FilenameUtils;
import org.quiltmc.parsers.json.JsonFormat;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.gson.GsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 *  Similar to {@link net.minecraft.resource.JsonDataLoader}, except it supports the JSON5 and JSONC spec.
 */
public abstract class ExtendedJsonDataLoader extends ExtendedSinglePreparationResourceReloader<Map<Identifier, JsonElement>> implements IExtendedJsonDataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedJsonDataLoader.class);

    private final Gson gson;
    protected final String directoryName;

    public ExtendedJsonDataLoader(Gson gson, String directoryName) {
        this.gson = gson;
        this.directoryName = directoryName;
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager, Profiler profiler) {

        Map<Identifier, JsonElement> result = new HashMap<>();
        manager.findResources(directoryName, this::hasValidFormat).forEach((fileId, resource) -> {

            Identifier resourceId = this.trim(fileId, directoryName);
            String fileExtension = "." + FilenameUtils.getExtension(fileId.getPath());

            JsonFormat jsonFormat = this.getValidFormats().get(fileExtension);
            String packName = resource.getResourcePackName();

            try (Reader resourceReader = resource.getReader()) {

                GsonReader gsonReader = new GsonReader(JsonReader.create(resourceReader, jsonFormat));
                JsonElement jsonElement = gson.fromJson(gsonReader, JsonElement.class);

                if (jsonElement == null) {
                    throw new JsonParseException("JSON cannot be empty!");
                }

                if (result.put(resourceId, jsonElement) != null) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + resourceId);
                }

            } catch (Exception e) {
                this.onError(packName, resourceId, fileExtension, e);
            }

        });

        return result;

    }

    @Override
    protected void preApply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {

        var preparedEntryIterator = prepared.entrySet().iterator();
        while (preparedEntryIterator.hasNext()) {

            var preparedEntry = preparedEntryIterator.next();

            Identifier id = preparedEntry.getKey();
            JsonElement jsonElement = preparedEntry.getValue();

            if (!(jsonElement instanceof JsonObject jsonObject) || CalioResourceConditions.objectMatchesConditions(id, jsonObject)) {
                continue;
            }

            this.onReject("", id);
            preparedEntryIterator.remove();

        }

    }

    @Override
    public void onError(String packName, Identifier resourceId, String fileExtension, Exception exception) {
        String filePath = packName + "/.../" + resourceId.getNamespace() + "/" + directoryName + "/" + resourceId.getPath() + fileExtension;
        LOGGER.error("Couldn't parse data file \"{}\" from \"{}\":", resourceId, filePath, exception);
    }

}
