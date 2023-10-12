package de.acepe.fritzstreams.app;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScreenManager extends StackPane {

    public enum Direction {
        LEFT, RIGHT, NONE
    }

    private static final Logger LOG = LoggerFactory.getLogger(ScreenManager.class);
    private static final Duration FADE_DURATION = new Duration(400);

    private final Application application;
    private final Provider<FXMLLoader> fxmlLoaderProvider;
    private final String appTitle;
    private final Map<Screens, Node> screens;
    private final Map<Screens, ControlledScreen> controllers;
    private final Set<Stage> stages = new HashSet<>();
    private final String css;

    private Screens activeScreen;

    @Inject
    public ScreenManager(Application application,
                         Provider<FXMLLoader> fxmlLoaderProvider,
                         @Named("APP_TITLE") String appTitle) {
        this.application = application;
        this.fxmlLoaderProvider = fxmlLoaderProvider;
        this.appTitle = appTitle;

        css = application.getClass().getResource("style.css").toExternalForm();
        screens = new HashMap<>();
        controllers = new HashMap<>();
    }

    public Node getScreen(Screens name) {
        return screens.get(name);
    }

    public <T extends ControlledScreen> void loadScreen(Screens screen) {
        try {
            FXMLLoader fxmlLoader = fxmlLoaderProvider.get();
            fxmlLoader.setLocation(application.getClass().getResource(screen.getResource()));

            Parent screenView = fxmlLoader.load();
            T myScreenControler = fxmlLoader.getController();
            controllers.put(screen, myScreenControler);
            screens.put(screen, screenView);

        } catch (IOException e) {
            LOG.error("Couldn't load FXML-View {}", screen, e);
        }
    }

    public <T> T loadFragment(Fragments fragment) {
        try {
            FXMLLoader fxmlLoader = fxmlLoaderProvider.get();
            fxmlLoader.setLocation(application.getClass().getResource(fragment.getResource()));
            fxmlLoader.load();
            return fxmlLoader.getController();
        } catch (Exception e) {
            LOG.error("Couldn't load FXML-View-Fragment {}", fragment, e);
            return null;
        }
    }

    public void setScreen(Screens id) {
        setScreen(id, Direction.NONE);
    }

    public void setScreen(Screens id, Direction direction) {
        if (screens.get(id) == null) {
            LOG.error("Screen {} hasn't been loaded", id);
            return;
        }
        if (getChildren().isEmpty() || direction == Direction.NONE) {
            showScreen(id);
            return;
        }

        changeScreens(id, direction);
    }

    private void showScreen(Screens id) {
        notifyActiveScreenHidden();
        getChildren().setAll(screens.get(id));
        Stage stage = (Stage) getScene().getWindow();
        stage.setTitle(appTitle + " - " + id.getTitle());
        activeScreen = id;
        controllers.get(id).onScreenShown();
    }

    private void notifyActiveScreenHidden() {
        if (activeScreen != null) {
            controllers.get(activeScreen).onScreenHidden();
        }
    }

    public Stage showScreenInNewStage(Screens id) {
        loadScreen(id);
        BorderPane contentContainer = new BorderPane();
        contentContainer.setCenter(screens.get(id));

        Scene scene = new Scene(contentContainer, id.getWidth(), id.getHeight());
        scene.getStylesheets().add(css);

        Window mainWindow = getScene().getWindow();

        Stage stage = new Stage();
        stage.setOnCloseRequest(event -> {
            controllers.get(id).onScreenHidden();
            unloadScreen(id);
            stages.remove(stage);
        });
        stages.add(stage);

        stage.setScene(scene);
        stage.setTitle(appTitle + " - " + id.getTitle());
        stage.setX(mainWindow.getX() + mainWindow.getWidth());
        stage.setY(mainWindow.getY());
        stage.show();
        controllers.get(id).onScreenShown();
        return stage;
    }

    private void changeScreens(Screens id, Direction direction) {
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
        Timeline newImageTimeline = new Timeline(newImageKeyFrame);
        newImageTimeline.setOnFinished(t -> {
            notifyActiveScreenHidden();
            activeScreen = id;
            getChildren().setAll(newNode);
            Stage stage = (Stage) getScene().getWindow();
            stage.setTitle(appTitle + " - " + id.getTitle());
            controllers.get(id).onScreenShown();
        });

        double endValue = direction == Direction.LEFT ? -oldNodeBounds.getWidth() : oldNodeBounds.getWidth();
        KeyFrame oldImageKeyFrame = new KeyFrame(FADE_DURATION,
                new KeyValue(oldImage.translateXProperty(),
                        endValue,
                        Interpolator.EASE_BOTH));
        Timeline oldImageTimeLine = new Timeline(oldImageKeyFrame);

        newImageTimeline.play();
        oldImageTimeLine.play();
    }

    public void unloadScreen(Screens id) {
        controllers.remove(id);
        screens.remove(id);
    }

    public void closeStages() {
        stages.forEach(Stage::close);
    }

    @SuppressWarnings("unchecked")
    public <T extends ControlledScreen> T getController(Screens id) {
        return (T) controllers.get(id);
    }

    public Application getApplication() {
        return application;
    }

}
