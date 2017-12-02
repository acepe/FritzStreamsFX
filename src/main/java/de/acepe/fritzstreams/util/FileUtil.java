package de.acepe.fritzstreams.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;

public final class FileUtil {

    private static final Pattern PATTERN = Pattern.compile("[^A-Za-z0-9_\\-. ]");
    private static final int MAX_LENGTH = 127;

    private static final String OS_NAME = System.getProperty("os.name");
    private static final String _OS_NAME = OS_NAME.toLowerCase(Locale.US);
    private static final boolean isWindows = _OS_NAME.startsWith("windows");
    private static final boolean isLinux = _OS_NAME.startsWith("linux");
    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    private static Boolean hasXdgOpen() {
        return new File("/usr/bin/xdg-open").canExecute();
    }

    private FileUtil() {
    }

    public static String escapeStringAsFilename(String in) {
        StringBuffer sb = new StringBuffer();
        Matcher matcher = PATTERN.matcher(in);
        while (matcher.find()) {
            // Convert matched character to percent-encoded.
            String replacement = "%" + Integer.toHexString(matcher.group().charAt(0)).toUpperCase();
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        String encoded = sb.toString();

        // Truncate the string.
        int end = Math.min(encoded.length(), MAX_LENGTH);
        return encoded.substring(0, end);
    }

    public static void doOpen(File file) {
        doOpen(file.toURI());
    }

    public static void doOpen(URI uri) {
        Platform.runLater(() -> {
            try {
                if (isWindows) {
                    ProcessBuilder pb = new ProcessBuilder("explorer", uri.toString());
                    pb.start();
                } else if (hasXdgOpen()) {
                    ProcessBuilder pb = new ProcessBuilder("/usr/bin/xdg-open", uri.toString());
                    pb.start();
                }
            } catch (IOException e) {
                LOG.error("Couldn't open {}", uri, e);
            }
        });
    }

    public static void doOpenFolder(File downloadedFile) {
        doOpen(downloadedFile.getParentFile());
    }

    public static void delete(File file) {
        try {
            Files.delete(file.toPath());
        } catch (IOException e) {
            LOG.error("Couldn't delete File {}.", file, e);
        }
    }

    /**
     * Convert bytes in a human readable format.
     *
     * @param bytes
     *            The byte count
     * @param iec
     *            false for KB, false for KiB
     * @return The human readable file size
     */
    public static String humanReadableBytes(long bytes, boolean iec) {
        // Are we using xB or xiB?
        int byteUnit = iec ? 1024 : 1000;
        float newBytes = bytes;
        int exp = 0;

        // Calculate the file size in the best readable way
        while (newBytes > byteUnit) {
            newBytes = newBytes / byteUnit;
            exp++;
        }

        // What prefix do we have to use?
        String prefix = "";
        if (exp > 0) {
            prefix = (iec ? " KMGTPE" : " kMGTPE").charAt(exp) + ((iec) ? "i" : "");
        }

        // Return a human readable String
        return String.format("%.2f %sB", newBytes, prefix);
    }

}
