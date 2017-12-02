package de.acepe.fritzstreams.util;

import java.util.stream.Collectors;

import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.input.*;
import javafx.util.StringConverter;

public final class ListUtils {

    /**
     * Install the keyboard handler: CTRL + C = copy to clipboard
     */
    public static <T> void installCopyPasteHandler(ListView<T> table, StringConverter<T> converter) {
        table.setOnKeyPressed(new KeyEventHandler<>(converter));
    }

    /**
     * Copy/Paste keyboard event handler. The handler uses the keyEvent's source for the clipboard data. The source must
     * be of type TableView.
     */
    public static class KeyEventHandler<T> implements EventHandler<KeyEvent> {
        private final StringConverter<T> converter;
        KeyCodeCombination copyKeyCodeCompination = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);

        public KeyEventHandler(StringConverter<T> converter) {
            this.converter = converter;
        }

        public void handle(KeyEvent keyEvent) {
            if (!copyKeyCodeCompination.match(keyEvent)) {
                return;
            }
            if (keyEvent.getSource() instanceof ListView) {
                // noinspection unchecked
                copySelectionToClipboard((ListView<T>) keyEvent.getSource(), converter);
                keyEvent.consume();
            }
        }
    }

    /**
     * Get table selection and copy it to the clipboard.
     */
    public static <T> void copySelectionToClipboard(ListView<T> list, StringConverter<T> converter) {

        String text = list.getSelectionModel()
                          .getSelectedItems()
                          .stream()
                          .map(converter::toString)
                          .collect(Collectors.joining("\n"));

        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(text);
        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }

}