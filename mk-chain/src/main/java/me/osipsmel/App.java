package me.osipsmel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import me.osipsmel.mediator.Mediator;
import me.osipsmel.app_storage.AppStorage;
import me.osipsmel.generator.Generator;
import me.osipsmel.ui.Ui;

public class App {
    public static void main(String[] args) {
        // 1. По красоте ставим темную тему (если скачал FlatLaf)
        // Если либы нет, Java просто проигнорирует этот блок
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (Exception e) {
            System.out.println("FlatLaf не найден, будет стандартный (уродливый) UI");
        }

        // 2. Запускаем приложение в потоке обработки событий Swing
        SwingUtilities.invokeLater(() -> {
            Mediator mediator = new Mediator(Generator::new, AppStorage::new, Ui::new);
            
            // Показываем окно
            mediator.show();
        });
    }
}
