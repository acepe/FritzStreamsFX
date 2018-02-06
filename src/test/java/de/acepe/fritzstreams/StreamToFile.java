package de.acepe.fritzstreams;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class StreamToFile {

    public static void main(String... args) throws IOException {

        URLConnection conn = new URL("https://rbb-fritz-live.sslcast.addradio.de/rbb/fritz/live/mp3/128/stream.mp3").openConnection();
        InputStream is = conn.getInputStream();

        OutputStream outstream = new FileOutputStream(new File("/tmp/file.mp3"));
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) > 0) {
            outstream.write(buffer, 0, len);
        }
        outstream.close();
    }
}
