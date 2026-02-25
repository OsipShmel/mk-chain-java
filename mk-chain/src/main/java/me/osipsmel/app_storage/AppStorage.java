package me.osipsmel.app_storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class AppStorage {
    private final Path rootPath = Paths.get("app_data");
    private final Path storagePath = rootPath.resolve("storage");
    private final Path historyFile = rootPath.resolve("history.log"); // Проще хранить историю в логе

    public AppStorage() {
        try {
            Files.createDirectories(storagePath);
            if (!Files.exists(historyFile)) Files.createFile(historyFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 1. Копируем файл юзера к себе
    public String importFile(File source) {
        if (source == null || !source.exists()) {
            throw new StorageException("Файл не найден на диске", null);
        }
        if (!source.canRead()) {
            throw new StorageException("Нет прав на чтение файла: " + source.getName(), null);
        }

        String cleanName = source.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        Path target = storagePath.resolve(cleanName);

        try {
            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return cleanName;
        } catch (NoSuchFileException e) {
            throw new StorageException("Исходный файл внезапно исчез при копировании", e);
        } catch (AccessDeniedException e) {
            throw new StorageException("Приложению запрещено писать в папку storage", e);
        } catch (IOException e) {
            // Все остальные системные ошибки (диск полон и т.д.)
            throw new StorageException("Системная ошибка при импорте файла: " + e.getMessage(), e);
        }
    }

    // 2. Получаем список всех импортов (просто смотрим, что лежит в папке)
    public List<String> getAvailableFiles() throws IOException {
        try (var s = Files.list(storagePath)) {
            return s.map(p -> p.getFileName().toString()).collect(Collectors.toList());
        }
    }

    // 3. Работа с историей (Last 3)
    // Храним в формате: Prompt|Result (одна строка - одна запись)
    public void saveHistory(String prompt, String result) {
        try {
            // Убираем переносы строк из промпта и резалта для однострочного хранения
            String entry = prompt.replace("\n", " ") + "|" + result.replace("\n", " ") + "\n";
            List<String> history = Files.readAllLines(historyFile);
            
            history.add(0, entry); // Добавляем в начало
            
            // Ограничиваем тремя записями
            List<String> limited = history.stream().limit(3).collect(Collectors.toList());
            Files.write(historyFile, limited);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Чтение конкретного файла для генератора
    public String getFileContent(String fileName) throws IOException {
        return Files.readString(storagePath.resolve(fileName));
    }

    public String getHistoryEntry(int index) {
    try {
        List<String> history = Files.readAllLines(historyFile);
        if (index >= 0 && index < history.size()) {
            return history.get(index); // Возвращает строку вида "Prompt|Result"
        }
    } catch (IOException e) {
        throw new StorageException("Не удалось прочитать историю генераций", e);
    }
    return null; 
}
}