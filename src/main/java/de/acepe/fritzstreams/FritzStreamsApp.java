package de.acepe.fritzstreams;

import javax.inject.Inject;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.acepe.fritzstreams.app.AppModule;
import de.acepe.fritzstreams.app.ScreenManager;
import de.acepe.fritzstreams.app.Screens;
import de.acepe.fritzstreams.ui.MainViewController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class FritzStreamsApp extends Application {

    @Inject
    private ScreenManager screenManager;

    private Injector injector;

    @Override
    public void start(Stage primaryStage) {
        System.setProperty("prism.lcdtext", "false");

        injector = Guice.createInjector(new AppModule(this, this::getInjector));
        injector.injectMembers(this);

        screenManager.loadScreen(Screens.SETTINGS);
        screenManager.loadScreen(Screens.STREAMS);

        BorderPane root = new BorderPane();
        root.setCenter(screenManager);

        Screens startScreen = Screens.STREAMS;
        Scene scene = new Scene(root, startScreen.getWidth(), startScreen.getHeight());
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setX(0);
        primaryStage.show();
        primaryStage.setOnCloseRequest((event -> screenManager.closeStages()));

        screenManager.setScreen(startScreen);
    }

    @Override
    public void stop() throws Exception {
        MainViewController mainViewController = screenManager.getController(Screens.STREAMS);
        mainViewController.stop();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }

    Injector getInjector() {
        return injector;
    }
}
