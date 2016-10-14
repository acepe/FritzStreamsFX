package de.acepe.fritzstreams;

import de.acepe.fritzstreams.backend.vk.VkAudioApi;
import de.acepe.fritzstreams.backend.vk.model.AudioSearchResponse;
import javafx.application.Application;
import javafx.stage.Stage;

public class VKTest extends Application {

    private static final String APP_ID = "5618524";
    private static final String PREFERENCE_NODE = "de.acepe.fritzstreams";

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Preferences.userRoot().node(PREFERENCE_NODE).clear();

        AudioSearchResponse audioSearchResponseJson = VkAudioApi.with(APP_ID, PREFERENCE_NODE)
                                                                .searchAudio("Ghost - Square Hammer", 100, false);
        audioSearchResponseJson.getItems().forEach(System.out::println);
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
