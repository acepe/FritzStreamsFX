package de.acepe.fritzstreams;

public enum ScreenId {
    STREAMS("ui/streams.fxml"), SETTINGS("ui/settings.fxml");

    private String resource;

    ScreenId(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
