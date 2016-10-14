package de.acepe.fritzstreams.backend.download;

public interface Downloadable {

    String getDownloadURL();

    String getTargetFileName();

    void setTotalSizeInBytes(Integer size);

    Integer getTotalSizeInBytes();

    void setDownloadedSizeInBytes(Integer size);

    Integer getDownloadedSizeInBytes();
}
