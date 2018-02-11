package de.acepe.fritzstreams.app;

import java.time.LocalDate;

import de.acepe.fritzstreams.backend.OnDemandStream;
import de.acepe.fritzstreams.backend.Stream;

public interface OnDemandStreamFactory {
    OnDemandStream create(LocalDate date, Stream stream);
}
