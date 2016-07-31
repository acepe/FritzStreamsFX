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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class ScreenManager extends StackPane {
    private static final Logger LOG = LoggerFactory.getLogger(ScreenManager.class);
    private static final Duration FADE_DURATION = new Duration(600);

    private final Map<ScreenId, Node> screens;
    private final Map<ScreenId, ControlledScreen> controllers;
    private Timeline fadeOut;
    private Timeline fadeIn;

    public ScreenManager() {
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

            myScreenControler.setScreenParent(this);
            return true;
        } catch (IOException e) {
            LOG.error("Couldn't load FXML-View {}", id, e);
            return false;
        }
    }

    public boolean setScreen(ScreenId id) {
        if (screens.get(id) == null) {
            LOG.error("Screen {} hasn't been loaded", id);
            return false;
        }

        if (getChildren().isEmpty()) {
            return showScreen(id);
        }

        return changeScreens(id);
    }

    private boolean showScreen(ScreenId id) {
        getChildren().add(screens.get(id));
        return true;
    }

    private boolean changeScreens(ScreenId id) {
        Node oldNode = getChildren().get(0);
        Node newNode = screens.get(id);

        if (fadeIn != null) {
            fadeIn.stop();
        }
        if (fadeOut != null) {
            fadeOut.stop();
        }
//        fadeOut = new Timeline(new KeyFrame(FADE_DURATION,
//                                            new KeyValue(oldNode.opacityProperty(), 0, Interpolator.EASE_BOTH)));

        fadeIn = new Timeline(new KeyFrame(Duration.ZERO,
                                           new KeyValue(newNode.opacityProperty(), 0.0, Interpolator.EASE_BOTH)),
                              new KeyFrame(FADE_DURATION,
                                           new KeyValue(newNode.opacityProperty(), 1, Interpolator.EASE_BOTH)));
        fadeIn.setOnFinished(event -> getChildren().remove(oldNode));

        getChildren().add(newNode);

//        fadeOut.play();
        fadeIn.play();

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
}
