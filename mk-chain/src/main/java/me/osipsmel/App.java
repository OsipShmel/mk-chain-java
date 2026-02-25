package me.osipsmel;
import me.osipsmel.mediator.Mediator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

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
            // Создаем медиатор (он создаст Storage, Generator и UI внутри себя)
            Mediator mediator = new Mediator();
            
            // Показываем окно
            mediator.show();
        });
    }
}
