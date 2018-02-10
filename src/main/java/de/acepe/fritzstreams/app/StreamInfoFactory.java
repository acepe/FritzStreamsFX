package de.acepe.fritzstreams.app;

import java.time.LocalDate;

import de.acepe.fritzstreams.backend.OnDemandStream;

public interface StreamInfoFactory {
    OnDemandStream create(LocalDate date, OnDemandStream.Stream stream);
}
