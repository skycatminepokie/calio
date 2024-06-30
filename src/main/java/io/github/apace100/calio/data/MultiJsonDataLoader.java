package io.github.apace100.calio.data;

import com.google.gson.*;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
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
 *  <p>Like {@link net.minecraft.resource.JsonDataLoader}, except it provides a list of {@link JsonElement JsonElements} associated
 *  with an {@link Identifier}, where each element is loaded by different resource packs. This allows for overriding and merging several
 *  data files into one, similar to how tags work. There is no guarantee on the order of the resulting list, so make sure to implement
 *  some kind of "priority" system.</p>
 *
 *  <p>This is <b>deprecated</b> in favor of using {@link IdentifiableMultiJsonDataLoader}.</p>
 */
@Deprecated
public abstract class MultiJsonDataLoader extends SinglePreparationResourceReloader<Map<Identifier, List<JsonElement>>> implements IExtendedJsonDataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiJsonDataLoader.class);

    private final Gson gson;
    protected final String directoryName;

    public MultiJsonDataLoader(Gson gson, String directoryName) {
        this.gson = gson;
        this.directoryName = directoryName;
    }

    @Override
    protected Map<Identifier, List<JsonElement>> prepare(ResourceManager manager, Profiler profiler) {

        Map<Identifier, List<JsonElement>> result = new HashMap<>();
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

                    else {
                        result
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
    public void onError(String packName, Identifier resourceId, String fileExtension, Exception exception) {
        String filePath = packName + "/.../" + resourceId.getNamespace() + "/" + directoryName + "/" + resourceId.getPath() + fileExtension;
        LOGGER.error("Couldn't parse data file \"{}\" from \"{}\":", resourceId, filePath, exception);
    }

}
