package xyz.funforge.scratchypaws.hellfrog.common;

public class InMemoryAttach {

    private final String fileName;
    private final byte[] bytes;

    public InMemoryAttach(String fileName, byte[] bytes) {
        this.fileName = fileName;
        this.bytes = bytes;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
