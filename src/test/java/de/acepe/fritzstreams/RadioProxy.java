package de.acepe.fritzstreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


public class RadioProxy {

//    private static final String FRITZ_URL = "https://rbb-fritz-live.sslcast.addradio.de/rbb/fritz/live/mp3/128/stream.mp3";
    private static final String FRITZ_URL = "http://rbb-fritz-live.cast.addradio.de/rbb/fritz/live/mp3/128/stream.mp3";
    private static final String RADIOEINS_URL = "http://rbb-radioeins-live.cast.addradio.de/rbb/radioeins/live/mp3/128/stream.mp3";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/fritz", new RadioStreamHandler(FRITZ_URL));
        server.createContext("/radioeins", new RadioStreamHandler(RADIOEINS_URL));
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
    }

    private static class RadioStreamHandler implements HttpHandler {
        private final String url;

        public RadioStreamHandler(String url) {
            this.url = url;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            Headers headers = t.getResponseHeaders();
            headers.set("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type");
            headers.set("Access-Control-Allow-Methods", "GET, OPTIONS, HEAD");
            headers.set("Access-Control-Allow-Origin", "*");
            headers.set("Cache-Control", "no-cache, no-store");
            headers.set("Connection", "Close");
            headers.set("Content-Type", "audio/mpeg");
            // headers.set("Date", "Sun, 07 Jan 2018 18:19:39 GMT");
            // headers.set("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");

            t.sendResponseHeaders(200, 0);

            URLConnection conn = new URL(url).openConnection();
            InputStream is = conn.getInputStream();

            OutputStream os = t.getResponseBody();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
            os.close();
        }
    }
}