package io.github.apace100.calio.registry;

import io.github.apace100.calio.data.SerializableData;

public interface DataObjectFactory<T> {

    SerializableData getSerializableData();
    T fromData(SerializableData.Instance instance);

    SerializableData.Instance toData(T t, SerializableData serializableData);
    default SerializableData.Instance toData(T t) {
        return toData(t, getSerializableData());
    }

}
