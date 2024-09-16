package io.github.apace100.calio.data;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.*;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.util.Validatable;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class SerializableData extends MapCodec<SerializableData.Instance> {

    // Should be set to the current namespace of the file that is being read. Allows using * in identifiers.
    public static String CURRENT_NAMESPACE;

    // Should be set to the current path of the file that is being read. Allows using * in identifiers.
    public static String CURRENT_PATH;

    protected final Map<String, Field<?>> fields;
    protected final Function<Instance, DataResult<Instance>> validator;

    protected final boolean root;

    public SerializableData(Map<String, Field<?>> fields, Function<Instance, DataResult<Instance>> validator, boolean root) {
        this.fields = new Object2ObjectLinkedOpenHashMap<>(fields);
        this.validator = validator;
        this.root = root;
    }

    public SerializableData() {
        this.fields = new Object2ObjectLinkedOpenHashMap<>();
        this.validator = DataResult::success;
        this.root = true;
    }

    @Override
    public SerializableData validate(Function<Instance, DataResult<Instance>> validator) {
        return new SerializableData(this.fields, validator, this.root);
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return SerializableData.this.getFieldNames()
            .stream()
            .map(ops::createString);
    }

    @Override
    public <T> DataResult<Instance> decode(DynamicOps<T> ops, MapLike<T> mapInput) {

        try {

            Instance data = instance();
            Map<String, Field<?>> defaultedFields = new Object2ObjectLinkedOpenHashMap<>();

            for (Map.Entry<String, Field<?>> fieldEntry : getFields().entrySet()) {

                String fieldName = fieldEntry.getKey();
                Field<?> field = fieldEntry.getValue();

                try {

                    if (mapInput.get(fieldName) != null) {
                        data.set(fieldName, field.decode(ops, mapInput.get(fieldName)));
                    }

                    else if (field.hasDefault()) {
                        defaultedFields.put(fieldName, field);
                    }

                    else {
                        throw new NoSuchFieldException("Required field \"" + fieldName + "\" is missing!");
                    }

                }

                catch (DataException de) {
                    throw de.prepend(field.path());
                }

                catch (NoSuchFieldException nsfe) {
                    throw new DataException(DataException.Phase.READING, "", nsfe);
                }

                catch (Exception e) {
                    throw new DataException(DataException.Phase.READING, field.path(), e);
                }

            }

            defaultedFields.forEach((fieldName, field) -> data.set(fieldName, field.getDefault(data)));
            return DataResult.success(data).flatMap(validator);

        }

        catch (Exception e) {

            if (root) {
                return DataResult.error(e::getMessage);
            }

            else {
                throw e;
            }

        }

    }

    @Override
    public <T> RecordBuilder<T> encode(Instance input, DynamicOps<T> ops, RecordBuilder<T> prefix) {

        try {

            for (Map.Entry<String, Field<?>> fieldEntry : getFields().entrySet()) {

                String fieldName = fieldEntry.getKey();
                Field<?> field = fieldEntry.getValue();

                try {

                    if (input.isPresent(fieldName)) {
                        prefix.add(fieldName, field.encode(ops, input.get(fieldName)));
                    }

                }

                catch (DataException de) {
                    throw de.prepend(field.path());
                }

                catch (Exception e) {
                    throw new DataException(DataException.Phase.WRITING, field.path(), e);
                }

            }

            return prefix;

        }

        catch (Exception e) {

            if (root) {
                return prefix.withErrorsFrom(DataResult.error(e::getMessage));
            }

            else {
                throw e;
            }

        }

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
    @Deprecated(forRemoval = true)
    public Instance read(JsonObject jsonObject) {
        return this.fromJson(jsonObject);
    }

    public Instance fromJson(JsonObject jsonObject) {
        DynamicOps<JsonElement> ops = Calio.wrapRegistryOps(JsonOps.INSTANCE);
        return ops.getMap(jsonObject)
            .flatMap(mapLike -> decode(ops, mapLike))
            .getOrThrow();
    }

    /**
     *  Use {@link #toJson(Instance)} instead.
     */
    @Deprecated(forRemoval = true)
    public JsonObject write(Instance data) {
        return this.toJson(data);
    }

    public JsonObject toJson(Instance data) {
        return encoder().encodeStart(JsonOps.INSTANCE, data)
            .flatMap(jsonElement -> jsonElement instanceof JsonObject jsonObject
                ? DataResult.success(jsonObject)
                : DataResult.error(() -> "Not a JSON object: " + jsonElement))
            .getOrThrow();
    }

    public <T> SerializableData add(String name, @NotNull Codec<T> codec) {
        return add(name, SerializableDataType.of(codec));
    }

    public <T> SerializableData add(String name, @NotNull Codec<T> codec, T defaultValue) {
        return add(name, SerializableDataType.of(codec), defaultValue);
    }

    public <T> SerializableData addSupplied(String name, @NotNull Codec<T> codec, @NotNull Supplier<T> defaultSupplier) {
        return addSupplied(name, SerializableDataType.of(codec), defaultSupplier);
    }

    public <T> SerializableData addFunctionedDefault(String name, @NotNull Codec<T> codec, @NotNull Function<Instance, T> defaultFunction) {
        return addFunctionedDefault(name, SerializableDataType.of(codec), defaultFunction);
    }

    public <T> SerializableData add(String name, @NotNull SerializableDataType<T> dataType) {
        return addField(name, dataType.field(name));
    }

    public <T> SerializableData add(String name, @NotNull SerializableDataType<T> dataType, T defaultValue) {
        return addField(name, dataType.field(name, Suppliers.memoize(() -> defaultValue)));
    }

    public <T> SerializableData addSupplied(String name, @NotNull SerializableDataType<T> dataType, @NotNull Supplier<T> defaultSupplier) {
        return addField(name, dataType.field(name, defaultSupplier));
    }

    public <T> SerializableData addFunctionedDefault(String name, @NotNull SerializableDataType<T> dataType, @NotNull Function<Instance, T> defaultFunction) {
        return addField(name, dataType.functionedField(name, defaultFunction));
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

    public boolean isRoot() {
        return root;
    }

	public SerializableData setRoot(boolean root) {
        return new SerializableData(this.fields, this.validator, root);
    }

    public SerializableData copy() {
        return new SerializableData(this.fields, this.validator, this.root);
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

    public Instance instance() {
        return new Instance();
    }

    public class Instance implements Validatable {

        private final Map<String, Object> map = new HashMap<>();

        public Instance() {
            SerializableData.this.getFieldNames().forEach(name -> map.putIfAbsent(name, null));
        }

        @Override
        public void validate() throws Exception {

            getFields().forEach((name, field) -> {

                if (!map.containsKey(name)) {
                    return;
                }

                try {

                    switch (map.get(name)) {
                        case List<?> list -> {

                            int index = 0;
                            for (Object element : list) {

                                try {

                                    if (element instanceof Validatable validatable) {
                                        validatable.validate();
                                    }

                                    index++;

                                }

                                catch (DataException de) {
                                    throw de.prependArray(index);
                                }

                                catch (Exception e) {
                                    throw new DataException(DataException.Phase.READING, DataException.Type.ARRAY, "[" + index + "]", e.getMessage());
                                }

                            }

                        }
                        case Validatable validatable ->
                            validatable.validate();
                        case null, default -> {

                        }
                    }

                }

                catch (DataException de) {
                    throw de.prepend(field.path());
                }

                catch (Exception e) {
                    throw new DataException(DataException.Phase.READING, field.path(), e);
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

    public interface Field<E> {

        String path();

        SerializableDataType<E> dataType();

        <I> E decode(DynamicOps<I> ops, I input);

        <I> I encode(DynamicOps<I> ops, E input);

        E getDefault(SerializableData.Instance data);

        boolean hasDefault();

    }

    public record FieldImpl<E>(String path, SerializableDataType<E> dataType) implements Field<E> {

        @Override
        public <I> E decode(DynamicOps<I> ops, I input) {
            return dataType().read(ops, input);
        }

        @Override
        public <I> I encode(DynamicOps<I> ops, E input) {
            return dataType().write(ops, input);
        }

        @Override
        public E getDefault(Instance data) {
            throw new IllegalStateException("Tried getting default value of field \"" + path + "\", which doesn't and cannot have any!");
        }

        @Override
        public boolean hasDefault() {
            return false;
        }

    }

    public record FunctionedFieldImpl<E>(String path, SerializableDataType<E> dataType, Function<Instance, E> defaultFunction) implements Field<E> {

        @Override
        public <I> E decode(DynamicOps<I> ops, I input) {
            return dataType().read(ops, input);
        }

        @Override
        public <I> I encode(DynamicOps<I> ops, E input) {
            return dataType().write(ops, input);
        }

        @Override
        public E getDefault(Instance data) {
            return defaultFunction().apply(data);
        }

        @Override
        public boolean hasDefault() {
            return true;
        }

    }

    public record OptionalFieldImpl<E>(String path, SerializableDataType<E> dataType, Supplier<E> defaultSupplier) implements Field<E> {

        @Override
        public <I> E decode(DynamicOps<I> ops, I input) {
            return dataType().read(ops, input);
        }

        @Override
        public <I> I encode(DynamicOps<I> ops, E input) {
            return dataType().write(ops, input);
        }

        @Override
        public E getDefault(Instance data) {
            return defaultSupplier().get();
        }

        @Override
        public boolean hasDefault() {
            return true;
        }

    }

}
