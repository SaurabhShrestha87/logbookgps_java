package com.baeldung.view.listview;

import com.baeldung.model.Emulator;
import com.baeldung.service.ProcessService;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class ActionButtonCellFactory implements Callback<TableColumn<Emulator, Boolean>, TableCell<Emulator, Boolean>> {
    private final ProcessService processService;

    public ActionButtonCellFactory() {
        this.processService = ProcessService.getInstance();
    }

    @Override
    public TableCell<Emulator, Boolean> call(TableColumn<Emulator, Boolean> param) {
        return new TableCell<>() {
            final Button button = new Button();

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // text
                    button.setText(item ? "Stop" : "Start");
                    //color
                    button.setStyle(item ? "-fx-background-color: red; -fx-text-fill: white;" : "-fx-background-color: green; -fx-text-fill: white;");
                    button.setOnAction(event -> {
                        Boolean isRunning = getTableView().getItems().get(getIndex()).getIsRunning();
                        Boolean success;
                        if (isRunning) {
                            success = processService.stopEmulator(getTableView().getItems().get(getIndex()));
                        } else {
                            success = processService.startEmulator(getTableView().getItems().get(getIndex()));
                        }
                        if (success) {
                            isRunning = !isRunning;
                        }
                        Emulator emulator = getTableView().getItems().get(getIndex());
                        emulator.setIsRunning(isRunning);
                        getTableView().refresh();
                    });
                    setGraphic(button);
                    setText(null);
                }
            }
        };
    }
}