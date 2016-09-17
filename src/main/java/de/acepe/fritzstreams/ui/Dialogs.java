package de.acepe.fritzstreams.ui;

import static javafx.scene.layout.GridPane.setHgrow;
import static javafx.scene.layout.VBox.setVgrow;

import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public class Dialogs {

    private Dialogs() {
    }

    public static void showErrorDialog(Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        String title = "Download fehlgeschlagen";
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(title + ". Grund: " + ex.getMessage());
        alert.setResizable(true);
        alert.getDialogPane().setPrefWidth(800);

        Label label = new Label("Fehlermeldung:");
        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        setVgrow(textArea, Priority.ALWAYS);
        setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        // workaround for https://bugs.openjdk.java.net/browse/JDK-8095777
        // dialog doesn't resize if expandable is collapsed
        alert.getDialogPane().expandedProperty().addListener((l) -> {
            Platform.runLater(() -> {
                alert.getDialogPane().requestLayout();
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                stage.sizeToScene();
            });
        });
        alert.showAndWait();
    }
}
