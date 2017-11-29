package de.acepe.fritzstreams;

import java.time.LocalDate;

import de.acepe.fritzstreams.backend.stream.StreamInfo;

public interface StreamInfoFactory {
    StreamInfo create(LocalDate date, StreamInfo.Stream stream);
}
