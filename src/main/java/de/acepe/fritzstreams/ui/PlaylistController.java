package de.acepe.fritzstreams.ui;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;

import de.acepe.fritzstreams.ControlledScreen;
import de.acepe.fritzstreams.ListUtils;
import de.acepe.fritzstreams.ScreenManager;
import de.acepe.fritzstreams.backend.PlayListEntry;
import de.acepe.fritzstreams.backend.Playlist;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.StringConverter;

public class PlaylistController implements ControlledScreen {

    private static final String GOOGLE_TEMPLATE = "https://www.google.de/search?q=%s";
    private static final String VK_TEMPLATE = "https://vk.com/audio?q=%s";
    private static final String GOOGLE_ITEM_TEMPLATE = "Google nach \"%s\" durchsuchen";
    private static final String VK_ITEM_TEMPLATE = "VKontakte nach \"%s\" durchsuchen";
    private static final String COPY = "Kopieren";

    private final ObservableList<PlayListEntry> entriesList = FXCollections.observableArrayList();

    @FXML
    private ListView<PlayListEntry> entriesListView;
    @FXML
    private Label titleLabel;
    private ScreenManager screenManager;

    @FXML
    private void initialize() {
        entriesListView.setItems(entriesList);
        entriesListView.setCellFactory(param -> {
            TextFieldListCell<PlayListEntry> cell = new TextFieldListCell<>(entryConverter);

            ObjectProperty<PlayListEntry> itemProperty = cell.itemProperty();
            StringProperty itemtextProperty = new SimpleStringProperty();
            itemtextProperty.bindBidirectional(itemProperty, entryConverter);

            MenuItem copyItem = new MenuItem(COPY);
            copyItem.setOnAction(event -> {
                ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(entryConverter.toString(cell.getItem()));
                Clipboard.getSystemClipboard().setContent(clipboardContent);
            });

            MenuItem googleItem = new MenuItem();
            googleItem.textProperty().bind(Bindings.format(GOOGLE_ITEM_TEMPLATE, itemtextProperty));
            googleItem.setOnAction(event -> searchTrack(GOOGLE_TEMPLATE, cell.itemProperty()));

            MenuItem vkItem = new MenuItem();
            vkItem.textProperty().bind(Bindings.format(VK_ITEM_TEMPLATE, itemtextProperty));
            vkItem.setOnAction(event -> searchTrack(VK_TEMPLATE, cell.itemProperty()));

            ContextMenu contextMenu = new ContextMenu();
            contextMenu.getItems().addAll(copyItem, googleItem, vkItem);
            cell.emptyProperty()
                .addListener((obs, wasEmpty, isNowEmpty) -> cell.setContextMenu(isNowEmpty ? null : contextMenu));
            return cell;
        });
        ListUtils.installCopyPasteHandler(entriesListView, entryConverter);
    }

    private void searchTrack(String template, ObjectProperty<PlayListEntry> itemProperty) {
        String searchUri = String.format(template, itemToString(itemProperty));
        HostServicesFactory.getInstance(screenManager.getApplication()).showDocument(searchUri);
    }

    private String itemToString(ObjectProperty<PlayListEntry> itemProperty) {
        return entryConverter.toString(itemProperty.get());
    }

    @Override
    public void setScreenManager(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }

    public void setPlayList(Playlist playlist) {
        entriesList.addAll(playlist.getEntries());
        titleLabel.setText(playlist.getTitle());
    }

    private final StringConverter<PlayListEntry> entryConverter = new StringConverter<PlayListEntry>() {
        @Override
        public String toString(PlayListEntry entry) {
            if (entry == null) {
                return "";
            }
            return entry.getArtist() + " - " + entry.getTitle();
        }

        @Override
        public PlayListEntry fromString(String string) {
            return null;
        }
    };
}
