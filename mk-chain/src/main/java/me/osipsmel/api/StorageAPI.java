package me.osipsmel.api;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface StorageAPI {
    String importFile(File source);
    List<String> getAvailableFiles() throws IOException;
    void saveHistory(String prompt, String result);
    String getFileContent(String fileName) throws IOException;
    String getHistoryEntry(int index);
}
