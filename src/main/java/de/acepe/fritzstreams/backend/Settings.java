package de.acepe.fritzstreams.backend;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Settings {

    public static final String PREFERENCES_ROOT = "FritzStreams";
    public static final String APP_ID = "5618524";

    private static final Logger LOG = LoggerFactory.getLogger(Settings.class);
    private static final String DOWNLOAD_PATH = "download-path";

    private static Settings instance;

    private Preferences prefs;
    private String targetpath;

    private Settings() {
        init();
    }

    public static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    private void init() {
        prefs = Preferences.userRoot().node(PREFERENCES_ROOT);
        targetpath = prefs.get(DOWNLOAD_PATH, System.getProperty("user.home"));
    }

    public void persist() {
        try {
            prefs.put(DOWNLOAD_PATH, targetpath);
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

}
