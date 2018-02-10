package de.acepe.fritzstreams.app;

public enum Fragments {
    ONDEMAND_STREAM("ui/stream_fragment.fxml"), PLAYER("ui/player_controls.fxml"), LIVE_STREAM("ui/live_fragment.fxml");

    private final String resource;

    Fragments(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
