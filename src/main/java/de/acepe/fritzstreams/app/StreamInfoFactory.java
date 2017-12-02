package de.acepe.fritzstreams.app;

import java.time.LocalDate;

import de.acepe.fritzstreams.backend.StreamInfo;

public interface StreamInfoFactory {
    StreamInfo create(LocalDate date, StreamInfo.Stream stream);
}
