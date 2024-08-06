package io.github.apace100.calio.data;

public class DataException extends RuntimeException {

    private final Phase phase;

    private final String path;
    private final String exceptionMessage;

    public DataException(Phase phase, String path, String exceptionMessage) {
        super("Error " + phase + " data field");
        this.phase = phase;
        this.path = path;
        this.exceptionMessage = exceptionMessage;
    }

    public DataException(Phase phase, String path, Exception exception) {
        this(phase, path, exception.getMessage());
    }

    public DataException prepend(String path) {
        return new DataException(this.phase, path + "." + this.path, this.exceptionMessage);
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " at " + path + ": " + exceptionMessage;
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
