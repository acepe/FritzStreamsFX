package de.acepe.fritzstreams.backend.json;

public class OnAirData {

    public class ImageData {

        private String alt;
        private String lnk;

        public ImageData() {

        }

        public String getLnk() {
            return lnk;
        }

        public void setLnk(String lnk) {
            this.lnk = lnk;
        }

        public String getAlt() {
            return alt;
        }

        public void setAlt(String alt) {
            this.alt = alt;
        }
    }

    private String t;
    private long id;
    private String type;
    private String title;
    private String artist;
    private String lnk;
    private ImageData img;

    public OnAirData() {
    }

    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getLnk() {
        return lnk;
    }

    public void setLnk(String lnk) {
        this.lnk = lnk;
    }

    public ImageData getImg() {
        return img;
    }

    public void setImg(ImageData img) {
        this.img = img;
    }
}
