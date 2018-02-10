package de.acepe.fritzstreams;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * Creates a FlowPane and adds some rectangles inside. A LayoutAnimator is set to observe the contents of the FlowPane
 * for layout changes. https://stackoverflow.com/questions/16828234/animation-upon-layout-changes
 */
public class TestLayoutAnimate extends Application {
    private GridPane grid;

    public static void main(String[] args) {
        Application.launch(TestLayoutAnimate.class);
    }

    @Override
    public void start(Stage primaryStage) {
        grid = new GridPane();
        grid.setStyle("-fx-hgap: 5; -fx-vgap: 10; ");

        Label l1 = new Label("label1");
        Label l2 = new Label("label2");
        Label l3 = new Label("label3");
        TextField tf1 = new TextField("text1");
        TextField tf2 = new TextField("text2");
        TextField tf3 = new TextField("text3");

        l1.setMinWidth(Control.USE_PREF_SIZE);
        l2.setMinWidth(Control.USE_PREF_SIZE);
        l3.setMinWidth(Control.USE_PREF_SIZE);
        tf1.setMinWidth(Control.USE_PREF_SIZE);
        tf2.setMinWidth(Control.USE_PREF_SIZE);
        tf3.setMinWidth(Control.USE_PREF_SIZE);

        ObservableList<Node> nodes = grid.getChildren();
        grid.widthProperty().addListener((observableValue, number, t1) -> {
            int width = 0;
            int row = 0;
            int col = 0;
            for (int i = 0; i < nodes.size(); i += 2) {
                Node label = nodes.get(i);
                Node editor = nodes.get(i + 1);
                width += label.getBoundsInParent().getWidth();
                width += editor.getBoundsInParent().getWidth();
                width += grid.getHgap() * 2;

                GridPane.setConstraints(label, col, row);
                GridPane.setConstraints(editor, col + 1, row);

                if (width > grid.getWidth() && i != 0) {
                    row++;
                    col = 0;
                    GridPane.setConstraints(label, col, row);
                    GridPane.setConstraints(editor, col + 1, row);
                } else {
                    col += 2;
                }
            }
        });

        nodes.setAll(l1, tf1, l2, tf2, l3, tf3);

        GridPane.setConstraints(l1, 0, 0);
        GridPane.setConstraints(tf1, 1, 0);
        GridPane.setConstraints(l2, 2, 0);
        GridPane.setConstraints(tf2, 3, 0);
        GridPane.setConstraints(l3, 4, 0);
        GridPane.setConstraints(tf3, 5, 0);

        LayoutAnimator ly = new LayoutAnimator();
        ly.observe(nodes);

        Scene scene = new Scene(grid, 300, 250);

        primaryStage.setTitle("Flow Layout Test");
        primaryStage.setScene(scene);
        primaryStage.setX(400);
        primaryStage.show();
    }

}