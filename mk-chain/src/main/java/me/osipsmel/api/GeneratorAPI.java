package me.osipsmel.api;
import java.util.*;

public interface GeneratorAPI {
    Iterator<String> generate(String source, String start, int length, int depth);
}
