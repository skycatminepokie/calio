package io.github.apace100.calio.registry;

import com.google.common.collect.ImmutableSet;
import com.google.gson.*;
import com.mojang.serialization.*;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.data.*;
import io.github.apace100.calio.network.packet.s2c.SyncDataObjectRegistryS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;

//  TODO: Generalize this to use codecs/packet codecs -eggohito
public class DataObjectRegistry<T extends DataObject<T>> {

    private static final HashMap<Identifier, DataObjectRegistry<?>> REGISTRIES = new HashMap<>();
    private static final Set<Identifier> AUTO_SYNC_SET = new HashSet<>();

    private final Identifier registryId;

    private final Map<Identifier, T> idToEntry = new HashMap<>();
    private final Map<T, Identifier> entryToId = new HashMap<>();
    private final Map<Identifier, T> staticEntries = new HashMap<>();

    private final String factoryFieldName;
    private final DataObjectFactory<T> defaultFactory;
    private final Map<Identifier, DataObjectFactory<T>> factoriesById = new HashMap<>();
    private final Map<DataObjectFactory<T>, Identifier> factoryToId = new HashMap<>();

    private SerializableDataType<T> dataType;
    private SerializableDataType<List<T>> listDataType;
    private SerializableDataType<T> registryDataType;
    private SerializableDataType<Supplier<T>> lazyDataType;

    @NotNull
    private final UnaryOperator<JsonElement> jsonPreProcessor;

    private Loader loader;

    private DataObjectRegistry(Identifier registryId, String factoryFieldName, DataObjectFactory<T> defaultFactory, @NotNull UnaryOperator<JsonElement> jsonPreProcessor) {
        this.registryId = registryId;
        this.factoryFieldName = factoryFieldName;
        this.defaultFactory = defaultFactory;
        this.jsonPreProcessor = jsonPreProcessor;
    }

    private DataObjectRegistry(Identifier registryId, String factoryFieldName, DataObjectFactory<T> defaultFactory, @NotNull UnaryOperator<JsonElement> jsonPreProcessor, String dataFolder, boolean useLoadingPriority, ImmutableSet.Builder<Identifier> dependencies, @Nullable BiConsumer<Identifier, Exception> legacyErrorHandler, @Nullable TriConsumer<Identifier, String, Exception> errorHandler, @Nullable ResourceType resourceType) {
        this(registryId, factoryFieldName, defaultFactory, jsonPreProcessor);
        this.loader = new Loader(dataFolder, useLoadingPriority, dependencies, legacyErrorHandler, errorHandler, resourceType);
    }

    /**
     * @return the resource reload listener which loads the data from data packs. This is not registered automatically, thus you need to
     *         register it, preferably in {@link net.fabricmc.fabric.api.resource.ResourceManagerHelper}
     */
    public IdentifiableResourceReloadListener getLoader() {
        return loader;
    }

    public Identifier getRegistryId() {
        return registryId;
    }

    public Identifier getId(T entry) {
        return entryToId.get(entry);
    }

    public DataObjectFactory<T> getFactory(Identifier id) {
        return factoriesById.get(id);
    }

    public Identifier getFactoryId(DataObjectFactory<T> factory) {
        return factoryToId.get(factory);
    }

    public void registerFactory(Identifier id, DataObjectFactory<T> factory) {
        factoriesById.put(id, factory);
        factoryToId.put(factory, id);
    }

    public void register(Identifier id, T entry) {
        idToEntry.put(id, entry);
        entryToId.put(entry, id);
    }

    public void registerStatic(Identifier id, T entry) {
        staticEntries.put(id, entry);
        register(id, entry);
    }

    public void send(RegistryByteBuf buf) {

        int totalCount = Math.abs(idToEntry.size() - staticEntries.size());
        buf.writeInt(totalCount);

        idToEntry.forEach((id, entry) -> {

            //  Static entries are added via code by mods, so they don't have to be synced to clients (as clients
            //  are expected to have the same mods)
            if (!staticEntries.containsKey(id)) {
                buf.writeIdentifier(id);
                sendDataObject(buf, entry);
            }

        });

    }

    public void sendDataObject(RegistryByteBuf buf, T t) {

        DataObjectFactory<T> factory = t.getFactory();
        buf.writeIdentifier(factoryToId.get(factory));

        SerializableData.Instance factoryData = factory.toData(t);
        factory.getSerializableData().send(buf, factoryData);

    }

    public DataObjectRegistry<T> receive(RegistryByteBuf buf) {

        clear();
        int entryCount = buf.readVarInt();

        for (int i = 0; i < entryCount; i++) {

            Identifier entryId = buf.readIdentifier();
            T entry = receiveDataObject(buf);

            register(entryId, entry);

        }

        return this;

    }

    public T receiveDataObject(RegistryByteBuf buf) {

        Identifier factoryId = buf.readIdentifier();
        DataObjectFactory<T> factory = getFactory(factoryId);

        SerializableData.Instance data = factory.getSerializableData().receive(buf);
        return factory.fromData(data);

    }

    public JsonElement writeDataObject(T t) {
        return t.getFactory().getSerializableData().codec()
            .encodeStart(JsonOps.INSTANCE, t.getFactory().toData(t))
            .resultOrPartial(Calio.LOGGER::warn)
            .orElseGet(JsonObject::new);
    }

    public T readDataObject(JsonElement element) {
        element = jsonPreProcessor.apply(element);
        if(!element.isJsonObject()) {
            throw new JsonParseException(
                "Could not read data object of type \"" + registryId +
                    "\": expected a json object.");
        }
        JsonObject jsonObject = element.getAsJsonObject();
        if(!jsonObject.has(factoryFieldName) && defaultFactory == null) {
            throw new JsonParseException("Could not read data object of type \"" + registryId +
                "\": no factory identifier provided (expected key: \"" + factoryFieldName + "\").");
        }
        DataObjectFactory<T> factory;
        if(jsonObject.has(factoryFieldName)) {
            String type = JsonHelper.getString(jsonObject, factoryFieldName);
            Identifier factoryId = null;
            try {
                factoryId = Identifier.of(type);
            } catch (InvalidIdentifierException e) {
                throw new JsonParseException(
                    "Could not read data object of type \"" + registryId +
                        "\": invalid factory identifier (id: \"" + factoryId + "\").", e);
            }
            if(!factoriesById.containsKey(factoryId)) {
                throw new JsonParseException(
                    "Could not read data object of type \"" + registryId +
                        "\": unknown factory (id: \"" + factoryId + "\").");
            }
            factory = getFactory(factoryId);
        } else {
            factory = defaultFactory;
        }

        SerializableData.Instance data = factory.getSerializableData().decoder().parse(JsonOps.INSTANCE, jsonObject).getOrThrow(JsonParseException::new);
        return factory.fromData(data);

    }

    public void sync(ServerPlayerEntity player) {

        if (player.server.isDedicated()) {
            ServerPlayNetworking.send(player, new SyncDataObjectRegistryS2CPacket(this));
        }

    }

    public void clear() {
        idToEntry.clear();
        entryToId.clear();
        staticEntries.forEach(this::register);
    }

    @Nullable
    public T get(Identifier id) {
        return idToEntry.get(id);
    }

    public Set<Identifier> getIds() {
        return idToEntry.keySet();
    }

    public boolean containsId(Identifier id) {
        return idToEntry.containsKey(id);
    }

    @NotNull
    public Iterator<T> iterator() {
        return idToEntry.values().iterator();
    }

    public SerializableDataType<T> dataType() {
        if(dataType == null) {
            dataType = createDataType();
        }
        return dataType;
    }

    public SerializableDataType<List<T>> listDataType() {
        if(dataType == null) {
            dataType = createDataType();
        }
        if(listDataType == null) {
            listDataType = SerializableDataType.list(dataType);
        }
        return listDataType;
    }

    public SerializableDataType<T> registryDataType() {
        if(registryDataType == null) {
            registryDataType = createRegistryDataType();
        }
        return registryDataType;
    }

    public SerializableDataType<Supplier<T>> lazyDataType() {
        if(lazyDataType == null) {
            lazyDataType = createLazyDataType();
        }
        return lazyDataType;
    }

    private SerializableDataType<Supplier<T>> createLazyDataType() {
        return SerializableDataTypes.IDENTIFIER.xmap(
            id -> () -> get(id),
            lazy -> getId(lazy.get())
        );
    }

    private SerializableDataType<T> createDataType() {
        return SerializableDataType.jsonBacked(
            this::sendDataObject,
            this::receiveDataObject,
            this::writeDataObject,
            this::readDataObject
        );
    }

    private SerializableDataType<T> createRegistryDataType() {
        return SerializableDataTypes.IDENTIFIER.xmap(
            this::get,
            this::getId
        );
    }

    public static DataObjectRegistry<?> getRegistry(Identifier registryId) {
        return REGISTRIES.get(registryId);
    }

    @ApiStatus.Internal
    public static void updateRegistry(DataObjectRegistry<?> registry) {
        REGISTRIES.put(registry.getRegistryId(), registry);
    }

    public static void performAutoSync(ServerPlayerEntity player) {

        for (Identifier registryId : AUTO_SYNC_SET) {
            DataObjectRegistry<?> registry = getRegistry(registryId);
            registry.sync(player);
        }

    }

    private class Loader extends IdentifiableMultiJsonDataLoader implements IdentifiableResourceReloadListener {

        private static final Map<Identifier, Integer> LOADING_PRIORITIES = new HashMap<>();
        private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

        private final BiConsumer<Identifier, Exception> legacyErrorHandler;
        private final TriConsumer<Identifier, String, Exception> errorHandler;

        private final ImmutableSet<Identifier> dependencies;
        private final boolean useLoadingPriority;

        public Loader(String dataFolder, boolean useLoadingPriority, ImmutableSet.Builder<Identifier> dependencies, @Nullable BiConsumer<Identifier, Exception> legacyErrorHandler, @Nullable TriConsumer<Identifier, String, Exception> errorHandler, @Nullable ResourceType resourceType) {
            super(GSON, dataFolder, resourceType);
            this.useLoadingPriority = useLoadingPriority;
            this.legacyErrorHandler = legacyErrorHandler;
            this.errorHandler = errorHandler;
            this.dependencies = dependencies.build();
        }

        @Override
        protected void apply(MultiJsonDataContainer prepared, ResourceManager manager, Profiler profiler) {

            clear();
            LOADING_PRIORITIES.clear();

            prepared.forEach((packName, id, jsonElement) -> {

                try {

                    SerializableData.CURRENT_NAMESPACE = id.getNamespace();
                    SerializableData.CURRENT_PATH = id.getPath();

                    if (!(jsonElement instanceof JsonObject jsonObject)) {
                        throw new JsonSyntaxException("Expected a JSON object");
                    }

                    T object = readDataObject(jsonObject);
                    if (useLoadingPriority) {

                        int loadingPriority = JsonHelper.getInt(jsonObject, "loading_priority", 0);
                        if (!containsId(id) || LOADING_PRIORITIES.getOrDefault(id, 0) < loadingPriority) {
                            LOADING_PRIORITIES.put(id, loadingPriority);
                            register(id, object);
                        }

                    }

                    else {
                        register(id, object);
                    }

                }

                catch (Exception e) {

                    if (errorHandler != null) {
                        errorHandler.accept(id, packName, e);
                    }

                    else if (legacyErrorHandler != null) {
                        legacyErrorHandler.accept(id, e);
                    }

                }

            });

            LOADING_PRIORITIES.clear();

        }

        @Override
        public Identifier getFabricId() {
            return registryId;
        }

        @Override
        public Collection<Identifier> getFabricDependencies() {
            return dependencies;
        }

    }

    public static class Builder<T extends DataObject<T>> {

        private final ImmutableSet.Builder<Identifier> dependencies = new ImmutableSet.Builder<>();

        private final Identifier registryId;

        private DataObjectFactory<T> defaultFactory;
        private UnaryOperator<JsonElement> jsonPreProcessor = UnaryOperator.identity();

        @Nullable
        private BiConsumer<Identifier, Exception> legacyErrorHandler;
        @Nullable
        private TriConsumer<Identifier, String, Exception> errorHandler;

        @Nullable
        private ResourceType resourceType;

        private String dataFolder;
        private String factoryFieldName = "type";

        private boolean autoSync;
        private boolean readFromData;
        private boolean useLoadingPriority;

        /**
         *  <b>Use {@link #Builder(Identifier)} instead.</b>
         */
        @Deprecated(forRemoval = true)
        public Builder(Identifier registryId, Class<T> objectClass) {
            this(registryId);
        }

        public Builder(Identifier registryId) {

            this.registryId = registryId;

            if (REGISTRIES.containsKey(registryId)) {
                throw new IllegalArgumentException("A data object registry with ID \"" + registryId + "\" already exists!");
            }

        }

        public Builder<T> autoSync() {
            this.autoSync = true;
            return this;
        }

        public Builder<T> defaultFactory(DataObjectFactory<T> factory) {
            this.defaultFactory = factory;
            return this;
        }

        public Builder<T> jsonPreprocessor(UnaryOperator<JsonElement> preProcessor) {
            this.jsonPreProcessor = preProcessor;
            return this;
        }

        public Builder<T> factoryFieldName(String factoryFieldName) {
            this.factoryFieldName = factoryFieldName;
            return this;
        }

        public Builder<T> readFromData(String dataFolder, boolean useLoadingPriority) {
            this.readFromData = true;
            this.dataFolder = dataFolder;
            this.useLoadingPriority = useLoadingPriority;
            return this;
        }

        /**
         *  <p>Use {@link #dataErrorHandler(TriConsumer)} instead.</p>
         */
        @Deprecated
        public Builder<T> dataErrorHandler(BiConsumer<Identifier, Exception> handler) {
            this.legacyErrorHandler = handler;
            return this;
        }

        public Builder<T> dataErrorHandler(TriConsumer<Identifier, String, Exception> handler) {
            this.errorHandler = handler;
            return this;
        }

        public Builder<T> dependencies(Identifier... dependencies) {

            for (Identifier dependency : dependencies) {
                this.dependencies.add(dependency);
            }

            return this;

        }

        public Builder<T> resourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public DataObjectRegistry<T> buildAndRegister() {

            DataObjectRegistry<T> registry = readFromData
                ? new DataObjectRegistry<>(registryId, factoryFieldName, defaultFactory, jsonPreProcessor, dataFolder, useLoadingPriority, dependencies, legacyErrorHandler, errorHandler, resourceType)
                : new DataObjectRegistry<>(registryId, factoryFieldName, defaultFactory, jsonPreProcessor);

            REGISTRIES.put(registryId, registry);
            if (autoSync) {
                AUTO_SYNC_SET.add(registryId);
            }

            return registry;

        }

    }

}
