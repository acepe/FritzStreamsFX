package de.acepe.fritzstreams;

public enum ScreenId {
    STREAMS("streams.fxml"), SETTINGS("settings.fxml");

    private String resource;

    ScreenId(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
