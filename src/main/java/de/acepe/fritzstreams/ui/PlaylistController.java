package de.acepe.fritzstreams.ui;

import de.acepe.fritzstreams.ControlledScreen;
import de.acepe.fritzstreams.ScreenManager;
import de.acepe.fritzstreams.backend.PlayListEntry;
import de.acepe.fritzstreams.backend.Playlist;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.util.StringConverter;

public class PlaylistController implements ControlledScreen {

    private final ObservableList<PlayListEntry> entriesList = FXCollections.observableArrayList();

    @FXML
    private ListView<PlayListEntry> entriesListView;
    @FXML
    private Label titleLabel;

    @FXML
    private void initialize() {
        entriesListView.setItems(entriesList);
        entriesListView.setCellFactory(param -> new TextFieldListCell<>(entryConverter));
    }

    @Override
    public void setScreenManager(ScreenManager screenManager) {
    }

    public void setPlayList(Playlist playlist) {
        entriesList.addAll(playlist.getEntries());
        titleLabel.setText(playlist.getTitle());
    }

    private final StringConverter<PlayListEntry> entryConverter = new StringConverter<PlayListEntry>() {
        @Override
        public String toString(PlayListEntry entry) {
            return entry.getArtist() + " - " + entry.getTitle();
        }

        @Override
        public PlayListEntry fromString(String string) {
            return null;
        }
    };
}
