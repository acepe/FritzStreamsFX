package de.acepe.fritzstreams.app;

public enum Fragments {
    STREAM("ui/stream_view.fxml"),
    PLAYER("ui/player_controls.fxml"),
    AUDIO_ITEM("ui/audio_item.fxml"),
    DOWNLOAD_ITEM("ui/download_item.fxml");

    private final String resource;

    Fragments(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
