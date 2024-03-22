package com.baeldung.view;

import com.baeldung.model.Emulator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
public class SearchController {

    private final ObservableList<Emulator> masterData = FXCollections.observableArrayList();
    @FXML
    private TextField searchField;
    @FXML
    private Button searchButton;
    @FXML
    private Label searchLabel;
    @FXML
    private TableView tableView;
    @FXML
    private VBox dataContainer;
    private ObservableList<Emulator> results = FXCollections.observableList(masterData);


    @FXML
    private void initialize() {
        // search panel
        searchButton.setText("Search");
        searchButton.setOnAction(event -> loadData());
        searchButton.setStyle("-fx-background-color: slateblue; -fx-text-fill: white;");

        searchField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                loadData();
            }
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchLabel.setText(newValue);
        });
        initData();
        initTable();

    }

    private void initData() {
        List<Emulator> emulators = extracted2();
        masterData.addAll(emulators);
    }

    private void initTable() {
        tableView = new TableView<>(FXCollections.observableList(masterData));
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn id = new TableColumn("ID");
        id.setCellValueFactory(new PropertyValueFactory("id"));
        TableColumn name = new TableColumn("NAME");
        name.setCellValueFactory(new PropertyValueFactory("name"));
        TableColumn employed = new TableColumn("ACTIVE");
        employed.setCellValueFactory(new PropertyValueFactory("isRunning"));
        TableColumn action = new TableColumn("ACTION");
        action.setCellValueFactory(new PropertyValueFactory("isRunning"));
        // add button to the last column, based on the value of the isRunning
        action.setCellFactory(param -> new TableCell<Emulator, Boolean>() {
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
                        Emulator emulator = getTableView().getItems().get(getIndex());
                        emulator.setIsRunning(!emulator.getIsRunning());
                        getTableView().refresh();
                    });
                    setGraphic(button);
                    setText(null);
                }
            }
        });
        tableView.getColumns().addAll(id, name, employed, action);

        dataContainer.getChildren().add(tableView);
    }

    private List<Emulator> extracted2() {
        List<Emulator> emulators = new ArrayList<>();
        try {
            // set environment variable ANDROID_SDK_ROOT to the path of the Android SDK
            ProcessBuilder builder = new ProcessBuilder("emulator", "-list-avds");
            Process process = builder.start();
            List<String> output = readOutput(process.getInputStream());
            output.forEach(System.out::println);
            for (String line : output) {
                emulators.add(new Emulator(0, line, false));
            }
        } catch (IOException e) {
            System.out.println("Error while executing command" + e.getMessage());
        }
        return emulators;
    }

    private void extracted() {
        try {
            List<ProcessBuilder> builders = Arrays.asList(new ProcessBuilder("emulator", " -list-avds"), new ProcessBuilder("wc", "-l"));
            List<Process> processes = ProcessBuilder.startPipeline(builders);
            Process first = processes.get(0);
            Process last = processes.get(processes.size() - 1);
            List<String> firstOutput = readOutput(first.getInputStream());
            System.out.println(firstOutput);
            List<String> output = readOutput(last.getInputStream());
            System.out.println(output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> readOutput(InputStream inputStream) throws IOException {
        // read the output from the input stream and save as a list of strings
        String output = new String(inputStream.readAllBytes());
        System.out.println(output);
        // split the output by new line and filter out lines that are empty or more than 10 characters
        return Arrays.stream(output.split("\n")).filter(line -> !line.isEmpty() && line.length() <= 10).collect(Collectors.toList());
    }

    private void loadData() {
        String searchText = searchField.getText();

        Task<ObservableList<Emulator>> task = new Task<ObservableList<Emulator>>() {
            @Override
            protected ObservableList<Emulator> call() throws Exception {
                updateMessage("Loading data");
                return FXCollections.observableArrayList(masterData.stream().filter(value -> value.getName().toLowerCase().contains(searchText)).collect(Collectors.toList()));
            }
        };

        task.setOnSucceeded(event -> {
            results = task.getValue();
            tableView.setItems(FXCollections.observableList(results));
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

}
