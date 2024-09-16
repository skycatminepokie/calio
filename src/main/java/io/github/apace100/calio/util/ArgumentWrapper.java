package io.github.apace100.calio.util;

public record ArgumentWrapper<T>(T argument, String input) {

    @Deprecated(forRemoval = true)
    public T get() {
        return argument;
    }

    @Deprecated(forRemoval = true)
    public String rawArgument() {
        return input;
    }

}
