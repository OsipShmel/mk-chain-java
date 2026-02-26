package me.osipsmel.mediator;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.JFrame;


import me.osipsmel.api.GenRequest;
import me.osipsmel.api.MediatorAPI;
import me.osipsmel.api.ViewAPI;
import me.osipsmel.api.GeneratorAPI;
import me.osipsmel.api.StorageAPI;

import me.osipsmel.app_storage.StorageException;


public class Mediator implements MediatorAPI{
    private final ViewAPI view;
    private final GeneratorAPI generator;
    private final StorageAPI storage;

    public Mediator(Supplier<GeneratorAPI> generatorFactory, 
        Supplier<StorageAPI> storageFactory, 
        Function<MediatorAPI, ViewAPI> viewFactory) {
        this.generator = generatorFactory.get();
        this.storage = storageFactory.get();
        this.view = viewFactory.apply(this);
    }
    @Override
    public void show() {
        ((JFrame)view).setVisible(true);
    }

    @Override
    public List<String> getFilesList() {
        try{
            return storage.getAvailableFiles();
        } catch(IOException e){
            e.printStackTrace();

            view.alertError("Не удалось прочитать список файлов: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    @Override
    public void importNewFile(java.io.File file){
        try {
            String savedName = storage.importFile(file);
            view.updateStatus("Файл " + savedName + " успешно импортирован");
        } catch (StorageException e) {
            e.printStackTrace();
            view.alertError(e.getMessage());
        }
    }

    @Override
    public void startGeneration(GenRequest req, int len, int depth){
        new Thread(()-> {
            try{
                String corpus = req.resolveContent((StorageAPI) storage);


                if(corpus==null || corpus.isBlank()){
                    throw new IllegalArgumentException("Источник текста пуст!");
                }
                view.clearOutput();

                var item = generator.generate(corpus, null, len, depth);
                StringBuilder result = new StringBuilder();
                while(item.hasNext()){
                    String word = item.next();
                    view.showToken(word);
                    result.append(word);
                    try { Thread.sleep(8); } catch (InterruptedException e) { break; }
                }

                storage.saveHistory(req.data(), result.toString());
                

            }
            catch (StorageException | IOException e) {
                view.alertError("Ошибка доступа к данным: " + e.getMessage());
            } catch (Exception e) {
                view.alertError("Критическая ошибка: " + e.getMessage());
            }

        }).start();
    }
    @Override
    public void loadHistoryEntry(int hist_ind){
        String entry = storage.getHistoryEntry(hist_ind);
        if (entry != null) {
            String[] parts = entry.split("\\|");
            if (parts.length >= 2) {
                view.clearOutput();
                view.showToken(parts[1]); // [1] — это Result
                view.updateStatus(parts[0]); // [0] — это Prompt
            }
        } else {
            view.updateStatus("История на этом индексе пуста");
        }
    }
}
