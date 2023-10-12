package de.acepe.fritzstreams;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sun.javafx.application.LauncherImpl;
import de.acepe.fritzstreams.app.AppModule;
import de.acepe.fritzstreams.app.ScreenManager;
import de.acepe.fritzstreams.app.Screens;
import de.acepe.fritzstreams.backend.DownloadManager;
import de.acepe.fritzstreams.backend.StreamManager;
import jakarta.inject.Inject;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FritzStreamsApp extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(FritzStreamsApp.class);

    @Inject
    private ScreenManager screenManager;
    @Inject
    private StreamManager streamManager;
    @Inject
    private DownloadManager downloadManager;

    private Injector injector;

    public static void start(String... args) {
        LauncherImpl.launchApplication(FritzStreamsApp.class, args);
    }

    @Override
    public void start(Stage primaryStage) {
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("https.protocols", "TLSv1.2");

        injector = Guice.createInjector(new AppModule(this, this::getInjector));
        injector.injectMembers(this);

        screenManager.loadScreen(Screens.SETTINGS);
        screenManager.loadScreen(Screens.MAIN);

        BorderPane root = new BorderPane();
        root.setCenter(screenManager);

        Screens startScreen = Screens.MAIN;
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
        LOG.info("Shutting down");
        streamManager.stop();
        downloadManager.stop();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }

    Injector getInjector() {
        return injector;
    }
}
