package io.github.apace100.calio.data;

public class DataException extends RuntimeException {

    private final Phase phase;
    private final Type type;

    private final String path;
    private final String exceptionMessage;

    protected DataException(String baseMessage, Phase phase, Type type, String path, String exceptionMessage) {
        super(baseMessage);
        this.phase = phase;
        this.type = type;
        this.path = path;
        this.exceptionMessage = exceptionMessage;
    }

    public DataException(Phase phase, String path, String exceptionMessage) {
        this("Error " + phase + " data at field", phase, Type.OBJECT, path, exceptionMessage);
    }

    public DataException(Phase phase, String path, Exception exception) {
        this(phase, path, exception.getMessage());
    }

    public DataException(Phase phase, int index, String exceptionMessage) {
        this("Error " + phase + " element at index", phase, Type.ARRAY, "[" + index + "]", exceptionMessage);
    }

    public DataException(Phase phase, int index, Exception exception) {
        this(phase, index, exception.getMessage());
    }

    public DataException prependArray(int index) {
        String processedPath = "[" + index + "]" + (this.path.isEmpty() ? "" : ".") + this.path;
        return new DataException(super.getMessage(), this.phase, Type.ARRAY, processedPath, this.exceptionMessage);
    }

    public DataException prepend(String path) {
        String separator = this.type == Type.ARRAY || this.path.isEmpty() ? "" : ".";
        return new DataException(super.getMessage(), this.phase, Type.OBJECT, path + separator + this.path, this.exceptionMessage);
    }

    @Override
    public String getMessage() {
        return this.path.isEmpty()
            ? this.exceptionMessage
            : super.getMessage() + " " + this.path + ": " + this.exceptionMessage;
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
