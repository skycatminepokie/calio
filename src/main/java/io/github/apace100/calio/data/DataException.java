package io.github.apace100.calio.data;

public class DataException extends RuntimeException {

    private final Phase phase;
    private final Type type;

    private final String path;
    private final String exceptionMessage;

    public DataException(Phase phase, Type type, String path, String exceptionMessage) {
        super("Error " + phase + " data field");
        this.phase = phase;
        this.type = type;
        this.path = path;
        this.exceptionMessage = exceptionMessage;
    }

    public DataException(Phase phase, String path, String exceptionMessage) {
        this(phase, Type.OBJECT, path, exceptionMessage);
    }

    public DataException(Phase phase, String path, Exception exception) {
        this(phase, Type.OBJECT, path, exception.getMessage());
    }

    public DataException prependArray(int index) {
        String processedPath = "[" + index + "]" + (this.path.isEmpty() ? "" : ".") + this.path;
        return new DataException(this.phase, Type.ARRAY, processedPath, this.exceptionMessage);
    }

    public DataException prepend(String path) {
        String processedPath = path + (this.type == Type.ARRAY || this.path.isEmpty() ? "" : ".") + this.path;
        return new DataException(this.phase, Type.OBJECT, processedPath, this.exceptionMessage);
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " at " + path + ": " + exceptionMessage;
    }

    public enum Type {
        OBJECT,
        ARRAY
    }

    public enum Phase {

        READING("decoding"),
        WRITING("encoding"),
        SENDING("sending"),
        RECEIVING("receiving");

        final String name;

        Phase(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }

}
