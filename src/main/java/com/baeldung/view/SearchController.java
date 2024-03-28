package com.baeldung.view;

import com.baeldung.model.Emulator;
import com.baeldung.service.ProcessService;
import com.baeldung.view.listview.ActionButtonCellFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

public class SearchController {
    public static String authToken = null;
    private final ProcessService processService;
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

    public SearchController() {
        this.processService = ProcessService.getInstance();
    }

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
        authToken = processService.getEmulatorConsoleAuthToken();
        System.out.println("Auth token: " + authToken);
        List<Emulator> emulators = processService.getEmulatorsList();
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
        action.setCellFactory(new ActionButtonCellFactory());
        tableView.getColumns().addAll(id, name, employed, action);

        dataContainer.getChildren().add(tableView);
    }


    private void loadData() {
        List<Emulator> emulators = processService.getEmulatorsList();
        // compare emulators to masterData, add any non matching name with isRunning = false and port incremented by 2 from the last port
        emulators.stream().filter(value -> masterData.stream().noneMatch(value1 -> value1.getName().equals(value.getName()))).forEach(value -> {
            value.setIsRunning(false);
            value.setId(masterData.get(masterData.size() - 1).getId() + 2);
            masterData.add(value);
        });

        String searchText = searchField.getText();

        Task<ObservableList<Emulator>> task = new Task<ObservableList<Emulator>>() {
            @Override
            protected ObservableList<Emulator> call() {
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
