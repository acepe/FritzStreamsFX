package de.acepe.fritzstreams.app;

import java.util.function.Consumer;

import de.acepe.fritzstreams.backend.StreamCrawler;

public interface StreamCrawlerFactory {
    StreamCrawler create(String contentURL, Consumer<StreamCrawler> onStreamCrawledCallback);
}
