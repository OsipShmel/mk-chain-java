package me.osipsmel.api;

public record GenRequest(String data, SourceType type) {
    public String resolveContent(StorageAPI resolver) throws Exception {
        return switch (type) {
            case FILE -> resolver.getFileContent(data);
            case RAW_TEXT -> data;
        };
    }
}
