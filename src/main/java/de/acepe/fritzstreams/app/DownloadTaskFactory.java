package de.acepe.fritzstreams.app;

import java.io.File;
import java.util.function.Consumer;

import de.acepe.fritzstreams.backend.Playlist;
import de.acepe.fritzstreams.backend.download.DownloadTask;
import de.acepe.fritzstreams.backend.download.Downloadable;

public interface DownloadTaskFactory<T extends Downloadable> {
    DownloadTask<T> create(T downloadable, Consumer<File> downloadedFileConsumer, Playlist playlist);

    DownloadTask<T> create(T downloadable);
}
