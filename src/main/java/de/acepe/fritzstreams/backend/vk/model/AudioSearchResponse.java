package de.acepe.fritzstreams.backend.vk.model;

import java.util.List;

public class AudioSearchResponse {

    private int count;
    private List<AudioItem> items;

    public List<AudioItem> getItems() {
        return items;
    }

    public void setItems(List<AudioItem> items) {
        this.items = items;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
