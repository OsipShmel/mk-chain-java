package me.osipsmel.api;

import java.util.*;

// АПИ медиатора, тст те запросы, которые могут делать другие компоненты в медиатор.
public interface MediatorAPI {
    void startGeneration(GenRequest req, int len, int depth); // Запрос на генерацию текста. GenRequest – промпт(тот самый enum с текстом и типом источника)
    List<String> getFilesList();       // Получить от storage список доступных файлов для генерации(в данной реализации запрос возвращает буквально список, в питоновской реализации он вроде отдает словарь. Тебя это в прочем ебать не должно, тк работает с данными ui, а не ты)
    void importNewFile(java.io.File file);  // импорт файла, нихуя себе правда?
    void loadHistoryEntry(int hist_ind);    // получить от storage историю генерации
    void show();    // показать окно приложения, это джава момент – он используется в main. Не шарю, нужно ли такое в питоне. Сама разберешься.
}


