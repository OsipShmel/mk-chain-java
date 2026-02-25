package me.osipsmel.api;

import java.util.*;
import me.osipsmel.api.GenRequest;

public interface MediatorAPI {
    void startGeneration(GenRequest req, int len, int depth);
    List<String> getFilesList();
    void importNewFile(java.io.File file);
    void loadHistoryEntry(int hist_ind);
}


