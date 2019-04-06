package de.acepe.fritzstreams.app;

import java.io.File;
import java.util.function.Consumer;

import de.acepe.fritzstreams.backend.Playlist;
import de.acepe.fritzstreams.backend.DownloadTask;
import de.acepe.fritzstreams.backend.OnDemandStream;

public interface DownloadTaskFactory {
    DownloadTask create(OnDemandStream onDemandStream, Consumer<File> downloadedFileConsumer);

}
