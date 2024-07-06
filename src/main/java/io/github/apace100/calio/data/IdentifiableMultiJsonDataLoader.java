package io.github.apace100.calio.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.apace100.calio.Calio;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.parsers.json.JsonFormat;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.gson.GsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.*;

/**
 *  <p>Similar to {@link MultiJsonDataLoader}, except it provides a {@link MultiJsonDataContainer}, which contains a map of {@link Identifier} and a
 *  {@link List} of {@link JsonElement JsonElements} associated with a {@link String} that identifies the data/resource pack the JSON data is from.</p>
 */
public abstract class IdentifiableMultiJsonDataLoader extends ExtendedSinglePreparationResourceReloader<MultiJsonDataContainer> implements IExtendedJsonDataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableMultiJsonDataLoader.class);

    private final Gson gson;

    @Nullable
    protected final ResourceType resourceType;
    protected final String directoryName;

    public IdentifiableMultiJsonDataLoader(Gson gson, String directoryName) {
        this(gson, directoryName, null);
    }

    public IdentifiableMultiJsonDataLoader(Gson gson, String directoryName, @Nullable ResourceType resourceType) {
        this.gson = gson;
        this.directoryName = directoryName;
        this.resourceType = resourceType;
    }

    @Override
    protected MultiJsonDataContainer prepare(ResourceManager manager, Profiler profiler) {

        MultiJsonDataContainer result = new MultiJsonDataContainer();
        manager.findAllResources(directoryName, this::hasValidFormat).forEach((fileId, resources) -> {

            Identifier resourceId = this.trim(fileId, directoryName);
            String fileExtension = "." + FilenameUtils.getExtension(fileId.getPath());

            JsonFormat jsonFormat = this.getValidFormats().get(fileExtension);
            for (Resource resource : resources) {

                String packName = resource.getPackId();
                try (Reader resourceReader = resource.getReader()) {

                    GsonReader gsonReader = new GsonReader(JsonReader.create(resourceReader, jsonFormat));
                    JsonElement jsonElement = gson.fromJson(gsonReader, JsonElement.class);

                    if (jsonElement == null) {
                        throw new JsonParseException("JSON cannot be empty!");
                    }

                    else {
                        result
                            .computeIfAbsent(packName, k -> new LinkedHashMap<>())
                            .computeIfAbsent(resourceId, k -> new LinkedList<>())
                            .add(jsonElement);
                    }

                }

                catch (Exception e) {
                    this.onError(packName, resourceId, fileExtension, e);
                }

            }

        });

        return result;

    }

    @Override
    protected void preApply(MultiJsonDataContainer prepared, ResourceManager manager, Profiler profiler) {

        var preparedEntryIterator = prepared.entrySet().iterator();
        while (preparedEntryIterator.hasNext()) {

            var preparedEntry = preparedEntryIterator.next();

            String packName = preparedEntry.getKey();
            Map<Identifier, List<JsonElement>> idAndJsonData = preparedEntry.getValue();

            var idAndJsonDataEntryIterator = idAndJsonData.entrySet().iterator();
            while (idAndJsonDataEntryIterator.hasNext()) {

                var idAndJsonDataEntry = idAndJsonDataEntryIterator.next();

                Identifier id = idAndJsonDataEntry.getKey();
                List<JsonElement> jsonData = idAndJsonDataEntry.getValue();

                Iterator<JsonElement> jsonIterator = jsonData.iterator();
                while (jsonIterator.hasNext()) {

                    JsonElement json = jsonIterator.next();
                    if (!(json instanceof JsonObject jsonObject) || ResourceConditionsImpl.applyResourceConditions(jsonObject, directoryName, id, Calio.DYNAMIC_REGISTRIES.get())) {
                        continue;
                    }

                    this.onReject(packName, id);
                    jsonIterator.remove();

                }

                if (jsonData.isEmpty()) {
                    idAndJsonDataEntryIterator.remove();
                }

            }

            if (idAndJsonData.isEmpty()) {
                preparedEntryIterator.remove();
            }

        }

    }

    @Override
    public void onError(String packName, Identifier resourceId, String fileExtension, Exception exception) {
        String filePath = packName + "/" + (resourceType != null ? resourceType.getDirectory() : "...") + "/" + resourceId.getNamespace() + "/" + directoryName + "/" + resourceId.getPath() + fileExtension;
        LOGGER.error("Couldn't parse data file \"{}\" from \"{}\"", resourceId, filePath, exception);
    }

}
