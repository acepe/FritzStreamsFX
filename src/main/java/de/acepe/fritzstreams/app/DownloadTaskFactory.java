package de.acepe.fritzstreams.app;

import java.io.File;
import java.util.function.Consumer;

import de.acepe.fritzstreams.backend.Playlist;
import de.acepe.fritzstreams.backend.DownloadTask;
import de.acepe.fritzstreams.backend.StreamInfo;

public interface DownloadTaskFactory {
    DownloadTask create(StreamInfo streamInfo, Consumer<File> downloadedFileConsumer, Playlist playlist);

}
