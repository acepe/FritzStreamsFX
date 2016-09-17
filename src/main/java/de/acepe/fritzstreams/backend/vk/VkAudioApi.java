package de.acepe.fritzstreams.backend.vk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gson.Gson;

import de.acepe.fritzstreams.backend.vk.model.AudioSearchResponse;
import de.acepe.fritzstreams.backend.vk.model.AudioSearchResponseEnvelope;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public final class VkAudioApi {
    private static final Logger LOG = LoggerFactory.getLogger(VkAudioApi.class);

    private static final String API = "5.53";
    private static final String APP_ID = "{APP_ID}";
    private static final String PERMISSIONS = "{PERMISSIONS}";
    private static final String REDIRECT_URI = "{REDIRECT_URI}";
    private static final String DISPLAY = "{DISPLAY}";
    private static final String API_VERSION = "{API_VERSION}";
    private static final String PARAMETERS = "{PARAMETERS}";
    private static final String ACCESS_TOKEN = "{ACCESS_TOKEN_REF}";
    private static final String ACCESS_TOKEN_REF = "#access_token=";
    private static final Pattern TOKEN_PATTERN = Pattern.compile(ACCESS_TOKEN_REF + "(.*?)&");
    private static final String AUTH_URL_TEMPLATE = "https://oauth.vk.com/authorize"
                                                    + "?client_id="
                                                    + APP_ID
                                                    + "&scope="
                                                    + PERMISSIONS
                                                    + "&redirect_uri="
                                                    + REDIRECT_URI
                                                    + "&display="
                                                    + DISPLAY
                                                    + "&v="
                                                    + API_VERSION
                                                    + "&response_type=token";
    private static final String API_REQUEST = "https://api.vk.com/method/{METHOD_NAME}"
                                              + "?"
                                              + PARAMETERS
                                              + "&access_token="
                                              + ACCESS_TOKEN
                                              + "&v="
                                              + API;
    private static final String REDIRECT_URL = "https://oauth.vk.com/blank.html";
    private static final String PREF_VK_TOKEN = "VK_TOKEN";

    private final String appId;
    private final Preferences prefs;
    private String accessToken;

    public static VkAudioApi with(String appId, String preferencesRoot) {
        return new VkAudioApi(appId, preferencesRoot);
    }

    private VkAudioApi(String appId, String preferencesRoot) {
        this.appId = appId;
        prefs = Preferences.userRoot().node(preferencesRoot);
        accessToken = prefs.get(PREF_VK_TOKEN, null);

        LOG.debug("Stored token: " + accessToken);

        if (Strings.isNullOrEmpty(accessToken) || !validateToken()) {
            auth();
        }
    }

    public AudioSearchResponse searchAudio(String audioQuery, int limit) {
        String response = null;
        try {
            response = invokeApi("audio.search",
                                        Params.create()
                                              .add("q", URLEncoder.encode(audioQuery, "UTF-8"))
                                              .add("count", String.valueOf(limit)));
        } catch (UnsupportedEncodingException e) {
        LOG.error("Cannot escape URL param-.",e);
        }
        LOG.debug(response);

        Gson g = new Gson();
        AudioSearchResponseEnvelope audioSearchResponse = g.fromJson(response, AudioSearchResponseEnvelope.class);
        return audioSearchResponse.getResponse();
    }

    private void auth() {
        LOG.info("Authentification needed.");

        String reqUrl = AUTH_URL_TEMPLATE.replace(APP_ID, appId)
                                         .replace(PERMISSIONS, "audio")
                                         .replace(REDIRECT_URI, REDIRECT_URL)
                                         .replace(DISPLAY, "page")
                                         .replace(API_VERSION, API);

        WebView view = new WebView();
        WebEngine webEngine = view.getEngine();

        Scene scene = new Scene(new BorderPane(view));

        Stage stage = new Stage();
        stage.setScene(scene);

        webEngine.locationProperty().addListener((observable, oldValue, newLocation) -> {
            stage.setTitle(newLocation);
            if (newLocation.startsWith(REDIRECT_URL)) {
                Matcher matcher = TOKEN_PATTERN.matcher(newLocation);

                String token = null;
                while (matcher.find() && token == null)
                    token = matcher.group(1);

                prefs.put(PREF_VK_TOKEN, token);
                accessToken = token;
                stage.close();
            }
        });

        webEngine.load(reqUrl);
        stage.showAndWait();
    }

    private boolean validateToken() {
        String response = invokeApi("account.getInfo", Params.create());
        LOG.debug("auth response: " + response);
        return response != null && !response.startsWith("{\"error\"");
    }

    private String invokeApi(String method, Params params) {
        String parameters = (params == null) ? "" : params.build();
        try {
            URL reqUrl = new URL(API_REQUEST.replace("{METHOD_NAME}", method)
                                            .replace(ACCESS_TOKEN, accessToken)
                                            .replace(PARAMETERS + "&", parameters));
            return invokeApi(reqUrl);
        } catch (MalformedURLException e) {
            LOG.error("Not a valid URL.", e);
            return null;
        } catch (IOException e) {
            LOG.error("Error reading response.", e);
            return null;
        }
    }

    private static String invokeApi(URL requestUrl) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(requestUrl.openStream()))) {
            reader.lines().forEach(result::append);
        }
        return result.toString();
    }

    private static class Params {

        public static Params create() {
            return new Params();
        }

        private final HashMap<String, String> params;

        private Params() {
            params = new HashMap<>();
        }

        private Params add(String key, String value) {
            params.put(key, value);
            return this;
        }

        private String build() {
            if (params.isEmpty())
                return "";

            StringBuilder result = new StringBuilder();
            params.keySet().forEach(key -> result.append(key).append('=').append(params.get(key)).append('&'));
            return result.toString();
        }
    }
}