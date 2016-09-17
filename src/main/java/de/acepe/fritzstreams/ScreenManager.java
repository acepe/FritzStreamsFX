package de.acepe.fritzstreams;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public class ScreenManager extends StackPane {
    private static final String APP_TITLE = "Fritz Streams";
    private final Application application;

    public enum Direction {
        LEFT, RIGHT, NONE
    }

    private static final Logger LOG = LoggerFactory.getLogger(ScreenManager.class);
    private static final Duration FADE_DURATION = new Duration(400);

    private final Map<Screens, Node> screens;
    private final Map<Screens, ControlledScreen> controllers;
    private final Set<Stage> stages = new HashSet<>();

    public ScreenManager(Application application) {
        this.application = application;
        screens = new HashMap<>();
        controllers = new HashMap<>();
    }

    public Node getScreen(Screens name) {
        return screens.get(name);
    }

    public boolean loadScreen(Screens screen) {
        try {
            FXMLLoader myLoader = new FXMLLoader(getClass().getResource(screen.getResource()));
            Parent screenView = myLoader.load();
            ControlledScreen myScreenControler = myLoader.getController();
            controllers.put(screen, myScreenControler);
            screens.put(screen, screenView);

            myScreenControler.setScreenManager(this);
            return true;
        } catch (IOException e) {
            LOG.error("Couldn't load FXML-View {}", screen, e);
            return false;
        }
    }

    public boolean setScreen(Screens id) {
        return setScreen(id, Direction.NONE);
    }

    public boolean setScreen(Screens id, Direction direction) {
        if (screens.get(id) == null) {
            LOG.error("Screen {} hasn't been loaded", id);
            return false;
        }
        if (getChildren().isEmpty() || direction == Direction.NONE) {
            return showScreen(id);
        }

        return changeScreens(id, direction);
    }

    private boolean showScreen(Screens id) {
        getChildren().setAll(screens.get(id));
        Stage stage = (Stage) getScene().getWindow();
        stage.setTitle(APP_TITLE + " - " + id.getTitle());
        return true;
    }

    public Stage showScreenInNewStage(Screens id) {
        loadScreen(id);
        BorderPane contentContainer = new BorderPane();
        contentContainer.setCenter(screens.get(id));

        Scene scene = new Scene(contentContainer, id.getWidth(), id.getHeight());
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        Window mainWindow = getScene().getWindow();

        Stage stage = new Stage();
        stage.setOnCloseRequest(event -> stages.remove(stage));
        stages.add(stage);

        stage.setScene(scene);
        stage.setTitle(APP_TITLE + " - " + id.getTitle());
        stage.setX(mainWindow.getX() + mainWindow.getWidth());
        stage.setY(mainWindow.getY());
        stage.show();
        return stage;
    }

    private boolean changeScreens(Screens id, Direction direction) {
        Node oldNode = getChildren().get(0);
        Bounds oldNodeBounds = oldNode.getBoundsInParent();
        ImageView oldImage = new ImageView(oldNode.snapshot(new SnapshotParameters(),
                                                            new WritableImage((int) oldNodeBounds.getWidth(),
                                                                              (int) oldNodeBounds.getHeight())));

        Node newNode = screens.get(id);
        getChildren().add(newNode);
        ImageView newImage = new ImageView(newNode.snapshot(new SnapshotParameters(),
                                                            new WritableImage((int) oldNodeBounds.getWidth(),
                                                                              (int) oldNodeBounds.getHeight())));
        getChildren().remove(newNode);

        // Create new animationPane with both images
        StackPane animationPane = new StackPane(oldImage, newImage);
        animationPane.setPrefSize((int) oldNodeBounds.getWidth(), (int) oldNodeBounds.getHeight());
        getChildren().setAll(animationPane);

        oldImage.setTranslateX(0);
        newImage.setTranslateX(direction == Direction.LEFT ? oldNodeBounds.getWidth() : -oldNodeBounds.getWidth());

        KeyFrame newImageKeyFrame = new KeyFrame(FADE_DURATION,
                                                 new KeyValue(newImage.translateXProperty(),
                                                              0,
                                                              Interpolator.EASE_BOTH));
        Timeline newImageTimeline = new Timeline();
        newImageTimeline.getKeyFrames().add(newImageKeyFrame);
        newImageTimeline.setOnFinished(t -> {
            getChildren().setAll(newNode);
            Stage stage = (Stage) getScene().getWindow();
            stage.setTitle(APP_TITLE + " - " + id.getTitle());
        });

        double endValue = direction == Direction.LEFT ? -oldNodeBounds.getWidth() : oldNodeBounds.getWidth();
        KeyFrame oldImageKeyFrame = new KeyFrame(FADE_DURATION,
                                                 new KeyValue(oldImage.translateXProperty(),
                                                              endValue,
                                                              Interpolator.EASE_BOTH));
        Timeline oldImageTimeLine = new Timeline();
        oldImageTimeLine.getKeyFrames().add(oldImageKeyFrame);

        newImageTimeline.play();
        oldImageTimeLine.play();
        return true;
    }

    public boolean unloadScreen(Screens id) {
        if (screens.remove(id) == null) {
            LOG.error("Couldn't unload Screen {}, as it was not loaded...");
            return false;
        } else {
            return true;
        }
    }

    public void closeStages() {
        stages.forEach(Stage::close);
    }

    public ControlledScreen getController(Screens id) {
        return controllers.get(id);
    }

    public Application getApplication() {
        return application;
    }

}
