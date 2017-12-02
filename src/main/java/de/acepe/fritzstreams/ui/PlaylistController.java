package de.acepe.fritzstreams.ui;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.UrlEscapers;

import de.acepe.fritzstreams.app.ControlledScreen;
import de.acepe.fritzstreams.backend.PlayListEntry;
import de.acepe.fritzstreams.backend.Playlist;
import de.acepe.fritzstreams.util.FileUtil;
import de.acepe.fritzstreams.util.ListUtils;
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
    private static final String COPY_ENTRY = "Eintrag kopieren";
    private static final String COPY_LIST = "Playlist kopieren";
    private static final Logger LOG = LoggerFactory.getLogger(PlaylistController.class);

    private final ObservableList<PlayListEntry> entriesList = FXCollections.observableArrayList();

    @FXML
    private ListView<PlayListEntry> entriesListView;
    @FXML
    private Label titleLabel;

    @Inject
    public PlaylistController() {
    }

    @FXML
    private void initialize() {
        entriesListView.setItems(entriesList);
        entriesListView.setCellFactory(param -> {
            TextFieldListCell<PlayListEntry> cell = new TextFieldListCell<>(entryConverter);

            ObjectProperty<PlayListEntry> itemProperty = cell.itemProperty();
            StringProperty itemtextProperty = new SimpleStringProperty();
            itemtextProperty.bindBidirectional(itemProperty, entryConverter);

            MenuItem copyItem = new MenuItem(COPY_ENTRY);
            copyItem.setOnAction(event -> {
                ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(entryConverter.toString(cell.getItem()));
                Clipboard.getSystemClipboard().setContent(clipboardContent);
            });
            MenuItem copyList = new MenuItem(COPY_LIST);
            copyList.setOnAction(event -> {
                ClipboardContent clipboardContent = new ClipboardContent();
                String list = entriesList.stream().map(entryConverter::toString).collect(Collectors.joining("\n"));
                clipboardContent.putString(list);
                Clipboard.getSystemClipboard().setContent(clipboardContent);
            });

            MenuItem googleItem = new MenuItem();
            googleItem.textProperty().bind(Bindings.format(GOOGLE_ITEM_TEMPLATE, itemtextProperty));
            googleItem.setOnAction(event -> searchTrack(GOOGLE_TEMPLATE, cell.itemProperty()));

            MenuItem vkItem = new MenuItem();
            vkItem.textProperty().bind(Bindings.format(VK_ITEM_TEMPLATE, itemtextProperty));
            vkItem.setOnAction(event -> searchTrack(VK_TEMPLATE, cell.itemProperty()));

            ContextMenu contextMenu = new ContextMenu();
            contextMenu.getItems().addAll(copyItem, copyList, googleItem, vkItem);
            cell.emptyProperty()
                .addListener((obs, wasEmpty, isNowEmpty) -> cell.setContextMenu(isNowEmpty ? null : contextMenu));
            return cell;
        });
        ListUtils.installCopyPasteHandler(entriesListView, entryConverter);
    }

    private void searchTrack(String template, ObjectProperty<PlayListEntry> itemProperty) {
        String searchUri = String.format(template,
                                         UrlEscapers.urlFormParameterEscaper().escape(itemToString(itemProperty)));
        try {
            FileUtil.doOpen(new URI(searchUri));
        } catch (URISyntaxException e) {
            LOG.error("Error opening URL: ", e);
        }
    }

    private String itemToString(ObjectProperty<PlayListEntry> itemProperty) {
        return entryConverter.toString(itemProperty.get());
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
