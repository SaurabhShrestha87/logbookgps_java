package com.baeldung.model;

import javafx.beans.property.*;

public class Emulator {

    private SimpleIntegerProperty id;
    private SimpleStringProperty name;
    private SimpleBooleanProperty isRunning;

    public Emulator(Integer id, String name, boolean isRunning) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.isRunning = new SimpleBooleanProperty(isRunning);
    }

    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public boolean getIsRunning() {
        return isRunning.get();
    }

    public BooleanProperty isRunningProperty() {
        return isRunning;
    }

    public void setIsRunning(boolean isRunning) {
        this.isRunning.set(isRunning);
    }

}
