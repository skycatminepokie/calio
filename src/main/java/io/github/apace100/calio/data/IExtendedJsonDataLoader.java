package io.github.apace100.calio.data;

import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.io.FilenameUtils;
import org.quiltmc.parsers.json.JsonFormat;

import java.util.HashMap;
import java.util.Map;

public interface IExtendedJsonDataLoader {

    Map<String, JsonFormat> DEFAULT_VALID_FORMATS = Util.make(new HashMap<>(), map -> {
        map.put(".json", JsonFormat.JSON);
        map.put(".json5", JsonFormat.JSON5);
        map.put(".jsonc", JsonFormat.JSONC);
    });

    /**
     *  <p>Called when a JSON data currently being prepared has been rejected due to its resource conditions not being fulfilled.</p>
     *
     *  @param packName     the name of the data/resource pack the resource is from
     *  @param fileId       the file ID (with the file extension suffix and directory) of the JSON data
     *  @param resourceId   the ID (without the file extension suffix and directory) of the JSON data
     */
    default void onReject(String packName, Identifier fileId, Identifier resourceId) {

    }

    /**
     *  <p>Called when an error occurs upon preparing a JSON data.</p>
     *
     *  @param packName     the name of the data/resource pack the resource is from
     *  @param fileId       the file ID (<b>with</b> the file extension suffix and directory) of the JSON data
     *  @param resourceId   the ID (<b>without</b> the file extension suffix and directory) of the JSON data
     *  @param exception    the {@link Exception} thrown when preparing the JSON data
     */
    default void onError(String packName, Identifier fileId, Identifier resourceId, Exception exception) {

    }

    /**
     *  @return a {@link Map} of supported file extensions ({@link String}) and its associated JSON formats ({@link JsonFormat})
     */
    default Map<String, JsonFormat> getValidFormats() {
        return DEFAULT_VALID_FORMATS;
    }

    default Identifier trim(Identifier fileId, String directoryName) {
        String path = FilenameUtils.removeExtension(fileId.getPath()).substring(directoryName.length() + 1);
        return new Identifier(fileId.getNamespace(), path);
    }

    default boolean hasValidFormat(Identifier fileId) {
        return this.getValidFormats().keySet()
            .stream()
            .anyMatch(suffix -> fileId.getPath().endsWith(suffix));
    }

}
