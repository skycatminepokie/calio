package io.github.apace100.calio.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.apace100.calio.util.CalioResourceConditions;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.apache.commons.io.FilenameUtils;
import org.quiltmc.parsers.json.JsonFormat;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.gson.GsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.*;

/**
 *  Similar to {@link MultiJsonDataLoader}, except it provides a {@link MultiJsonDataContainer}, which contains a map of {@link Identifier} and a
 *  {@link List} of {@link JsonElement JsonElements} associated with a {@link String} that identifies the data/resource pack the JSON data is from.
 */
public abstract class IdentifiableMultiJsonDataLoader extends SinglePreparationResourceReloader<MultiJsonDataContainer> implements IExtendedJsonDataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableMultiJsonDataLoader.class);

    private final Gson gson;

    protected final ResourceType resourceType;
    protected final String directoryName;

    public IdentifiableMultiJsonDataLoader(Gson gson, String directoryName, ResourceType resourceType) {
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

                String packName = resource.getResourcePackName();
                try (Reader resourceReader = resource.getReader()) {

                    GsonReader gsonReader = new GsonReader(JsonReader.create(resourceReader, jsonFormat));
                    JsonElement jsonElement = gson.fromJson(gsonReader, JsonElement.class);

                    if (jsonElement == null) {
                        throw new JsonParseException("JSON cannot be empty!");
                    }

                    else if (jsonElement instanceof JsonObject jsonObject && !CalioResourceConditions.objectMatchesConditions(resourceId, jsonObject)) {
                        this.onReject(packName, fileId, resourceId);
                    }

                    else {
                        result
                            .computeIfAbsent(resourceId, k -> new LinkedHashMap<>())
                            .computeIfAbsent(packName, k -> new LinkedList<>())
                            .add(jsonElement);
                    }

                }

                catch (Exception e) {
                    this.onError(packName, fileId, resourceId, e);
                }

            }

        });

        return result;

    }

    @Override
    public void onError(String packName, Identifier fileId, Identifier resourceId, Exception exception) {
        String filePath = packName + "/" + resourceType.getDirectory() + "/" + fileId.getNamespace() + "/" + fileId.getPath();
        LOGGER.error("Couldn't parse data file \"{}\" from \"{}\"", resourceId, filePath, exception);
    }
}
