package de.acepe.fritzstreams;

import javax.inject.Inject;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.acepe.fritzstreams.backend.download.DownloadManager;
import de.acepe.fritzstreams.ui.VKAudioSearchController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class DownloaderApplication extends Application {

    @Inject
    private ScreenManager screenManager;
    @Inject
    private DownloadManager downloadManager;

    private Injector injector;

    @Override
    public void start(Stage primaryStage) {
        System.setProperty("prism.lcdtext", "false");

        injector = Guice.createInjector(new AppModule(this, this::getInjector));
        injector.injectMembers(this);

        screenManager.loadScreen(Screens.DOWNLOADER);
        screenManager.loadScreen(Screens.DOWNLOAD_MANAGERER);
        ((VKAudioSearchController) screenManager.getController(Screens.DOWNLOADER)).setSearchText("Primordial");

        BorderPane root = new BorderPane();
        root.setCenter(screenManager);

        Screens startScreen = Screens.DOWNLOADER;
        Scene scene = new Scene(root, startScreen.getWidth(), startScreen.getHeight());
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setX(0);
        primaryStage.show();
        primaryStage.setOnCloseRequest((event -> {
            screenManager.closeStages();
        }));

        screenManager.setScreen(startScreen);
        screenManager.showScreenInNewStage(Screens.DOWNLOAD_MANAGERER);
    }

    @Override
    public void stop() throws Exception {
        downloadManager.stopDownloads();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }

    Injector getInjector() {
        return injector;
    }
}
