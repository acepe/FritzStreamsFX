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
}
