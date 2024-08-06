package io.github.apace100.calio.serialization;

import com.mojang.serialization.*;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.data.DataException;

import java.util.Objects;
import java.util.stream.Stream;

public class StrictFieldDecoder<A> extends StrictMapDecoder.Implementation<A> {

    private final String name;
    private final StrictDecoder<A> elementCodec;

    public StrictFieldDecoder(String name, StrictDecoder<A> elementCodec) {
        this.name = name;
        this.elementCodec = elementCodec;
    }

    @Override
    public <T> A strictDecode(DynamicOps<T> ops, MapLike<T> input) {

        T value = input.get(name);

        if (value == null) {
            throw Calio.createMissingRequiredFieldError(name);
        }

        else {
            return elementCodec.strictParse(ops, value);
        }

    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.of(ops.createString(name));
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        else if (obj instanceof StrictFieldDecoder<?> that) {
            return this.name.equals(that.name)
                && this.elementCodec.equals(that.elementCodec);
        }

        else {
            return false;
        }

    }

    @Override
    public int hashCode() {
        return Objects.hash(name, elementCodec);
    }

    @Override
    public String toString() {
        return "StrictFieldDecoder[" + name + ": " + elementCodec + "]";
    }
}
