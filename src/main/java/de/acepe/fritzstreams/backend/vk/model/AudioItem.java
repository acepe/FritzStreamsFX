package de.acepe.fritzstreams.backend.vk.model;

import com.google.common.base.MoreObjects;

public class AudioItem {

    private long id; // positive number
    private int owner_id;
    private String artist;
    private String title;
    private int duration; // Duration (in seconds). positive number
    private String url; // Link to mp3.

    private String lyrics_id; // ID of the lyrics (if available) of the audio file.
    private String album_id;	// ID of the album containing the audio file (if assigned). positive number
    private String genre_id; // Genre ID. See the list of audio genres. positive number

    public AudioItem() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getOwner_id() {
        return owner_id;
    }

    public void setOwner_id(int owner_id) {
        this.owner_id = owner_id;
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

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLyrics_id() {
        return lyrics_id;
    }

    public void setLyrics_id(String lyrics_id) {
        this.lyrics_id = lyrics_id;
    }

    public String getAlbum_id() {
        return album_id;
    }

    public void setAlbum_id(String album_id) {
        this.album_id = album_id;
    }

    public String getGenre_id() {
        return genre_id;
    }

    public void setGenre_id(String genre_id) {
        this.genre_id = genre_id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("owner_id", owner_id)
                .add("artist", artist)
                .add("title", title)
                .add("duration", duration)
                .add("url", url)
                .add("lyrics_id", lyrics_id)
                .add("album_id", album_id)
                .add("genre_id", genre_id)
                .toString();
    }
}
