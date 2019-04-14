package de.acepe.fritzstreams.backend;

import javafx.scene.image.Image;

public class StreamMetaData {
    private final String title;
    private final String subtitle;
    private final String onAirArtist;
    private final String onAirTitle;
    private final Image image;
    private final Image onAirImage;

    public StreamMetaData(String title, String subtitle, Image image, Image onAirImage, String onAirArtist, String onAirTitle) {
        this.title = title;
        this.subtitle = subtitle;
        this.image = image;
        this.onAirImage = onAirImage;
        this.onAirArtist = onAirArtist;
        this.onAirTitle = onAirTitle;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public Image getImage() {
        return image;
    }

    public String getOnAirArtist() {
        return onAirArtist;
    }

    public String getOnAirTitle() {
        return onAirTitle;
    }

    public Image getOnAirImage() {
        return onAirImage;
    }
}
