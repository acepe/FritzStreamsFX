package de.acepe.fritzstreams.backend;

public class PlayListEntry {

    private String artist;
    private String title;

    public PlayListEntry() {
    }

    public PlayListEntry(String artist, String title) {
        this.artist = artist;
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
