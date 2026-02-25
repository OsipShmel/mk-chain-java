package me.osipsmel.ui;

import me.osipsmel.api.MediatorAPI;
import me.osipsmel.api.GenRequest;
import me.osipsmel.api.SourceType;
import me.osipsmel.api.ViewAPI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class Ui extends JFrame implements ViewAPI {
    private final MediatorAPI mediator;

    // Состояние текущего выбора
    private String selectedFileName = null;
    private SourceType currentSource = SourceType.RAW_TEXT;

    private int historyIndex = -1; 

    private JTextArea inputArea = new JTextArea("Введи текст или выбери файл...");
    private JTextArea outputArea = new JTextArea();
    private JSpinner depthSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 8, 1));
    private JSpinner lengthSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 5000, 50));
    private JButton loadBtn = new JButton("Импорт файла");
    private JButton generateBtn = new JButton("Сгенерировать");
    private JLabel fileLabel = new JLabel("Источник: Ручной ввод");

    public Ui(MediatorAPI mediator) {
        this.mediator = mediator;

        setTitle("Markov Chain Lab");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- ВЕРХНЯЯ ПАНЕЛЬ ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        topPanel.add(new JLabel("Глубина:"));
        topPanel.add(depthSpinner);
        topPanel.add(new JLabel("Длина:"));
        topPanel.add(lengthSpinner);
        topPanel.add(loadBtn);
        topPanel.add(fileLabel);
        
        // --- ЦЕНТРАЛЬНАЯ ПАНЕЛЬ ---
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        
        JPanel inputWrapper = new JPanel(new BorderLayout());
        inputWrapper.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        
        JButton fileListBtn = new JButton("◢"); 
        fileListBtn.setToolTipText("Список файлов приложения");
        JPanel southInput = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        southInput.add(fileListBtn);
        inputWrapper.add(southInput, BorderLayout.SOUTH);

        outputArea.setEditable(false);
        outputArea.setBackground(new Color(30, 30, 30));
        outputArea.setForeground(Color.CYAN);

        centerPanel.add(inputWrapper);
        centerPanel.add(new JScrollPane(outputArea));
        outputArea.setLineWrap(true);       // Включает перенос строк
        outputArea.setWrapStyleWord(true); 


        // --- БЛОК ИСТОРИИ (Стрелочки) ---
        JPanel historyBox = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton prevBtn = new JButton("❮");
        JButton nextBtn = new JButton("❯");
        prevBtn.setToolTipText("Старая запись");
        nextBtn.setToolTipText("Новая запись");
        
        historyBox.add(new JLabel("История:"));
        historyBox.add(prevBtn);
        historyBox.add(nextBtn);
        topPanel.add(historyBox);

        // --- ЛОГИКА ---

        // Импорт файла в AppStorage
        loadBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                mediator.importNewFile(chooser.getSelectedFile());
                updateStatus("Файл добавлен в базу");
            }
        });

        // "Ушко" — выбор файла из базы
        fileListBtn.addActionListener(e -> {
            List<String> files = mediator.getFilesList();
            if (files.isEmpty()) {
                alertError("База файлов пуста!");
                return;
            }

            String selected = (String) JOptionPane.showInputDialog(this, 
                    "Выберите файл для генерации:", "StorageLayer", 
                    JOptionPane.PLAIN_MESSAGE, null, files.toArray(), files.get(0));

            if (selected != null) {
                this.selectedFileName = selected;
                this.currentSource = SourceType.FILE;
                fileLabel.setText("Источник: [FILE] " + selected);
                
                // Визуально блокируем поле ввода, чтобы было ясно — юзаем файл
                inputArea.setEnabled(false);
                inputArea.setText("--- Текст будет взят из файла: " + selected + " ---");
            }
        });

        // Сброс на ручной ввод при клике по текстовому полю
        inputArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!inputArea.isEnabled()) {
                    inputArea.setEnabled(true);
                    inputArea.setText("");
                    currentSource = SourceType.RAW_TEXT;
                    selectedFileName = null;
                    fileLabel.setText("Источник: Ручной ввод");
                }
            }
        });

        // Запуск генерации через упаковку в GenRequest
        generateBtn.addActionListener(e -> {
            outputArea.setText(""); 
            historyIndex = -1; // Сброс: при нажатии "Назад" индекс станет 0
            
            String data = (currentSource == SourceType.FILE) ? selectedFileName : inputArea.getText();
            GenRequest request = new GenRequest(data, currentSource);
            
            mediator.startGeneration(request, (int) lengthSpinner.getValue(), (int) depthSpinner.getValue());
        });

        prevBtn.addActionListener(e -> {
            if (historyIndex < 2) { 
                historyIndex++;
                mediator.loadHistoryEntry(historyIndex);
            } else {
                updateStatus("Дальше истории нет (лимит 3)");
            }
        });

        nextBtn.addActionListener(e -> {
            if (historyIndex > 0) {
                historyIndex--;
                mediator.loadHistoryEntry(historyIndex);
            } else if (historyIndex == 0) {
                updateStatus("Это самая свежая запись");
            }
        });

        root.add(topPanel, BorderLayout.NORTH);
        root.add(centerPanel, BorderLayout.CENTER);
        root.add(generateBtn, BorderLayout.SOUTH);

        add(root);
        pack();
        setLocationRelativeTo(null);
    }

    // --- РЕАЛИЗАЦИЯ ViewAPI ---

    @Override
    public void showToken(String token) {
        // Обязательно в UI потоке
        SwingUtilities.invokeLater(() -> outputArea.append(token));
    }

    @Override
    public void updateStatus(String msg) {
        SwingUtilities.invokeLater(() -> fileLabel.setText(msg));
    }

    @Override
    public void alertError(String error) {
        JOptionPane.showMessageDialog(this, error, "Ошибка системы", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void clearOutput(){
        SwingUtilities.invokeLater(() -> outputArea.setText(""));
    }
}
