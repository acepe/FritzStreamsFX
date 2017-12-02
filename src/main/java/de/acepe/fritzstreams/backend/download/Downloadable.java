package de.acepe.fritzstreams.backend.download;

public interface Downloadable {

    String getDownloadURL();

    String getTargetFileName();

    void setTotalSizeInBytes(Long size);

    Long getTotalSizeInBytes();

    void setDownloadedSizeInBytes(Long size);

    Long getDownloadedSizeInBytes();
}
