package de.acepe.fritzstreams;

public enum Fragments {
    STREAM("ui/stream_view.fxml"), PLAYER("ui/player_controls.fxml");

    private final String resource;

    Fragments(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
