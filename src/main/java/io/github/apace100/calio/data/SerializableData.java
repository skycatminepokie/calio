package io.github.apace100.calio.data;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.*;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.serialization.StrictMapCodec;
import io.github.apace100.calio.util.Validatable;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class SerializableData extends StrictMapCodec<SerializableData.Instance> {

    // Should be set to the current namespace of the file that is being read. Allows using * in identifiers.
    public static String CURRENT_NAMESPACE;

    // Should be set to the current path of the file that is being read. Allows using * in identifiers.
    public static String CURRENT_PATH;

    protected final Map<String, Field<?>> fields = new LinkedHashMap<>();
    protected Consumer<SerializableData.Instance> postProcessor = data -> {};

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return SerializableData.this.getFieldNames()
            .stream()
            .map(ops::createString);
    }

    @Override
    public <T> Instance strictDecode(DynamicOps<T> ops, MapLike<T> input) {

        Instance data = new Instance();
        for (Map.Entry<String, Field<?>> fieldEntry : fields.entrySet()) {

            String fieldName = fieldEntry.getKey();
            Field<?> field = fieldEntry.getValue();

            try {

                T element = input.get(fieldName);
                if (element != null) {
                    data.set(fieldName, field.dataType().strictParse(ops, element));
                }

                else if (field.hasDefault()) {
                    data.set(fieldName, field.getDefault(data));
                }

                else {
                    throw new NoSuchFieldException("Field is required, but is missing!");
                }

            }

            catch (DataException de) {
                throw de.prepend(fieldName);
            }

            catch (Exception e) {
                throw new DataException(DataException.Phase.READING, fieldName, e);
            }

        }

        postProcessor.accept(data);
        return data;

    }

    @Override
    public <T> RecordBuilder<T> encode(Instance input, DynamicOps<T> ops, RecordBuilder<T> prefix) {

        this.getFields().forEach((name, field) -> {

            try {

                if (input.isPresent(name)) {
                    prefix.add(name, field.dataType().strictEncodeStart(ops, input.get(name)));
                }

            }

            catch (DataException de) {
                throw de.prepend(name);
            }

            catch (Exception e) {
                Calio.LOGGER.error(new DataException(DataException.Phase.WRITING, name, e).getMessage());
            }

        });

        return prefix;

    }

    @Override
    public String toString() {
        return "SerializableData[fields = " + fields + "]";
    }

    /**
     *  Use {@link #receive(RegistryByteBuf)} instead.
     */
    @Deprecated
    public Instance read(RegistryByteBuf buf) {
        return receive(buf);
    }

    public Instance receive(RegistryByteBuf buf) {

        Instance data = instance();
        fields.forEach((name, field) -> {

            SerializableDataType<?> dataType = field.dataType();
            try {

                boolean isPresent = buf.readBoolean();
                boolean hasDefault = buf.readBoolean();

                if (isPresent || hasDefault) {
                    data.set(name, dataType.receive(buf));
                }

            }

            catch (DataException de) {
                throw de.prepend(name);
            }

            catch (Exception e) {
                throw new DataException(DataException.Phase.RECEIVING, name, e);
            }

        });

        return data;

    }

    /**
     *  Use {@link #send(RegistryByteBuf, Instance)} instead.
     */
    @Deprecated
    public void write(RegistryByteBuf buf, Instance data) {
        this.send(buf, data);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void send(RegistryByteBuf buf, Instance data) {

        fields.forEach((name, field) -> {

            SerializableDataType dataType = field.dataType();
            try {

                if (data.get(name) != null) {

                    buf.writeBoolean(true);
                    buf.writeBoolean(false);

                    dataType.send(buf, data.get(name));

                }

                else if (field.hasDefault() && field.getDefault(data) != null) {

                    buf.writeBoolean(false);
                    buf.writeBoolean(true);

                    dataType.send(buf, field.getDefault(data));

                }

                else {

                    buf.writeBoolean(false);
                    buf.writeBoolean(false);

                }

            }

            catch (DataException de) {
                throw de.prepend(name);
            }

            catch (Exception e) {
                throw new DataException(DataException.Phase.SENDING, name, e);
            }

        });

    }

    /**
     *  Use {@link #fromJson(JsonObject)} instead.
     */
    @Deprecated
    public Instance read(JsonObject jsonObject) {
        return this.fromJson(jsonObject);
    }

    public Instance fromJson(JsonObject jsonObject) {
        DynamicOps<JsonElement> ops = Calio.wrapRegistryOps(JsonOps.INSTANCE);
        return this.strictDecode(ops, ops.getMap(jsonObject).getOrThrow());
    }

    /**
     *  Use {@link #toJson(Instance)} instead.
     */
    @Deprecated
    public JsonObject write(Instance data) {
        return this.toJson(data);
    }

    public JsonObject toJson(Instance data) {
        JsonOps ops = JsonOps.INSTANCE;
        return this.encode(data, ops, ops.mapBuilder()).build(ops.empty())
            .map(JsonElement::getAsJsonObject)
            .mapError(err -> "Something went wrong while encoding " + data + " to JSON (skipping): " + err)
            .resultOrPartial(Calio.LOGGER::warn)
            .orElseGet(JsonObject::new);
    }

    public <T> SerializableData add(String name, @NotNull Codec<T> codec) {
        return this.addField(name, new Field<>(SerializableDataType.of(codec)));
    }

    public <T> SerializableData add(String name, @NotNull Codec<T> codec, T defaultValue) {
        return this.addField(name, new Field<>(SerializableDataType.of(codec), Suppliers.memoize(() -> defaultValue)));
    }

    public <T> SerializableData addSupplied(String name, @NotNull Codec<T> codec, @NotNull Supplier<T> defaultSupplier) {
        return this.addField(name, new Field<>(SerializableDataType.of(codec), defaultSupplier));
    }

    public <T> SerializableData addFunctionedDefault(String name, @NotNull Codec<T> codec, @NotNull Function<Instance, T> defaultFunction) {
        return this.addField(name, new Field<>(SerializableDataType.of(codec), defaultFunction));
    }

    public SerializableData postProcessor(@NotNull Consumer<SerializableData.Instance> postProcessor) {
        this.postProcessor = postProcessor;
        return this;
    }

    protected <T> SerializableData addField(String name, Field<T> field) {

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be empty!");
        }

        else {
            fields.put(name, field);
            return this;
        }

    }

    public Instance instance() {
        return new Instance();
    }

    public SerializableData copy() {

        SerializableData copy = new SerializableData();

        copy.fields.putAll(this.fields);
        copy.postProcessor = this.postProcessor;

        return copy;

    }

    public ImmutableMap<String, Field<?>> getFields() {
        return ImmutableMap.copyOf(fields);
    }

    public ImmutableSet<String> getFieldNames() {
        return ImmutableSet.copyOf(fields.keySet());
    }

    public Field<?> getField(String fieldName) {

        if (!this.containsField(fieldName)) {
            throw new IllegalArgumentException(this + " contains no field with name \"" + fieldName + "\".");
        }

        else {
            return fields.get(fieldName);
        }

    }

    public boolean containsField(String fieldName) {
        return fields.containsKey(fieldName);
    }

    public class Instance implements Validatable {

        private final Map<String, Object> map = new HashMap<>();

        public Instance() {
            SerializableData.this.getFieldNames().forEach(name -> map.putIfAbsent(name, null));
        }

        @Override
        public void validate() throws Exception {

            map.forEach((field, value) -> {

                if (fields.containsKey(field) && value instanceof Validatable validatable) {

                    try {
                        validatable.validate();
                    }

                    catch (DataException de) {
                        throw de.prepend(field);
                    }

                    catch (Exception e) {
                        throw new DataException(DataException.Phase.READING, field, e);
                    }

                }

            });

        }

        public SerializableData serializableData() {
            return SerializableData.this;
        }

        public boolean isPresent(String name) {

            if (fields.containsKey(name)) {

                Field<?> field = fields.get(name);

                if (field.hasDefault() && field.getDefault(this) == null) {
                    return get(name) != null;
                }

            }

            return map.get(name) != null;

        }

        public <T> void ifPresent(String name, Consumer<T> consumer) {

            if (isPresent(name)) {
                consumer.accept(get(name));
            }

        }

        public Instance set(String name, Object value) {
            this.map.put(name, value);
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> T get(String name) {

            if (!map.containsKey(name)) {
                throw new RuntimeException("Tried to get field \"" + name + "\" from data " + this + ", which did not exist.");
            }

            else {
                return (T) map.get(name);
            }

        }

        public <T> T getOrElse(String name, T defaultValue) {
            return getOrElseGet(name, Suppliers.memoize(() -> defaultValue));
        }

        public <T> T getOrElseGet(String name, Supplier<T> defaultSupplier) {

            if (this.isPresent(name)) {
                return this.get(name);
            }

            else {
                return defaultSupplier.get();
            }

        }

        public boolean isEmpty() {
            return fields.isEmpty();
        }

        public int getInt(String name) {
            return get(name);
        }

        public boolean getBoolean(String name) {
            return get(name);
        }

        public float getFloat(String name) {
            return get(name);
        }

        public double getDouble(String name) {
            return get(name);
        }

        public String getString(String name) {
            return get(name);
        }

        public Identifier getId(String name) {
            return get(name);
        }

        public EntityAttributeModifier getModifier(String name) {
            return get(name);
        }

        public <T> DataResult<T> getSafely(Class<T> dataClass, String name) {

            try {
                return DataResult.success(dataClass.cast(this.get(name)));
            }

            catch (Exception e) {
                return DataResult.error(e::getMessage);
            }

        }

        @Override
        public String toString() {
            return "SerializableData$Instance[data = " + map + "]";
        }

    }

    public static class Field<T> {

        protected final SerializableDataType<T> dataType;
        protected final Function<Instance, T> defaultFunction;

        protected final Supplier<T> defaultValue;

        protected final boolean hasDefaultValue;
        protected final boolean hasDefaultFunction;

        public Field(SerializableDataType<T> dataType) {
            this.dataType = dataType;
            this.defaultValue = () -> null;
            this.defaultFunction = null;
            this.hasDefaultValue = false;
            this.hasDefaultFunction = false;
        }

        public Field(SerializableDataType<T> dataType, Supplier<T> defaultValue) {
            this.dataType = dataType;
            this.defaultValue = defaultValue;
            this.defaultFunction = null;
            this.hasDefaultValue = true;
            this.hasDefaultFunction = false;
        }

        public Field(SerializableDataType<T> dataType, @NotNull Function<Instance, T> defaultFunction) {
            this.dataType = dataType;
            this.defaultValue = () -> null;
            this.defaultFunction = defaultFunction;
            this.hasDefaultValue = false;
            this.hasDefaultFunction = true;
        }

        public SerializableDataType<T> dataType() {
            return dataType;
        }

        public boolean hasDefault() {
            return hasDefaultFunction
                || hasDefaultValue;
        }

        public T getDefault(Instance data) {

            if (hasDefaultFunction && defaultFunction != null) {
                return defaultFunction.apply(data);
            }

            else if (hasDefaultValue) {
                return defaultValue.get();
            }

            else {
                throw new IllegalStateException("Tried to access default value of field when no default was provided.");
            }

        }

    }

}
