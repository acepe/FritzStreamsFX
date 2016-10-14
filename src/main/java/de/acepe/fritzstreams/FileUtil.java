package de.acepe.fritzstreams;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtil {

    private static final Pattern PATTERN = Pattern.compile("[^A-Za-z0-9_\\-. ]");
    private static final int MAX_LENGTH = 127;

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
