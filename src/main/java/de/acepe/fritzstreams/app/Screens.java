package de.acepe.fritzstreams.app;

public enum Screens {
    MAIN("ui/main_view.fxml", "Musikstreams", 630, 538),
    SETTINGS("ui/settings.fxml", "Einstellungen", 630, 530),
    PLAYLIST("ui/playlist.fxml", "Playlist", 550, 530);

    private final String resource;
    private final String title;
    private final int width;
    private final int height;

    Screens(String resource, String title, int width, int height) {
        this.resource = resource;
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public String getResource() {
        return resource;
    }

    public String getTitle() {
        return title;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
