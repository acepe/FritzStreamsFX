package de.acepe.fritzstreams.app;

import com.google.inject.assistedinject.Assisted;
import de.acepe.fritzstreams.backend.OnDemandStream;

public interface OnDemandStreamFactory {
    OnDemandStream create(@Assisted("initialTitle") String initialTitle, @Assisted("url") String url);
}
