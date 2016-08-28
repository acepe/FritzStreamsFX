package de.acepe.fritzstreams;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class ScreenManager extends StackPane {
    private final StreamsApplication application;

    public enum Direction {
        LEFT, RIGHT, NONE
    }

    private static final Logger LOG = LoggerFactory.getLogger(ScreenManager.class);
    private static final Duration FADE_DURATION = new Duration(400);

    private final Map<ScreenId, Node> screens;
    private final Map<ScreenId, ControlledScreen> controllers;

    public ScreenManager(StreamsApplication application) {
        this.application = application;
        screens = new HashMap<>();
        controllers = new HashMap<>();
    }

    public Node getScreen(ScreenId name) {
        return screens.get(name);
    }

    public boolean loadScreen(ScreenId id) {
        try {
            FXMLLoader myLoader = new FXMLLoader(getClass().getResource(id.getResource()));
            Parent screen = myLoader.load();
            ControlledScreen myScreenControler = myLoader.getController();
            controllers.put(id, myScreenControler);
            screens.put(id, screen);

            myScreenControler.setScreenManager(this);
            return true;
        } catch (IOException e) {
            LOG.error("Couldn't load FXML-View {}", id, e);
            return false;
        }
    }

    public boolean setScreen(ScreenId id) {
        return setScreen(id, Direction.NONE);
    }

    public boolean setScreen(ScreenId id, Direction direction) {
        if (screens.get(id) == null) {
            LOG.error("Screen {} hasn't been loaded", id);
            return false;
        }
        if (getChildren().isEmpty() || direction == Direction.NONE) {
            return showScreen(id);
        }

        return changeScreens(id, direction);
    }

    private boolean showScreen(ScreenId id) {
        getChildren().setAll(screens.get(id));
        return true;
    }

    private boolean changeScreens(ScreenId id, Direction direction) {
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
        newImageTimeline.setOnFinished(t -> getChildren().setAll(newNode));

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

    public boolean unloadScreen(ScreenId id) {
        if (screens.remove(id) == null) {
            LOG.error("Couldn't unload Screen {}, as it was not loaded...");
            return false;
        } else {
            return true;
        }
    }

    public ControlledScreen getController(ScreenId id) {
        return controllers.get(id);
    }

    public StreamsApplication getApplication() {
        return application;
    }

}
