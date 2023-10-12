package de.acepe.fritzstreams.backend;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Settings {

    public static final String PREFERENCES_ROOT = "FritzStreams";

    private static final Logger LOG = LoggerFactory.getLogger(Settings.class);
    private static final String DOWNLOAD_PATH = "download-path";
    private static final String TMP_PATH = "tmp-path";

    private Preferences prefs;
    private String targetpath;
    private String tmpPath;

    @Inject
    public Settings() {
        init();
    }

    private void init() {
        prefs = Preferences.userRoot().node(PREFERENCES_ROOT);
        targetpath = prefs.get(DOWNLOAD_PATH, System.getProperty("user.home"));
        tmpPath = prefs.get(TMP_PATH, System.getProperty("java.io.tmpdir") + File.separator + "fritz-livestream.mp3");
    }

    public void persist() {
        try {
            prefs.put(DOWNLOAD_PATH, targetpath);
            prefs.put(TMP_PATH, tmpPath);
            prefs.flush();
        } catch (BackingStoreException e) {
            LOG.error("Settings could not be persisted.");
        }
    }

    public String getTargetpath() {
        return targetpath;
    }

    public void setTargetpath(String targetpath) {
        this.targetpath = targetpath;
    }

    public String getTmpPath() {
        return tmpPath;
    }
}
